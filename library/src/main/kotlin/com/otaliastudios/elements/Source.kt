package com.otaliastudios.elements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.otaliastudios.elements.extensions.ListSource
import com.otaliastudios.elements.extensions.LiveDataSource
import com.otaliastudios.elements.extensions.PaginationPresenter
import com.otaliastudios.elements.extensions.PaginationSource
import com.otaliastudios.elements.extensions.DividerSource
import kotlin.reflect.KClass

/**
 * The data provider. Creates [Element]s to be managed by the [Adapter]
 * and dispatched to [Presenter]s that can lay them out.
 *
 * The binding with [Presenter]s is done through the element types.
 * Each source declares the element type of its objects, and each presenter
 * declares the element types that it can deal with.
 *
 * Sources:
 * - receive hook when page is created or modified
 * - post results using one of the [postResult] methods
 * - can override their own results during [onPostResult]
 * - can declare internal dependencies using [dependsOn]
 * - can declare relative ordering inside the page using [insertBefore] and [insertAfter]
 * - help computing the page changes by implementing [areItemsTheSame] and [areContentsTheSame]
 *
 * @param T the model type
 */
abstract class Source<T: Any> {

    private val map: MutableMap<Int, ResultProvider> = mutableMapOf()
    private val keys: MutableMap<Int, Any?> = mutableMapOf()
    private val adapters: MutableList<Adapter> = mutableListOf()

    internal fun knowsPage(page: Page) = map.containsKey(page.number)

    internal fun hasResultsForPage(page: Page) = knowsPage(page) && map[page.number]!!.value != null

    internal fun getResultsForPage(page: Page) = map[page.number]!!.value!!

    internal fun getResults(): Map<Int, MutableLiveData<List<Element<T>>>> = map

    /**
     * Creates an [Element] with the given data.
     * We will ask [getElementType] for the element type.
     */
    protected fun createElement(data: T, extra: Any? = null): Element<T> {
        return Element(this, getElementType(data), data, extra)
    }

    /**
     * Creates a list of [Element]s out of a collection
     * of items.
     */
    protected fun createElements(data: Collection<T>): List<Element<T>> {
        return data.map { createElement(it) }
    }

    /**
     * Creates an element with no data, but with a special element type.
     * This can be useful when using [Element]s as messages for the presenter.
     */
    protected fun createEmptyElement(type: Int, extra: Any? = null): Element<T> {
        return Element(this, type, null, extra)
    }

    /**
     * Provides the element type for the given data.
     * This is called automatically by the framework when constructing elements
     * from items provided by this source.
     */
    public open fun getElementType(data: T): Int = 0

    private fun postResult(page: Page, result: Result<T>) {
        map[page.number]!!.postValue(result)
    }

    /**
     * Posts results for the given page.
     * The collection will be transformed into [Element]s.
     */
    protected fun postResult(page: Page, data: Collection<T>) {
        postResult(page, Result(createElements(data)))
    }

    /**
     * Posts results for the given page.
     */
    protected fun postResult(page: Page, elements: List<Element<T>>) {
        postResult(page, Result(elements))
    }

    /**
     * Posts results for the given page:
     * notifies that there was an error and the page could not
     * be loaded.
     */
    protected fun postResult(page: Page, error: Exception) {
        postResult(page, Result(listOf(), error))
    }

    /**
     * Posts results for the given page:
     * attaches a [LiveData] object to the given page, such that
     * whenever the [LiveData] provides results, we will update the page
     * with them, replacing the old.
     */
    protected fun postResult(page: Page, liveData: LiveData<List<T>>) {
        map[page.number]!!.attach(liveData)
    }

    /**
     * A callback useful to override the provided list of results.
     * For example, this can be used when the original result have an exception,
     * to replace the empty list with an 'error' element.
     */
    protected open fun onPostResult(page: Page, result: Result<T>): List<Element<T>> {
        return result.values
    }

    /**
     * Stores a key of type [Any] for the given page.
     * This is useful for doing meaningful queries.
     *
     * For instance, when quering page X, you might need to know the last element
     * fetched by the previous page.
     */
    protected fun setKey(page: Page, key: Any) {
        keys[page.number] = key
    }

    /**
     * Retrieves a key for the given page. This is typically called
     * on [Page.previous], the page before the current page, to set up
     * a query.
     */
    protected fun <K: Any> getKey(page: Page): K? {
        @Suppress("UNCHECKED_CAST")
        return keys[page.number] as? K
    }

    internal fun openPage(page: Page, dependencies: List<Element<*>>): LiveData<List<Element<T>>> {
        if (knowsPage(page)) throw RuntimeException("Opening an already opened page!")
        map[page.number] = ResultProvider(page)
        onPageOpened(page, dependencies)
        return map[page.number]!!
    }

    /**
     * The [Page] was just opened. At this point, we are required to provide, if present,
     * results for this page using one of the [postResult] methods.
     *
     * If we declared some dependencies using [dependsOn], the list of dependency objects
     * for this page is passed. This also means that this function is not called until all
     * our dependencies have provided data for the page.
     */
    public open fun onPageOpened(page: Page, dependencies: List<Element<*>>) {}

    /**
     * The [Page] contents provided by one of our dependencies has changed.
     * The new list of dependency items is passed.
     *
     * This is never called if we have no dependencies.
     */
    public open fun onPageChanged(page: Page, dependencies: List<Element<*>>) {}

    /**
     * Used to declare dependencies with other sources (using is / instanceof).
     * This has consequences:
     *
     * - we will receive [onPageOpened] after all dependencies have provided objects
     * - we will receive [onPageChanged] callback when some dependency provide updates
     * - we can provide conditional, relative ordering through [insertAfter] and [insertBefore].
     */
    public open fun dependsOn(source: Source<*>): Boolean = false

    /**
     * Called when we declared dependencies, and we must insert our data among the dependency object.
     * The number returned is the number of items to be put before the given [element], at position
     * [position] in a page made of our dependencies elements (the actual page might have more).
     */
    public open fun insertBefore(page: Page, dependencies: List<Element<*>>, element: Element<*>, position: Int, available: Int): Int {
        return 0
    }

    /**
     * Called when we declared dependencies, and we must insert our data among the dependency object.
     * The number returned is the number of items to be put after the given [element], at position
     * [position] in a page made of our dependencies elements (the actual page might have more).
     */
    public open fun insertAfter(page: Page, dependencies: List<Element<*>>, element: Element<*>, position: Int, available: Int): Int {
        return 0
    }

    /**
     * Whether the two items point to the same model entity. It's important to provide
     * an efficient implementation of this to speed up computations and animations.
     * For example, if your objects have an id property, use first.id() == second.id().
     */
    public abstract fun areItemsTheSame(first: T, second: T): Boolean

    /**
     * Whether two objects, that are declared to be the same by [areItemsTheSame],
     * have the same contents. If they haven't, a 'item changed' animation will be performed.
     * This defaults to standard equality.
     */
    public open fun areContentsTheSame(first: T, second: T) = first == second

    /**
     * Request a new page after the given page.
     * This is a no-op if the page is not the last page, and if
     * there are currently no adapters bound to this Source.
     */
    protected fun requestPage(after: Page) {
        if (after.isLastPage()) {
            for (adapter in adapters) {
                adapter.openPage()
            }
        }
    }

    /**
     * Represents the results of a [postResult] call. Includes a possibly empty list of elements,
     * and a possibly null exception.
     * Sources can override the actual result during [onPostResult].
     *
     * @property values the list of elements provided by the source
     * @property error an error provided by the source
     */
    public class Result<T: Any> internal constructor(val values: List<Element<T>>, val error: Exception? = null)

    private inner class ResultProvider(private val page: Page): MediatorLiveData<List<Element<T>>>() {

        private var attachedSource: LiveData<List<T>>? = null

        fun postValue(value: Result<T>?) {
            if (value != null) {
                postValue(onPostResult(page, value))
            } else {
                super.postValue(null)
            }
        }

        fun attach(source: LiveData<List<T>>) {
            if (attachedSource != null) removeSource(attachedSource!!)
            attachedSource = source
            addSource(attachedSource!!, {
                if (it != null) {
                    val elements = createElements(it)
                    postValue(Result(elements))
                }
            })
        }
    }

    internal fun bind(adapter: Adapter) {
        adapters.add(adapter)
    }

    internal fun unbind(adapter: Adapter) {
        adapters.remove(adapter)
    }

    companion object {

        /**
         * Creates a simple [ListSource] that displays a list in a single page.
         */
        fun <T: Any> fromList(list: List<T>, elementType: Int = 0) = ListSource(list, elementType)

        /**
         * Creates a simple [LiveDataSource] that displays results from a [LiveData]
         * object in a single page.
         */
        fun <T: Any> fromLiveData(data: LiveData<List<T>>, elementType: Int = 0) = LiveDataSource(data, elementType)

        /**
         * Creates a [PaginationSource].
         */
        fun forPagination(dependency: Source<*>) = PaginationSource({ it == dependency })

        /**
         * Creates a [DividerSource].
         */
        fun forDividers(dependency: Source<*>) = DividerSource({ it == dependency })
    }
}