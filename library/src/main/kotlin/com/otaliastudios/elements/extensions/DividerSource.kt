package com.otaliastudios.elements.extensions

import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source

/**
 * A [DividerSource] depends on some kind of main source and emits a divider element
 * with the [ELEMENT_TYPE] type for each of the main source.
 *
 * The divider is inserted before each item, except the first item of the first page.
 */
class DividerSource(private val dependsOn: ((Source<*>) -> Boolean) = { true }) : Source<Any>() {

    companion object {

        /**
         * The type for the element that we provide.
         */
        const val ELEMENT_TYPE = 1934812
    }

    override fun dependsOn(source: Source<*>): Boolean {
        return dependsOn.invoke(source)
    }

    override fun getElementType(data: Any) = ELEMENT_TYPE

    override fun areItemsTheSame(first: Any, second: Any): Boolean {
        return first == second
    }

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        maybePostResult(page, dependencies)
    }

    override fun onPageChanged(page: Page, dependencies: List<Element<*>>) {
        // Repost: we need to reorder ourselves.
        maybePostResult(page, dependencies)
    }

    // Ensure that we have at least some element and that it has some data.
    // This works better with MainSource. We don't want the button if the page is a loading indicator.
    private fun maybePostResult(page: Page, dependencies: List<Element<*>>) {
        val valid = dependencies.filter { it.data != null }
        if (valid.size > 1) {
            val count = if (page.isFirstPage()) valid.size - 1 else valid.size
            postResult(page, Array<Element<*>>(count, { createEmptyElement(ELEMENT_TYPE) }).toList())
        } else {
            postResult(page, listOf())
        }
    }

    override fun insertBefore(page: Page, dependencies: List<Element<*>>, element: Element<*>, position: Int, available: Int): Int {
        return if (page.isFirstPage() && position == 0) 0 else Math.min(available, 1)
    }
}