@file:Suppress("unused")

package com.otaliastudios.elements.pagers

import com.otaliastudios.elements.*
import com.otaliastudios.elements.extensions.MainSource

public open class NoPagesPager: Pager() {
    override fun onElementBound(
            page: Page,
            element: Element<*>,
            presenter: Presenter<*>,
            absolutePosition: Int,
            pagePosition: Int
    ): Unit = Unit
}

public open class PageFractionPager(
        expectedPageSize: Int,
        fraction: Float
): PageSizePager((expectedPageSize * fraction).toInt())

public open class PageSizePager(private val pageSize: Int): Pager() {

    override fun onElementBound(
            page: Page,
            element: Element<*>,
            presenter: Presenter<*>,
            absolutePosition: Int,
            pagePosition: Int
    ) {
        if (pagePosition == maxOf(pageSize - 1, 0) && page.isLastPage()) {
            requestPage()
        }
    }
}

public open class SourceResultsPager(
        private val fraction: Float = 0.8F,
        private val selector: (source: Source<*>, elementType: Int) -> Boolean = { source, elementType ->
            source is MainSource
                    && elementType != MainSource.ELEMENT_TYPE_LOADING
                    && elementType != MainSource.ELEMENT_TYPE_EMPTY
                    && elementType != MainSource.ELEMENT_TYPE_ERROR
        }
): Pager() {

    private val log = ElementsLogger("SourceResultsPager")

    override fun onElementBound(page: Page, element: Element<*>, presenter: Presenter<*>, absolutePosition: Int, pagePosition: Int) {
        val source = element.source
        if (page.isLastPage() && selector(source, element.type) && source.hasResultsForPage(page)) {
            val sourceResults = source.getResultsForPage(page)
            val sourcePosition = sourceResults.indexOfFirst { it === element }
            log.v { "source matches and has ${sourceResults.size} results. " +
                    "The position of this element is $sourcePosition. " +
                    "(in page: $pagePosition, absolute: $absolutePosition, data: ${element.data})" }
            if (sourcePosition >= 0) {
                val trigger = (sourceResults.size * fraction).toInt()
                log.v { "comparing with ${trigger - 1}" }
                if (sourcePosition == maxOf(trigger - 1, 0)) {
                    requestPage()
                }
            }
        }
    }
}