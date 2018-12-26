package com.otaliastudios.elements

import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.util.SparseArray
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.otaliastudios.elements.pagers.PageSizePager

/**
 * Base class. Use [Builder] to create an instance with the
 * apprioriate sources and presenters.
 */
public final class Adapter private constructor(
        private val lifecycleOwner: LifecycleOwner,
        private val sources: MutableList<Source<*>>,
        private val presenters: List<Presenter<*>>,
        private val pager: Pager,
        private var autoScrollMode: Int,
        private var autoScrollSmooth: Boolean
) : RecyclerView.Adapter<Presenter.Holder>(), LifecycleOwner, LifecycleObserver {


    /**
     * Constructs an [Adapter] with the given sources and presenters.
     * The order of presenters matters in case of more presenters that deal with the same
     * element types.
     */
    public class Builder(private val lifecycleOwner: LifecycleOwner, private val pageSizeHint: Int = Int.MAX_VALUE) {
        private val sources = mutableListOf<Source<*>>()
        private val presenters = mutableListOf<Presenter<*>>()
        private var pager: Pager? = null
        private var autoScrollMode: Int = AUTOSCROLL_OFF
        private var autoScrollSmooth: Boolean = false

        /**
         * Sets the pager for this adapter,
         * replacing any previous value.
         */
        public fun setPager(pager: Pager): Builder {
            this.pager = pager
            return this
        }

        /**
         * Append a [Source] for this adapter.
         * Returns this for chaining.
         */
        public fun addSource(source: Source<*>): Builder {
            sources.add(source)
            return this
        }

        /**
         * Append a [Presenter] for this adapter.
         * Returns this for chaining.
         */
        public fun addPresenter(presenter: Presenter<*>): Builder {
            presenters.add(presenter)
            return this
        }

        /**
         * Sets the adapter auto scroll behavior. One of
         * - [AUTOSCROLL_OFF]: No auto scroll behavior
         * - [AUTOSCROLL_POSITION_0]: Auto scroll when items are inserted at position 0
         * - [AUTOSCROLL_POSITION_ANY]: Auto scroll when items are inserted at any position
         * Returns this for chaining.
         */
        public fun setAutoScrollMode(mode: Int, smooth: Boolean = false): Builder {
            autoScrollMode = mode
            autoScrollSmooth = smooth
            return this
        }

        /**
         * Builds the adapter with the given options.
         * Use [into] to inject directly into a recycler.
         */
        public fun build(): Adapter {
            val pager = pager ?: PageSizePager(pageSizeHint)
            return Adapter(lifecycleOwner, sources, presenters, pager, autoScrollMode, autoScrollSmooth)
        }

        /**
         * Builds the adapter and injects it into
         * the given [RecyclerView].
         */
        public fun into(recyclerView: RecyclerView): Adapter {
            val adapter = build()
            recyclerView.itemAnimator = Animator(adapter)
            recyclerView.adapter = adapter
            return adapter
        }
    }

    companion object {

        /**
         * Constant for [Builder.autoScrollMode].
         * This means that no autoscroll will be performed.
         */
        const val AUTOSCROLL_OFF = 0

        /**
         * Constant for [Builder.autoScrollMode].
         * This means that, when items are inserted at position 0,
         * the adapter will automatically scroll any attached RecyclerView
         * to position 0 so that new items are visible.
         */
        const val AUTOSCROLL_POSITION_0 = 1

        /**
         * Constant for [Builder.autoScrollMode].
         * This means that, when items are inserted at any position,
         * the adapter will automatically scroll any attached RecyclerView
         * to that position so that new items are visible.
         */
        const val AUTOSCROLL_POSITION_ANY = 2

        /**
         * Shorthand for creating a [Builder] for the given
         * lifecycle owner and page hint.
         */
        fun builder(lifecycleOwner: LifecycleOwner, pageSizeHint: Int = Int.MAX_VALUE) = Builder(lifecycleOwner, pageSizeHint)

        private val TAG = Adapter::class.java.simpleName
    }

    /**
     * [Adapter] implements [LifecycleOwner] and this helps in not leaking
     * any reference while it observes sources and page state.
     * This is the same [Lifecycle] object that was passed to the builder.
     */
    override fun getLifecycle() = lifecycleOwner.lifecycle

    init {
        lifecycle.addObserver(this)
        for (presenter in presenters) {
            presenter.owner = lifecycleOwner
            presenter.adapter = this
        }
    }

    private val typeMap: SparseArray<Presenter<*>> = SparseArray()
    private val dependencyMap: MutableMap<Source<*>, MutableSet<Source<*>>> = mutableMapOf()
    private val reverseDependencyMap: MutableMap<Source<*>, MutableSet<Source<*>>> = mutableMapOf()

    internal fun getDependencies(source: Source<*>): MutableSet<Source<*>> = dependencyMap[source]!!

    internal fun getReverseDependencies(source: Source<*>): MutableSet<Source<*>> = reverseDependencyMap[source]!!

    internal fun hasDependencies(source: Source<*>) = getDependencies(source).isNotEmpty()

    internal fun dependsOn(source: Source<*>, other: Source<*>) = getDependencies(source).contains(other)

    init {
        sources.forEach {
            // Let's initialize. Could do this lazily but that solution has threading issues
            // that would make everything slower.
            dependencyMap[it] = mutableSetOf()
            reverseDependencyMap[it] = mutableSetOf()
        }

        // This ensures that every couple is processed. There must be some Kotlin
        // magic to do this in a more concise way, but this is OK for now.
        for (i in 0 until sources.size - 1) {
            for (j in i + 1 until sources.size) {
                val source1 = sources[i]
                val source2 = sources[j]
                val dep1 = source1.dependsOn(source2)
                val dep2 = source2.dependsOn(source1)
                if (dep1 && dep2) {
                    throw IllegalArgumentException("Circular dependency. Source " +
                            source1::class.java.simpleName + " and source " +
                            source2::class.java.simpleName + " both depend on each other.")
                } else if (dep1) {
                    // Surely source2 does not depend on source1.
                    // But we must check recursively that none
                    // of source2 dependencies depends on source1.
                    checkCircularDependencies(source1, source2)
                    getDependencies(source1).add(source2)
                    getReverseDependencies(source2).add(source1)
                } else if (dep2) {
                    // Same
                    checkCircularDependencies(source2, source1)
                    getDependencies(source2).add(source1)
                    getReverseDependencies(source1).add(source2)
                }
            }
        }

        sources.sortWith(Comparator { source1, source2 ->
            when {
                dependsOn(source1, source2) -> 1
                dependsOn(source2, source1) -> -1
                else -> 0
            }
        })
    }

    private fun checkCircularDependencies(source: Source<*>, target: Source<*>) {
        if (target.dependsOn(source)) throw IllegalArgumentException("Indirect circular dependency detected.")
        for (dependency in getDependencies(target)) {
            checkCircularDependencies(source, dependency)
        }
    }

    // Must be done after the init() so it gets sorted sources.
    private val pageManager: PageManager

    init {
        pager.adapter = this

        // First, let's bind to these sources, this is harmless.
        sources.forEach { it.bind(this) }

        // Second, let's collect pages, if there are any.
        val pages = mutableSetOf<Int>()
        sources.forEach { pages.addAll(it.getResults().keys) }

        // Just to be sure, ensure that we have no gaps.
        pages.asSequence().sorted().forEachIndexed { index, page ->
            if (index != page) throw IllegalStateException("Inconsistency in Source pages.")
        }
        /* pages.toList().sorted().forEachIndexed { index, page ->
            if (index != page) throw IllegalStateException("Inconsistency in Source pages.")
        } */
        val restoredCount = pages.size
        val pageCount = maxOf(restoredCount, 1) // Most frequently, this will be 0.

        // Third, create a pageManager.
        // The pageManager will create pageCount pages.
        pageManager = PageManager()
        pageManager.bind(this, pageCount)

        // This branch is useless but helps understand what's going on.
        if (restoredCount == 0) {
            onPageCreated(pageManager.pageAt(0))
        } else {

            // Complications at this point: Let's say we recovered X pages from source A,
            // and we have sources B, C, D who don't know about these X pages.
            // ISSUE 1: We are not subscribed to A LiveData about these existing X pages.
            // ISSUE 2. If (B, C, D) do depend on A: they won't receive openPage(), they are waiting for A results.
            // ISSUE 3. If (B, C, D) do not depend on A: they must receive openPage().

            // Solves 1 - 2:
            // We should subscribe to As through their results.
            // Since the results are cached by LiveData, this will finally trigger
            // openPage() on B, C, D.
            sources.forEach { source ->
                source.getResults().forEach { entry ->
                    // This source knows this page.
                    val page = pageManager.pageAt(entry.key)
                    onSourceLiveData(page, source, entry.value, true)
                }
            }

            // Solves 3:
            // Must be done after 1 - 2, or it will create results for B C D that depends on A.
            pageManager.forEach { onPageCreated(it) }

            // This solution will mess with the order a bit (if B1 depends on A, and B2 does not,
            // B1 will probably receive openPage() before B2 which makes no sense but should do no harm).
        }

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        pageManager.unbind()
        sources.forEach {
            it.unbind(this)
        }
    }

    /**
     * Got a new page.
     */
    private fun onPageCreated(page: Page) {
        sources.filter { !hasDependencies(it) && !it.knowsPage(page) }.forEach { source ->
            val data = source.openPage(page, listOf())
            onSourceLiveData(page, source, data, false)
        }
    }

    /**
     * This source has results.
     * 1. Update the Recycler
     * 2. Notify sources depending on this source
     */
    private fun onSourceResults(page: Page, source: Source<*>, result: List<Element<*>>, sync: Boolean) {
        pageManager.setResults(page, source, result, sync)
        // These are the sources that might be interested in this.
        val dependents = getReverseDependencies(source)
        for (dependent in dependents) {
            val dependencies = getDependencies(dependent)
            if (dependencies.all { it.hasResultsForPage(page) }) {
                val list = mutableListOf<Element<*>>()
                dependencies.forEach { list.addAll(it.getResultsForPage(page)) }
                if (dependent.knowsPage(page)) {
                    dependent.onPageChanged(page, list)
                } else {
                    val data = dependent.openPage(page, list)
                    onSourceLiveData(page, dependent, data, false)
                }
            }
        }
    }

    /**
     * This source provided us with a new [LiveData] object. Let's subscribe
     * to stay updated. This might happen after an openPage, or after rebinding to
     * sources that have values already.
     *
     * @param fromSavedState: If coming from a saved state (which means:
     * this source already knew about this page before this Adapter was created. This is the case
     * of a Source held in a ViewModel and the adapter is recreated with the view).
     *
     * In this case, we might want to perform a synchronous put (instead of async as usual),
     * because, the whole [RecyclerView] restoration relies on the fact that, at first layout,
     * it finds the data it had before.
     *
     * See, for instance, [LinearLayoutManager] callbacks about state saving and restoration.
     * During restoration, it asks for a new layout, but during layout, if it has no data,
     * it discards the saved state.
     */
    private fun onSourceLiveData(page: Page, source: Source<*>, data: LiveData<out List<Element<*>>>, fromSavedState: Boolean) {
        @Suppress("UNCHECKED_CAST")
        val cast = data as LiveData<List<Element<*>>>
        var firstTime = true
        cast.observe(this, Observer {
            if (it != null) {
                val sync = fromSavedState && firstTime
                onSourceResults(page, source, it, sync)
                firstTime = false
            }
        })
    }

    /**
     * Returns the current item count, not considering any
     * transaction that is being computed.
     */
    override fun getItemCount() = pageManager.elementCount()

    /**
     * Returns the element type for the given position,
     * by querying the element for that position.
     */
    override fun getItemViewType(position: Int): Int {
        return pageManager.elementAt(position, true)!!.element.type
    }

    internal fun presenterForType(viewType: Int): Presenter<*> {
        // Find a reasonable Presenter for this elementType. Try with cache map.
        var presenter = typeMap.get(viewType)
        if (presenter == null) {
            presenter = presenters.firstOrNull { it.elementTypes.contains(viewType) }
            typeMap.put(viewType, presenter)
        }
        return typeMap.get(viewType)
    }

    /**
     * Tries to find a presenter for the given viewType. If not found,
     * currently this will throw an exception.
     * In the future we want to just do a no-op.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Presenter.Holder {
        ElementsLogger.v( "onCreateViewHolder, type: $viewType")
        return presenterForType(viewType).createHolder(parent, viewType)
    }

    /**
     * Binds the element at this position with the presenter that manages
     * this element type.
     * If something goes wrong here, there is likely a synchronization issue
     * with pending updates. But it should be fixed now.
     */
    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: Presenter.Holder, position: Int, payloads: MutableList<Any>) {
        val presenter = typeMap.get(holder.itemViewType) as Presenter<Any>
        val query = pageManager.elementAt(position, true)!!
        val page = query.page
        val element = query.element as Element<Any>
        ElementsLogger.v("onBindViewHolder, type: ${holder.itemViewType}, " +
                "elementType: ${element.type}, " +
                "position: $position, " +
                "presenter: ${presenter.javaClass.simpleName}, " +
                "data: ${element.data?.javaClass?.simpleName}, " +
                "source: ${element.source.javaClass.simpleName}")
        if (element.type != holder.itemViewType) {
            throw RuntimeException("Something is wrong here...")
        }
        presenter.onBind(page, holder, element, payloads)
        pager.onElementBound(page, element, presenter, position, query.positionInPage)
    }

    // We use the payloads version.
    override fun onBindViewHolder(holder: Presenter.Holder, position: Int) {}

    /**
     * Request a new page to be opened.
     * This can be used in conjunction with a [NoPagesPager],
     * in order to disable automatic page opening.
     *
     * Note that sources might block this request by returning false in their
     * [Source.canOpenPage] callback.
     *
     * Returns true if the page was correctly opened, false if it was blocked.
     */
    public fun requestPage(): Boolean {
        val current = pageManager.lastOrNull()
        val should = sources.all {
            it.canOpenNextPage(current)
        }
        if (should) {
            val page = pageManager.requestPage()
            onPageCreated(page)
            return true
        } else {
            ElementsLogger.w("requestPage was blocked by one of the sources. Current page: $current ${current?.number}")
            return false
        }
    }

    /**
     * Queries the page manager to find the
     * element at the given position. This can be an expensive
     * computation so the usage should be limited.
     */
    public fun elementAt(position: Int): ElementAtResult? {
        return pageManager.elementAt(position, false)
    }

    /**
     * Sets the adapter auto scroll behavior. One of
     * - [AUTOSCROLL_OFF]: No auto scroll behavior
     * - [AUTOSCROLL_POSITION_0]: Auto scroll when items are inserted at position 0
     * - [AUTOSCROLL_POSITION_ANY]: Auto scroll when items are inserted at any position
     */
    public fun setAutoScrollMode(mode: Int, smooth: Boolean = false) {
        autoScrollMode = mode
        autoScrollSmooth = smooth
    }

    // Auto scroll to top during inserts.

    private val recyclerViews = mutableSetOf<RecyclerView>()

    init {
        registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (autoScrollMode == AUTOSCROLL_POSITION_0 && positionStart == 0) {
                    performAutoScroll(0)
                } else if (autoScrollMode == AUTOSCROLL_POSITION_ANY) {
                    performAutoScroll(positionStart)
                }
            }
        })
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerViews.add(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerViews.remove(recyclerView)
    }

    private fun performAutoScroll(position: Int) {
        recyclerViews.forEach {
            if (autoScrollSmooth) {
                it.smoothScrollToPosition(position)
            } else {
                it.scrollToPosition(position)
            }
        }
    }
}


