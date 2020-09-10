package com.otaliastudios.elements.extensions

import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source

/**
 * A source that already has results inside a list and will display them
 * in a single page.
 */
public open class ListSource<T: Any>(private val list: List<T>, private val elementType: Int = 0) : Source<T>() {

    override fun dependsOn(source: Source<*>): Boolean = false

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        if (page.isFirstPage()) {
            postResult(page, list)
        }
    }

    override fun getElementType(data: T): Int = elementType

    override fun areItemsTheSame(first: T, second: T): Boolean {
        return false
    }

}