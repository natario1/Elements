package com.otaliastudios.elements

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import com.otaliastudios.elements.extensions.ListSource
import com.otaliastudios.elements.extensions.LiveDataSource

abstract class Source<T: Any> {

    private val map: MutableMap<Page, ResultProvider> = mutableMapOf()
    private val keys: MutableMap<Page, Any?> = mutableMapOf()

    protected fun createElement(data: T, extra: Any? = null): Element<T> {
        return Element(this, getElementType(data), data, extra)
    }

    protected fun createElements(data: Collection<T>): List<Element<T>> {
        return data.map { createElement(it) }
    }

    protected fun createElementWithType(type: Int, data: T?, extra: Any? = null): Element<T> {
        return Element(this, type, data, extra)
    }

    protected fun postResult(page: Page, result: Result<T>) {
        map[page]!!.postValue(result)
    }

    protected fun postResult(page: Page, data: Collection<T>) {
        postResult(page, Result(createElements(data)))
    }

    protected fun postResult(page: Page, data: List<Element<T>>) {
        postResult(page, Result(data))
    }

    protected fun postResult(page: Page, error: Exception) {
        postResult(page, Result(listOf(), error))
    }

    protected fun postResult(page: Page, data: LiveData<List<T>>) {
        map[page]!!.attach(data)
    }

    protected fun setKey(page: Page, key: Any) {
        keys[page] = key
    }

    protected fun <K: Any> getKey(page: Page): K? {
        @Suppress("UNCHECKED_CAST")
        return keys[page] as? K
    }

    internal fun knowsPage(page: Page) = map.containsKey(page)

    internal fun getKnownPages() = map.keys

    internal fun hasResultsForPage(page: Page) = knowsPage(page) && map[page]!!.value != null

    internal fun getResultsForPage(page: Page) = map[page]!!.value!!

    internal fun getCurrentResults(): Map<Page, MutableLiveData<List<Element<T>>>> = map

    internal fun openPage(page: Page, dependencies: List<Element<*>>): LiveData<List<Element<T>>> {
        if (knowsPage(page)) throw RuntimeException("Opening an already opened page!")
        map[page] = ResultProvider(page)
        onPageOpened(page, dependencies)
        return map[page]!!
    }

    public open fun onPageOpened(page: Page, dependencies: List<Element<*>>) {}

    public open fun onPageChanged(page: Page, dependencies: List<Element<*>>) {}

    public open fun dependsOn(source: Source<*>): Boolean = false

    public open fun getElementType(data: T): Int = 0

    public open fun insertBefore(page: Page, dependencies: List<Element<*>>, element: Element<*>, position: Int, available: Int): Int {
        return 0
    }

    public open fun insertAfter(page: Page, dependencies: List<Element<*>>, element: Element<*>, position: Int, available: Int): Int {
        return 0
    }

    public abstract fun areItemsTheSame(first: T, second: T): Boolean

    public open fun areContentsTheSame(first: T, second: T) = first == second

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

    protected open fun onPostResult(page: Page, result: Result<T>): List<Element<T>> {
        return result.values
    }

    companion object {
        fun <T: Any> fromList(list: List<T>, elementType: Int = 0) = ListSource(list, elementType)
        fun <T: Any> fromLiveData(data: LiveData<List<T>>, elementType: Int = 0) = LiveDataSource(data, elementType)
    }
}