package com.otaliastudios.elements.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source

/**
 * A source that provides results through a LiveData object and will display them
 * in a single page.
 */
open class LiveDataSource<T: Any>(private val data: LiveData<List<T>>, private val elementType: Int = 0) : Source<T>() {

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        if (page.previous() == null) {
            postResult(page, data)
        }
    }

    override fun dependsOn(source: Source<*>) = false

    override fun getElementType(data: T) = elementType

    override fun areItemsTheSame(first: T, second: T): Boolean {
        return first == second
    }
}