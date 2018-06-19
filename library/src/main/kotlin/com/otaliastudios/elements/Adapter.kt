package com.otaliastudios.elements

import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.util.SparseArray
import android.view.ViewGroup

/**
 * Base class. Use [Builder] to create an instance with the
 * apprioriate sources and presenters.
 */
public final class Adapter private constructor(
        private val lifecycleOwner: LifecycleOwner,
        private val sources: MutableList<Source<*>>,
        private val presenters: List<Presenter<*>>,
        private val pageSizeHint: Int
) : RecyclerView.Adapter<Presenter.Holder>(), LifecycleOwner, LifecycleObserver {

    /**
     * Constructs an [Adapter] with the given sources and presenters.
     * The order of presenters matters in case of more presenters that deal with the same
     * element types.
     */
    public class Builder(private val lifecycleOwner: LifecycleOwner, private val pageSizeHint: Int = Int.MAX_VALUE) {
        private val sources = mutableListOf<Source<*>>()
        private val presenters = mutableListOf<Presenter<*>>()

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
         * Builds the adapter with the given options.
         * Use [into] to inject directly into a recycler.
         */
        public fun build() = Adapter(lifecycleOwner, sources, presenters, pageSizeHint)

        /**
         * Builds the adapter and injects it into
         * the given [RecyclerView].
         */
        public fun into(recyclerView: RecyclerView) {
            val adapter = build()
            recyclerView.itemAnimator = Animator(adapter)
            recyclerView.adapter = adapter
        }
    }

    companion object {

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
    private val pager: Pager

    init {
        // First, let's bind to these sources, this is harmless.
        sources.forEach { it.bind(this) }

        // Second, let's collect pages, if there are any.
        val pages = mutableSetOf<Int>()
        sources.forEach { pages.addAll(it.getResults().keys) }

        // Just to be sure, ensure that we have no gaps.
        pages.toList().sorted().forEachIndexed { index, page ->
            if (index != page) throw IllegalStateException("Inconsistency in Source pages.")
        }
        val restoredCount = pages.size
        val pageCount = maxOf(restoredCount, 1) // Most frequently, this will be 0.

        // Third, create a pager.
        // The pager will create pageCount pages.
        pager = Pager()
        pager.bind(this, pageCount)

        // This branch is useless but helps understand what's going on.
        if (restoredCount == 0) {
            onPageCreated(pager.pageAt(0))
        } else {

            // There is one issue. Let's say we recovered X pages from source A,
            // and we have sources B, C, D who don't know about these X pages.
            // 1. We are not subscribed to A LiveData about these X pages.
            // 2. If (B, C, D) do depend on A: they won't receive openPage(), they are waiting for A results.
            // 3. If (B, C, D) do not depend on A: they will receive openPage(). All is fine here.

            // Solve 1 - 2:
            // We should subscribe to As through their results.
            // Since the results are cached by LiveData, this will finally trigger
            // openPage() on B, C, D.
            sources.forEach { source ->
                source.getResults().forEach { entry ->
                    // This source knows this page.
                    val page = pager.pageAt(entry.key)
                    onSourceLiveData(page, source, entry.value)
                }
            }

            // Solve 3:
            // Must be done after 1 - 2, or it will create results for B C D that depends on A.
            pager.forEachPage { onPageCreated(it) }

            // This solution will mess with the order a bit (if B1 depends on A, and B2 does not,
            // B1 will probably receive openPage() before B2 which makes no sense but should do no harm).
        }

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        pager.unbind()
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
            onSourceLiveData(page, source, data)
        }
    }

    /**
     * This source has results.
     * 1. Update the Recycler
     * 2. Notify sources depending on this source
     */
    private fun onSourceResults(page: Page, source: Source<*>, result: List<Element<*>>) {
        pager.setResults(page, source, result)
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
                    onSourceLiveData(page, dependent, data)
                }
            }
        }
    }

    /**
     * This source provided us with a new LiveData object. Let's subscribe
     * to stay updated. This might happen after an openPage, or after rebinding to
     * sources that have values already.
     */
    private fun onSourceLiveData(page: Page, source: Source<*>, data: LiveData<out List<Element<*>>>) {
        @Suppress("UNCHECKED_CAST")
        val cast = data as LiveData<List<Element<*>>>
        cast.observe(this, Observer {
            if (it != null) onSourceResults(page, source, it)
        })
    }

    /**
     * Returns the current item count, not considering any
     * transaction that is being computed.
     */
    override fun getItemCount() = pager.elementCount()

    /**
     * Returns the element type for the given position,
     * by querying the element for that position.
     */
    override fun getItemViewType(position: Int): Int {
        return pager.elementAt(position).element!!.type
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
        Log.i("Adapter", "onCreateViewHolder, type: $viewType")
        return presenterForType(viewType).createHolder(parent, viewType)
    }

    /**
     * Binds the element at this position with the presenter that manages
     * this element type.
     * If something goes wrong here, there is likely a synchronization issue
     * with pending updates. But it should be fixed now.
     */
    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: Presenter.Holder, position: Int) {
        val presenter = typeMap.get(holder.itemViewType) as Presenter<Any>
        val query = pager.elementAt(position)
        val page = query.page!!
        val element = query.element as Element<Any>
        Log.i("Adapter", "onBindViewHolder, type: ${holder.itemViewType}, " +
                "elementType: ${element.type}, " +
                "position: $position, " +
                "presenter: ${presenter.javaClass.simpleName}, " +
                "data: ${element.data?.javaClass?.simpleName}, " +
                "source: ${element.source.javaClass.simpleName}")
        if (element.type != holder.itemViewType) {
            throw RuntimeException("Something is wrong here...")
        }
        presenter.onBind(page, holder, element)

        if (query.positionInPage == pageSizeHint - 1 && page.isLastPage()) {
            val page = pager.requestPage()
            onPageCreated(page)
        }
    }

    /**
     * Request a new page to be opened.
     * This can be used in conjunction with an infinite [pageSizeHint],
     * in order to disable automatic page opening.
     */
    public fun openPage() {
        val page = pager.requestPage()
        onPageCreated(page)
    }

}

