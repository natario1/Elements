package com.otaliastudios.elements.extensions

import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source

/**
 * A [PaginationSource] depends on some kind of main source and emits a single element
 * with the [ELEMENT_TYPE] type at the end of each page.
 *
 * This is meant to be used with unbound adapters in conjunction with [PaginationPresenter].
 * The provided element should be rendered as a 'Load more...' button.
 */
public class PaginationSource(private val dependsOn: ((Source<*>) -> Boolean) = { true }) : Source<Any>() {

    public companion object {

        /**
         * The type for the element that we provide.
         */
        public const val ELEMENT_TYPE: Int = 1933811
    }

    override fun dependsOn(source: Source<*>): Boolean {
        return dependsOn.invoke(source)
    }

    override fun getElementType(data: Any): Int = ELEMENT_TYPE

    override fun areItemsTheSame(first: Any, second: Any): Boolean {
        return first == second
    }

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        if (!page.isFirstPage()) { // Remove ourselves from the last page.
            postResult(page.previous()!!, emptyList())
        }
        maybePostResult(page, dependencies)
    }

    override fun onPageChanged(page: Page, dependencies: List<Element<*>>) {
        // Repost: we need to reorder ourselves.
        if (!page.isFirstPage()) { // Remove ourselves from the last page.
            postResult(page.previous()!!, emptyList())
        }
        maybePostResult(page, dependencies)
    }

    // Ensure that we have at least some element and that it has some data.
    // This works better with MainSource. We don't want the button if the page is a loading indicator.
    private fun maybePostResult(page: Page, dependencies: List<Element<*>>) {
        if (page.isLastPage()
                && dependencies.any { it.data != null }
                && dependencies.all { it.source.canOpenNextPage(page) }) {
            postResult(page, listOf(createEmptyElement(ELEMENT_TYPE)))
        } else {
            postResult(page, listOf())
        }
    }

    override fun insertAfter(page: Page, dependencies: List<Element<*>>, element: Element<*>, position: Int, available: Int): Int {
        if (position == dependencies.lastIndex && available > 0) {
            return 1
        }
        return 0
    }
}