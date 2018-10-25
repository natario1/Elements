package com.otaliastudios.elements.pagers

import com.otaliastudios.elements.*
import com.otaliastudios.elements.extensions.MainSource
import kotlin.math.abs

open class SourceResultsPager(
        val fraction: Float = 0.8F,
        val selector: (source: Source<*>, elementType: Int) -> Boolean = { source, elementType ->
            source is MainSource
                    && elementType != MainSource.ELEMENT_TYPE_LOADING
                    && elementType != MainSource.ELEMENT_TYPE_EMPTY
                    && elementType != MainSource.ELEMENT_TYPE_ERROR
        }): Pager() {

    override fun onElementBound(page: Page, element: Element<*>, presenter: Presenter<*>, absolutePosition: Int, pagePosition: Int) {
        val source = element.source
        if (page.isLastPage() && selector(source, element.type) && source.hasResultsForPage(page)) {
            val sourceResults = source.getResultsForPage(page)
            val sourcePosition = sourceResults.indexOfFirst { it === element }
            ElementsLogger.v("SourceResultsPager: source matches and has ${sourceResults.size} results. " +
                    "The position of this element is $sourcePosition. (in page: $pagePosition, absolute: $absolutePosition, data: ${element.data})")
            if (sourcePosition >= 0) {
                val trigger = (sourceResults.size * fraction).toInt()
                ElementsLogger.w("SourceResultsPager: comparing with ${trigger - 1}")
                if (sourcePosition == maxOf(trigger - 1, 0)) {
                    openPage()
                }
            }
        }
    }
}