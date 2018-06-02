package com.otaliastudios.elements.extensions

import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source


class PaginationSource(private val dependsOn: ((Source<*>) -> Boolean) = { true }) : Source<Any>() {

    companion object {
        const val ELEMENT_TYPE = 1933811
    }

    override fun dependsOn(source: Source<*>): Boolean {
        return dependsOn.invoke(source)
    }

    override fun getElementType(data: Any) = ELEMENT_TYPE

    override fun areItemsTheSame(first: Any, second: Any): Boolean {
        return first == second
    }

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        if (page.previous() != null) {
            // Remove ourselves from the last page.
            postResult(page.previous()!!, emptyList())
        }
        maybePostResult(page, dependencies)
    }

    override fun onPageChanged(page: Page, dependencies: List<Element<*>>) {
        // Repost: we need to reorder ourselves.
        maybePostResult(page, dependencies)
    }

    // Ensure that we have at least some element and that it has some data.
    // This works better with MainSource. We don't want the button if the page is a loading indicator.
    private fun maybePostResult(page: Page, dependencies: List<Element<*>>) {
        if (dependencies.isNotEmpty() && dependencies.any { it.data != null }) {
            postResult(page, listOf(createEmptyElement(ELEMENT_TYPE)))
        }
    }

    override fun insertAfter(page: Page, dependencies: List<Element<*>>, element: Element<*>, position: Int, available: Int): Int {
        if (position == dependencies.lastIndex && available > 0) {
            return 1
        }
        return 0
    }
}