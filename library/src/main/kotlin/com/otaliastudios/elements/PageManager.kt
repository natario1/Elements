package com.otaliastudios.elements

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * TODO remove bind / unbind, now this class is View-related, dies with the Adapter.
 */
internal class PageManager {

    private val pages = mutableListOf<Page>()
    private var adapter: Adapter? = null

    companion object {
        // Max pool size is ignored when using LinkedBlockingQueue. Using the 'unbounded queue' strategy:
        // https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ThreadPoolExecutor.html

        private val threadCount = AtomicInteger(1)
        private val threadFactory = ThreadFactory { r -> Thread(r, "Elements PageManager #" + threadCount.getAndIncrement()) }

        private val executor = ThreadPoolExecutor(5, 100,
                10L, TimeUnit.SECONDS,
                LinkedBlockingQueue(), threadFactory)

        private val uiExecutor = Handler(Looper.getMainLooper())
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

    internal fun forEachPage(action: (Page) -> Unit) {
        pages.forEach(action)
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

    internal var update = 0

    internal fun setResults(page: Page, source: Source<*>, data: List<Element<*>>) {
        executor.execute {
            val adapter = adapter ?: return@execute
            val list = page.startUpdate()
            val oldList = ArrayList(list)
            ElementsLogger.v("PageManager Update $update started by source ${source::class.java.simpleName}, items: ${data.size}")

            // Must split the page into elements from me, & elements from my dependencies.
            val dependencies: Set<Source<*>> = adapter.getDependencies(source)
            val fromSource = arrayListOf<Element<*>>()
            val fromDependencies = arrayListOf<Element<*>>()
            list.forEach {
                if (it.source == source) fromSource.add(it)
                if (dependencies.contains(it.source)) fromDependencies.add(it)
            }
            list.removeAll(fromSource)

            // Ask the source to order, assuming it has dependencies.
            var dataIndex = 0
            var available = data.size
            fromDependencies.forEachIndexed { index, element ->
                val before = source.insertBefore(page, fromDependencies, element, index, available)
                val after = source.insertAfter(page, fromDependencies, element, index, available - before)
                available = available - before - after
                if (available < 0) throw RuntimeException("insertBefore or insertAfter returned too much elements. " +
                        "available: $available, " +
                        "before: $before, " +
                        "after: $after, " +
                        "source: ${source.javaClass.simpleName}")
                var pageIndex = list.indexOf(element) // On the full page.
                repeat(before) {
                    list.add(pageIndex, data[dataIndex])
                    pageIndex++
                    dataIndex++
                }
                pageIndex++ // Jump after the current item.
                repeat(after) {
                    list.add(pageIndex, data[dataIndex])
                    pageIndex++
                    dataIndex++
                }
            }

            // If there are items left (all of them if the source had no deps),
            // Just append them.
            repeat(available) {
                list.add(data[dataIndex])
                dataIndex++
            }

            // Log.w("PageManager", "Update $update: OldList elements: ${oldList.joinToString(prefix = "[", postfix = "]", transform = { it.data.toString() })}")
            // Log.w("PageManager", "Update $update: NewList elements: ${list.joinToString(prefix = "[", postfix = "]", transform = { it.data.toString() })}")
            val callback = DiffCallback(oldList, list)
            val result = DiffUtil.calculateDiff(callback, true)
            uiExecutor.post {
                // Log.e("PageManager", "Update $update: Dispatched updates to adapter. 1")
                page.endUpdate()
                // Log.e("PageManager", "Update $update: Dispatched updates to adapter. 2")
                result.dispatchUpdatesTo(DiffDispatcher(page))
                // Log.e("PageManager", "Update $update: Dispatched updates to adapter. 3")
                update++
            }
        }
    }


    internal fun notifyPageItemRangeRemoved(page: Page, from: Int, count: Int) {
        adapter?.notifyItemRangeRemoved(countBefore(page) + from, count)
    }

    internal fun notifyPageItemRangeChanged(page: Page, from: Int, count: Int) {
        adapter?.notifyItemRangeChanged(countBefore(page) + from, count)
    }

    internal fun notifyPageItemRangeInserted(page: Page, from: Int, count: Int) {
        val before = countBefore(page)
        // Log.i("PageManager","NotifyInserted: countBefore is $before")
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
    private inner class DiffCallback(private val oldList: List<Element<*>>, private val newList: List<Element<*>>): DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.source == newItem.source &&
                    oldItem.type == newItem.type &&
                    ((oldItem.data == null && newItem.data == null) ||
                            (oldItem.data != null && newItem.data != null &&
                            cast(oldItem.source).areItemsTheSame(oldItem.data, newItem.data)))
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.source == newItem.source &&
                    oldItem.type == newItem.type &&
                    ((oldItem.data == null && newItem.data == null) ||
                            (oldItem.data != null && newItem.data != null &&
                                    cast(oldItem.source).areContentsTheSame(oldItem.data, newItem.data)))
        }

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        @Suppress("UNCHECKED_CAST")
        private fun cast(source: Source<*>) = source as Source<Any>
    }

    /**
     * Dispatcher for DiffResults. This will post any update
     * using the page as reference.
     */
    private inner class DiffDispatcher(private val page: Page): ListUpdateCallback {
        override fun onChanged(position: Int, count: Int, payload: Any?) {
            ElementsLogger.v("DiffDispatcher onChanged, position: $position, count: $count")
            notifyPageItemRangeChanged(page, position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            ElementsLogger.v("DiffDispatcher onMoved, fromPosition: $fromPosition, toPosition: $toPosition")
            notifyPageItemMoved(page, fromPosition, toPosition)
        }

        override fun onInserted(position: Int, count: Int) {
            ElementsLogger.v("DiffDispatcher onInserted, position: $position, count: $count")
            notifyPageItemRangeInserted(page, position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            ElementsLogger.v("DiffDispatcher onRemoved, position: $position, count: $count")
            notifyPageItemRangeRemoved(page, position, count)
        }
    }
}