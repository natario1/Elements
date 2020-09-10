package com.otaliastudios.elements

import android.annotation.SuppressLint
import android.os.Looper
import androidx.annotation.UiThread
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.collections.ArrayList

/**
 * A [Page] represents and holds element data for a single page.
 * It provides utilities to update the page data on the fly, and methods
 * to query the current page state.
 *
 * Synchronization:
 * This is how it should be:
 *
 * - AnyThread: Users must notify that an update has started ([startUpdate]).
 *   At this point we acquire the lock.
 *
 * - AnyThread: We copy the list of elements to a working, WIP list.
 *   During this time, if the adapter queries us, we return the old list which makes sense.
 *
 * - AnyThread: The user modifies the WIP list.
 *
 * - AnyThread: Users notify that the update has ended ([endUpdate]). We post the following action
 *   to the UI thread. If we are on the UI thread already, we just execute it:
 *
 *   - UiThread: Copy back the WIP list to the main list (it's actually an assignment so it's fast)
 *   - UiThread: Call notifyDataSetChanged()
 *   - UiThread: Release the lock
 *
 *   This is not 100% safe to user actions. Once we release the lock, future actions can be started,
 *   but the adapter might still not have queried this page, because notifyDataSetChanged() posts the
 *   changes on the next layout pass.
 *
 *   However, for this to break, The following actions should happen:
 *   - A: We end an update and call notifyDataSetChanged()
 *   - B: We release the lock
 *   - C: Before the adapter runs, we start another update (and aquire the lock)
 *   - D: Before the adapter runs, we END that update (and release the lock)
 *   - E: The adapter runs the old cycle and detects an inconsistency.
 *   Since the time between B and E events is extremely small, I would say that this is extremely
 *   unlikely. If this proves to be a problem, we should release the lock in the next UI pass,
 *   but this assumes that notify() behavior does not changes.
 *
 * The above is the policy for updates coming from [PageManager].
 * As said, they can start from any thread, but the lock releasing is streamlined to
 * the UI thread.
 *
 * This means that there is a potential deadlock when start() is called from the UI thread,
 * because if there's another update going on, start() will wait for it, but the previous update
 * will wait for the UI thread itself to end. So UI starts() are a problem.
 *
 * - When they come from lib users (Page APIs): we use the dirty [executePageApi]
 * - When they come from PageManager with the sync option: this is 'addressed' ignoring the sync flag
 *   if the page is in update. But that function is not synchronized.
 *
 * Should find another solution for both, and that does this without creating new threads or
 * streamlining starts().
 */
public class Page internal constructor(pageManager: PageManager, internal val number: Int) {

    private val log = ElementsLogger("Page($number)")
    private var pageManager: PageManager? = pageManager
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
    public fun elementCount(): Int = elements.size

    internal fun elementAt(position: Int) = elements[position]

    /**
     * Returns the previous [Page], if there is one,
     * or null if there isn't.
     */
    public fun previous(): Page? = pageManager?.previous(this)

    /**
     * Returns the next [Page], if there is one,
     * or null if there isn't.
     */
    public fun next(): Page? = pageManager?.next(this)

    /**
     * Returns true if this is the first page,
     * false otherwise.
     */
    public fun isFirstPage(): Boolean = previous() == null

    /**
     * Returns true if this is the last page,
     * false otherwise.
     */
    public fun isLastPage(): Boolean = next() == null


    internal fun isInUpdate() = semaphore.availablePermits() == 0

    internal fun isUiThread() = PageManager.isUiThread()

    /**
     * To avoid deadlocks, since locks are released on the UI thread,
     * this must be called either from a WorkerThread, or from the UI thread
     * when there are no current updates.
     */
    internal fun startUpdate(debug: String): MutableList<Element<*>> {
        log("startUpdate", debug)
        if (isUiThread() && isInUpdate()) {
            throw RuntimeException("Deadlock detected in page $this during update $debug." +
                    " Probably coming from PageManager with sync flag?")
        }
        semaphore.acquire()
        rawElements = ArrayList(elements) // Important.
        return rawElements
    }

    @UiThread
    internal fun endUpdate(debug: String, notifyAction: ((PageManager) -> Unit)?) {
        log("endUpdate", debug)
        pageManager?.let {
            elements = rawElements
            notifyAction?.invoke(it)
        }
        semaphore.release()
    }

    private fun log(event: String, message: String) {
        log.v { "$event: el=${elements.size} rawEl=${rawElements.size} ui=${isUiThread()} msg=$message" }
    }


    // This is not the most elegant thing I have ever seen.
    private fun executePageApi(action: () -> Unit) {
        if (!isUiThread()) {
            throw IllegalStateException("Page APIs must be called from the UI thread.")
        }
        if (!isInUpdate()) {
            action()
        } else {
            PageManager.uiExecutor.post {
                executePageApi(action)
            }
        }
    }

    internal fun release(): Int {
        pageManager = null
        val count = elementCount()
        elements.clear()
        return count

        /* val manager = pageManager ?: return
        pageManager = null
        if (!isUiThread()) {
            throw IllegalStateException("Page release() must be called from the UI thread.")
        }
        manager.notifyPageItemRangeRemoved(this, 0, elementCount()) */
        // This should be reasonably safe. Even if we are in a update, the logic in endUpdate
        // will not update the original list, making pending updates just useless.
        // Again, there's no strict synchronization going on.
    }

    /**
     * Clear this page from any object currently present.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread // API
    public fun clear() {
        executePageApi {
            val message = "clear"
            val list = startUpdate(message)
            val count = list.size
            list.clear()
            endUpdate(message) {
                it.notifyPageItemRangeRemoved(this, 0, count)
            }
        }
    }

    /**
     * Insert an element at the specified position in this page.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread // API
    public fun insertElement(position: Int, element: Element<*>) {
        executePageApi {
            val message = "insertElement position $position"
            val list = startUpdate(message)
            if (position >= 0 && position <= list.size) {
                list.add(position, element)
                endUpdate(message) {
                    it.notifyPageItemInserted(this, position)
                }
            } else {
                endUpdate(message, null)
            }
        }
    }

    /**
     * Remove an element at the specified position in this page.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread // API
    public fun removeElement(position: Int) {
        executePageApi {
            val message = "removeElement position $position"
            val list = startUpdate(message)
            if (position >= 0 && position < list.size) {
                list.removeAt(position)
                endUpdate(message) {
                    it.notifyPageItemRemoved(this, position)
                }
            } else {
                endUpdate(message, null)
            }
        }
    }

    /**
     * Remove the specified element from this page, if present.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread // API
    public fun removeElement(element: Element<*>) {
        executePageApi {
            val message = "removeElement element $element"
            val list = startUpdate(message)
            val index = list.indexOfFirst { it === element }
            if (index >= 0) {
                list.removeAt(index)
                endUpdate(message) {
                    it.notifyPageItemRemoved(this, index)
                }
            } else {
                endUpdate(message, null)
            }
        }
    }

    /**
     * Replaces the specified element with another.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread // API
    public fun replaceElement(element: Element<*>, replacement: Element<*>) {
        executePageApi {
            val message = "replaceElement $element with $replacement"
            val list = startUpdate(message)
            val position = list.indexOfFirst { it === element }
            if (position >= 0) {
                list[position] = replacement
                endUpdate(message) {
                    it.notifyPageItemChanged(this, position)
                }
            } else {
                endUpdate(message, null)
            }
        }
    }

    /**
     * Inserts the specified elements in this page, starting at the given position,
     * relative to this page.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread // API
    public fun insertElements(position: Int, elements: Collection<Element<*>>) {
        executePageApi {
            val message = "insertElements position $position size ${elements.size}"
            val list = startUpdate(message)
            if (position >= 0 && position <= list.size && elements.isNotEmpty()) {
                list.addAll(position, elements)
                endUpdate(message) {
                    it.notifyPageItemRangeInserted(this, position, elements.size)
                }
            } else {
                endUpdate(message, null)
            }
        }

    }

    /**
     * Replaces elements starting at the given position with the specified elements.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread // API
    public fun replaceElements(position: Int, vararg elements: Element<*>) {
        executePageApi {
            val message = "replaceElements position $position size ${elements.size}"
            val list = startUpdate(message)
            if (position >= 0 && position + elements.size <= list.size && elements.isNotEmpty()) {
                var offset = 0
                for (element in elements) {
                    list[position + offset] = element
                    offset += 1
                }
                endUpdate(message) {
                    it.notifyPageItemRangeChanged(this, position, elements.size)
                }
            }
            else {
                endUpdate(message, null)
            }
        }

    }

    /**
     * Removes up to count elements starting at the given position.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread // API
    public fun removeElements(position: Int, count: Int) {
        executePageApi {
            val message = "removeElements position $position count $count"
            val list = startUpdate(message)
            if (position >= 0 && position + count < list.size && count > 0) {
                var item = 0
                for (i in position until position + count) {
                    list.removeAt(i)
                    item += 1
                }
                endUpdate(message) {
                    it.notifyPageItemRangeRemoved(this, position, count)
                }
            } else {
                endUpdate(message, null)
            }
        }

    }

    /**
     * Notifies that the element at the given position has changed.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread // API
    public fun notifyItemChanged(position: Int) {
        executePageApi {
            val message = "notifyItemChanged position $position"
            val list = startUpdate(message)
            if (position >= 0 && position < list.size) {
                endUpdate(message) {
                    it.notifyPageItemChanged(this, position)
                }
            } else {
                endUpdate(message, null)
            }
        }

    }

    /**
     * Notifies that the element has changed.
     * Waits for other pending updates to finish so this can
     * actually block the UI thread.
     */
    @UiThread // API
    public fun notifyItemChanged(element: Element<*>) {
        executePageApi {
            val message = "notifyItemChanged element $element"
            val list = startUpdate(message)
            // Use strict equality instead of element equals(). If it changed,
            // equals would be always false.
            val index = list.indexOfFirst { it === element }
            if (index >= 0) {
                endUpdate(message) {
                    it.notifyPageItemChanged(this, index)
                }
            } else {
                endUpdate(message, null)
            }
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