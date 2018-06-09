package com.otaliastudios.elements

import androidx.annotation.UiThread
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.concurrent.withLock

/**
 * A [Page] represents and holds element data for a single page.
 * It provides utilities to update the page data on the fly, and methods
 * to query the current page state.
 *
 * It uses a [Semaphore] and two different lists to manage and lock updates.
 */
public class Page internal constructor(internal val pager: Pager, internal val number: Int) {

    private val semaphore = Semaphore(1, true)
    private var elements = arrayListOf<Element<*>>()
    private var rawElements = arrayListOf<Element<*>>()

    /**
     * An immutable list of the elements in this page.
     * Each time you call this a list is created, which might be expensive.
     */
    public fun elements(): List<Element<*>> = Collections.unmodifiableList(elements)

    /**
     * The current size, not considering any pending update.
     */
    public fun elementCount() = elements.size

    internal fun elementAt(position: Int) = elements[position]

    /**
     * Returns the previous [Page], if there is one,
     * or null if there isn't.
     */
    public fun previous(): Page? = pager.previous(this)

    /**
     * Returns the next [Page], if there is one,
     * or null if there isn't.
     */
    public fun next(): Page? = pager.next(this)

    /**
     * Returns true if this is the first page,
     * false otherwise.
     */
    public fun isFirstPage() = previous() == null

    /**
     * Returns true if this is the last page,
     * false otherwise.
     */
    public fun isLastPage() = next() == null

    /**
     * The order should be:
     * - acquire the lock (Worker?)
     * - edit the raw elements (Worker?)
     * - set raw elements to elements (UI)
     * - release the lock (UI)
     * - notify the adapter (UI)
     *
     * We *could* probably notify the adapter before calling endUpdate(),
     * because notify* will post updates on the next UI cycle as far as I know.
     * This would make code here more readable. Anyway, let's not mess things
     * up right now.
     */
    internal fun startUpdate(): MutableList<Element<*>> {
        semaphore.acquire()
        rawElements = ArrayList(elements) // Important.
        return rawElements
    }

    internal fun endUpdate() {
        elements = rawElements
        semaphore.release()
    }

    /**
     * Clear this page from any object currently present.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread
    public fun clear() {
        val list = startUpdate()
        val count = list.size
        list.clear()
        endUpdate()
        pager.notifyPageItemRangeRemoved(this, 0, count)
    }

    /**
     * Insert an element at the specified position in this page.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread
    public fun insertElement(position: Int, element: Element<*>) {
        val list = startUpdate()
        if (position >= 0 && position <= list.size) {
            list.add(position, element)
            endUpdate()
            pager.notifyPageItemInserted(this, position)
        } else {
            endUpdate()
        }
    }

    /**
     * Remove an element at the specified position in this page.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread
    public fun removeElement(position: Int) {
        val list = startUpdate()
        if (position >= 0 && position < list.size) {
            list.removeAt(position)
            endUpdate()
            pager.notifyPageItemRemoved(this, position)
        } else {
            endUpdate()
        }
    }

    /**
     * Remove the specified element from this page, if present.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread
    public fun removeElement(element: Element<*>) {
        val list = startUpdate()
        val index = list.indexOf(element)
        if (index >= 0) {
            list.removeAt(index)
            endUpdate()
            pager.notifyPageItemRemoved(this, index)
        } else {
            endUpdate()
        }
    }

    /**
     * Replaces the specified element with another.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread
    fun replaceElement(item: Element<*>, withItem: Element<*>) {
        val list = startUpdate()
        val position = list.indexOf(item)
        if (position >= 0) {
            list[position] = withItem
            endUpdate()
            pager.notifyPageItemChanged(this, position)
        } else {
            endUpdate()
        }
    }

    /**
     * Inserts the specified elements in this page, starting at the given position,
     * relative to this page.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread
    fun insertElements(position: Int, elements: Collection<Element<*>>) {
        val list = startUpdate()
        if (position >= 0 && position <= list.size && elements.isNotEmpty()) {
            list.addAll(position, elements)
            endUpdate()
            pager.notifyPageItemRangeInserted(this, position, elements.size)
        } else {
            endUpdate()
        }
    }

    /**
     * Replaces elements starting at the given position with the specified elements.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread
    fun replaceElements(position: Int, vararg elements: Element<*>) {
        val list = startUpdate()
        if (position >= 0 && position + elements.size <= list.size && elements.isNotEmpty()) {
            var offset = 0
            for (element in elements) {
                list[position + offset] = element
                offset += 1
            }
            endUpdate()
            pager.notifyPageItemRangeChanged(this, position, elements.size)
        }
         else {
            endUpdate()
        }
    }

    /**
     * Removes up to count elements starting at the given position.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread
    fun removeElements(position: Int, count: Int) {
        val list = startUpdate()
        if (position >= 0 && position + count < list.size && count > 0) {
            var item = 0
            for (i in position until position + count) {
                list.removeAt(i)
                item += 1
            }
            endUpdate()
            pager.notifyPageItemRangeRemoved(this, position, count)
        } else {
            endUpdate()
        }
    }

    /**
     * Notifies that the element at the given position has changed.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread
    fun notifyItemChanged(position: Int) {
        val list = startUpdate()
        if (position >= 0 && position < list.size) {
            endUpdate()
            pager.notifyPageItemChanged(this, position)
        } else {
            endUpdate()
        }
    }

    /**
     * Notifies that the element has changed.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread
    fun notifyItemChanged(element: Element<*>) {
        val list = startUpdate()
        val index = list.indexOf(element)
        if (index >= 0) {
            endUpdate()
            pager.notifyPageItemChanged(this, index)
        } else {
            endUpdate()
        }
    }

    /**
     * We need a fast implementation for equals().
     */
    override fun equals(other: Any?): Boolean {
        return other != null && other is Page && other.number == number
    }

    /**
     * A fast hash code function.
     */
    override fun hashCode(): Int {
        return number
    }

    /**
     * Provides details about this page.
     */
    override fun toString(): String {
        return "Page number: $number, elements: ${elementCount()}, updating: ${semaphore.availablePermits() == 0}"
    }
}