package com.otaliastudios.elements.pagers

import com.otaliastudios.elements.*
import com.otaliastudios.elements.extensions.MainSource

open class SourceResultsPager(
        val fraction: Float,
        val selector: (Source<*>) -> Boolean = { it is MainSource }): Pager() {

    override fun onElementBound(page: Page, element: Element<*>, presenter: Presenter<*>, absolutePosition: Int, pagePosition: Int) {
        val source = element.source
        if (page.isLastPage() && selector(source) && source.hasResultsForPage(page)) {
            val sourceResults = source.getResultsForPage(page)
            val sourcePosition = sourceResults.indexOfFirst { it === element }
            if (sourcePosition >= 0) {
                val trigger = (sourceResults.size * fraction).toInt()
                if (sourcePosition == maxOf(trigger - 1, 0)) {
                    openPage()
                }
            }
        }
    }
}