package com.otaliastudios.elements

import android.os.Handler
import android.os.Looper
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * TODO rename bind / unbind, now this class is View-related, dies with the Adapter.
 */
internal class PageManager : Iterable<Page> {

    private val pages = mutableListOf<Page>()
    private var adapter: Adapter? = null

    companion object {
        // Max pool size is ignored when using LinkedBlockingQueue. Using the 'unbounded queue' strategy:
        // https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ThreadPoolExecutor.html

        private val threadCount = AtomicInteger(1)
        private val threadFactory = ThreadFactory { r -> Thread(r, "Elements PageManager #" + threadCount.getAndIncrement()) }

        private val executor by lazy {
            ThreadPoolExecutor(5, 100,
                10L, TimeUnit.SECONDS,
                LinkedBlockingQueue(), threadFactory)
        }

        // This can't be lazy
        internal val uiExecutor = Handler(Looper.getMainLooper())
    }

    private fun countBefore(page: Page): Int {
        var count = 0
        for (current in pages) {
            if (current == page) break
            count += current.elementCount()
        }
        return count
    }

    internal fun requestPage(): Page {
        val number = if (pages.isEmpty()) 0 else pages.last().number + 1
        val new = Page(this, number)
        pages.add(new)
        return new
    }

    internal fun elementCount(): Int {
        val count = pages.fold(0) { count, page -> count + page.elementCount() }
        return count
    }

    private val elementAtResult = ElementAtResult()

    internal fun elementAt(position: Int, doThrow: Boolean): ElementAtResult? {
        var before = 0
        for (page in pages) {
            val count = page.elementCount()
            if (position >= before && position < before + count) {
                elementAtResult.page = page
                elementAtResult.element = page.elementAt(position - before)
                elementAtResult.positionInPage = position - before
                elementAtResult.position = position
                return elementAtResult
            }
            before += count
        }
        if (doThrow) {
            throw RuntimeException("No page for position $position")
        } else {
            return null
        }
    }

    internal fun pageAt(number: Int): Page {
        return pages.first { it.number == number }
    }

    override fun iterator(): Iterator<Page> {
        return pages.iterator()
    }

    /**
     * Initialize ourselves when the adapter binds to us.
     * This means creating the first page if we haven't.
     */
    internal fun bind(adapter: Adapter, pages: Int) {
        if (this.adapter != null) throw IllegalStateException("This pageManager is already bound to an adapter.")
        if (pages <= 0) throw IllegalArgumentException("Asked to create $pages pages. Should be > 0.")
        this.adapter = adapter
        repeat(pages) { requestPage() }
    }

    internal fun unbind() {
        adapter = null
    }

    internal fun previous(page: Page): Page? {
        val index = pages.indexOf(page)
        return if (index > 0) {
            pages[index - 1]
        } else null
    }

    internal fun next(page: Page): Page? {
        val index = pages.indexOf(page)
        return if (index < pages.size - 1) {
            pages[index + 1]
        } else null
    }

    private class UpdateOperation(val page: Page, val source: Source<*>, val data: List<Element<*>>)

    private val updates = mutableListOf<UpdateOperation>()

    private fun isInBatchUpdate() = updates.isNotEmpty()

    internal fun isUiThread() = Thread.currentThread() == Looper.getMainLooper().thread

    private var debugUpdate = 0

    @UiThread
    internal fun setResults(page: Page, source: Source<*>, data: List<Element<*>>, sync: Boolean) {
        if (!isUiThread()) throw IllegalStateException("setResults must be called from the UI thread. This signals a lib internal error.")
        val op = UpdateOperation(page, source, data)
        if (sync && !page.isInUpdate()) {
            // Run this now
            performSyncUpdate(op)
        } else if (isInBatchUpdate()) {
            // Just add, it will be run
            updates.add(op)
        } else {
            // Add and post update.
            updates.add(op)
            uiExecutor.post {
                performUpdates()
            }
        }
    }

    @UiThread
    private fun performUpdates() {
        if (!isUiThread()) throw IllegalStateException("performUpdates must be called from the UI thread. This signals a lib internal error.")
        if (!isInBatchUpdate()) throw IllegalStateException("Something is wrong. performUpdates called but no batched updates")
        val ops = updates.toList()
        updates.clear()
        // The executor call could be here, but we can't batch updates from different pages anyway, they are
        // dispatched at different times on the UI thread. So we put the executor inside in order to leverage
        // multithreading.
        ops.asSequence().map { it.page }.distinctBy { it.number }.sortedBy { it.number }.forEach { page ->
            executor.execute {
                performUpdates(page, ops.filter { it.page === page })
            }
        }
    }

    @WorkerThread
    private fun performUpdates(page: Page, ops: List<UpdateOperation>) {
        val adapter = adapter ?: return
        val newList = page.startUpdate("performUpdates")
        val oldList = newList.toList()
        ops.forEach {
            performPageUpdate(adapter, it, newList)
        }
        val callback = DiffCallback(adapter, oldList, newList)
        val result = DiffUtil.calculateDiff(callback, true)
        uiExecutor.post {
            page.endUpdate("performUpdates") {
                result.dispatchUpdatesTo(DiffDispatcher(page))
            }
            debugUpdate++
        }
    }

    /**
     * The sync version: like [performUpdates], but keeps operating on the UI thread
     * and has only a single OP.
     */
    @UiThread
    private fun performSyncUpdate(op: UpdateOperation) {
        if (!isUiThread()) throw IllegalStateException("performSyncUpdate must be called from the UI thread. This signals a lib internal error.")
        val adapter = adapter ?: return
        val page = op.page
        val newList = page.startUpdate("performSyncUpdate")
        val oldList = newList.toList()
        ElementsLogger.v("PageManager performSyncUpdate for update $debugUpdate. Executing. Source: ${op.source::class.java.simpleName}")
        performPageUpdate(adapter, op, newList)
        val callback = DiffCallback(adapter, oldList, newList)
        val result = DiffUtil.calculateDiff(callback, true)
        page.endUpdate("performSyncUpdate") {
            result.dispatchUpdatesTo(DiffDispatcher(page))
        }
        debugUpdate++
    }

    /**
     * This is the update core.
     * Mutates the [newList].
     */
    private fun performPageUpdate(adapter: Adapter, op: UpdateOperation, newList: MutableList<Element<*>>) {
        val page = op.page
        val source = op.source
        val data = op.data

        // Must split the page into elements from me, & elements from my dependencies.
        val dependencies: Set<Source<*>> = adapter.getDependencies(source)
        val fromSource = arrayListOf<Element<*>>()
        val fromDependencies = arrayListOf<Element<*>>()
        newList.forEach {
            if (it.source == source) fromSource.add(it)
            if (dependencies.contains(it.source)) fromDependencies.add(it)
        }
        newList.removeAll(fromSource)

        // Ask the source to order, assuming it has dependencies.
        var dataIndex = 0
        var available = data.size
        fromDependencies.forEachIndexed { index, element ->
            if (available <= 0) return@forEachIndexed
            val before = source.insertBefore(page, fromDependencies, element, index, available)
            val after = source.insertAfter(page, fromDependencies, element, index, available - before)
            available = available - before - after
            if (available < 0) throw RuntimeException("insertBefore or insertAfter returned too much elements. " +
                    "available: $available, before: $before, " +
                    "after: $after, source: ${source.javaClass.simpleName}")
            var pageIndex = newList.indexOf(element) // On the full page.
            repeat(before) {
                newList.add(pageIndex, data[dataIndex])
                pageIndex++
                dataIndex++
            }
            pageIndex++ // Jump after the current item.
            repeat(after) {
                newList.add(pageIndex, data[dataIndex])
                pageIndex++
                dataIndex++
            }
        }

        // If there are items left (all of them if the source had no deps), Just append them.
        repeat(available) {
            newList.add(data[dataIndex])
            dataIndex++
        }
    }

    internal fun notifyPageItemRangeRemoved(page: Page, from: Int, count: Int) {
        adapter?.notifyItemRangeRemoved(countBefore(page) + from, count)
    }

    internal fun notifyPageItemRangeChanged(page: Page, from: Int, count: Int, payload: Any? = null) {
        adapter?.notifyItemRangeChanged(countBefore(page) + from, count, payload)
    }

    internal fun notifyPageItemRangeInserted(page: Page, from: Int, count: Int) {
        val before = countBefore(page)
        adapter?.notifyItemRangeInserted(before + from, count)
    }

    internal fun notifyPageItemInserted(page: Page, position: Int) {
        notifyPageItemRangeInserted(page, position, 1)
    }

    internal fun notifyPageItemRemoved(page: Page, position: Int) {
        notifyPageItemRangeRemoved(page, position, 1)
    }

    internal fun notifyPageItemChanged(page: Page, position: Int) {
        notifyPageItemRangeChanged(page, position, 1)
    }

    internal fun notifyPageItemMoved(page: Page, fromPosition: Int, toPosition: Int) {
        val count = countBefore(page)
        adapter?.notifyItemMoved(count + fromPosition, count + toPosition)
    }

    /**
     * Callback for the DiffUtil during our page updates.
     * Will simply ask the sources if the element is the same or has changed.
     * Elements belonging to different sources are different by default.
     */
    private class DiffCallback(
            private val adapter: Adapter,
            private val oldList: List<Element<*>>,
            private val newList: List<Element<*>>): DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return when {
                old.source !== new.source -> {
                    if (adapter.dependsOn(new.source, old.source)) {
                        new.data != null && cast(new.source).areItemsTheSame(new.data, cast(old.source), old.data)
                    } else if (adapter.dependsOn(old.source, new.source)) {
                        old.data != null && cast(old.source).areItemsTheSame(old.data, cast(new.source), new.data)
                    } else {
                        false
                    }
                }
                old.type != new.type -> false
                old.data == null && new.data == null -> true
                old.data != null && new.data != null -> {
                    cast(old.source).areItemsTheSame(old.data, new.data)
                }
                else -> false
            }
        }

        /**
         * This is called after [areItemsTheSame] return true so we can make some assumptions.
         * Either the sources are different, or both data is null, or both data is not null.
         */
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return when {
                old.source !== new.source -> false
                old.data == null && new.data == null -> true
                else -> cast(old.source).areContentsTheSame(old.data!!, new.data!!)
            }
        }

        /**
         * This is called after [areItemsTheSame] returned true and [areContentsTheSame] return false.
         */
        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return when {
                old.source !== new.source -> {
                    if (adapter.dependsOn(new.source, old.source)) {
                        cast(new.source).getItemsChangePayload(new.data!!, cast(old.source), old.data)
                    } else if (adapter.dependsOn(old.source, new.source)) {
                        cast(old.source).getItemsChangePayload(old.data!!, cast(new.source), new.data)
                    } else {
                        null
                    }
                }
                old.data != null && new.data != null -> cast(old.source).getItemsChangePayload(old.data, new.data)
                else -> null
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun cast(source: Source<*>) = source as Source<Any>
    }

    /**
     * Dispatcher for DiffResults. This will post any update
     * using the page as reference.
     */
    private inner class DiffDispatcher(private val page: Page): ListUpdateCallback {
        override fun onChanged(position: Int, count: Int, payload: Any?) {
            ElementsLogger.v("DiffDispatcher ${hashCode()} onChanged, position: $position, count: $count")
            notifyPageItemRangeChanged(page, position, count, payload)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            ElementsLogger.v("DiffDispatcher ${hashCode()} onMoved, fromPosition: $fromPosition, toPosition: $toPosition")
            notifyPageItemMoved(page, fromPosition, toPosition)
        }

        override fun onInserted(position: Int, count: Int) {
            ElementsLogger.v("DiffDispatcher ${hashCode()} onInserted, position: $position, count: $count")
            notifyPageItemRangeInserted(page, position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            ElementsLogger.v("DiffDispatcher ${hashCode()} onRemoved, position: $position, count: $count")
            notifyPageItemRangeRemoved(page, position, count)
        }
    }
}