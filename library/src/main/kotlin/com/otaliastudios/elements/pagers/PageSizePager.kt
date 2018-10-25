package com.otaliastudios.elements.pagers

import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Pager
import com.otaliastudios.elements.Presenter

open class PageSizePager(val pageSize: Int): Pager() {

    override fun onElementBound(page: Page, element: Element<*>, presenter: Presenter<*>, absolutePosition: Int, pagePosition: Int) {
        if (pagePosition == maxOf(pageSize - 1, 0) && page.isLastPage()) {
            openPage()
        }
    }
}