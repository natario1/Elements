package com.otaliastudios.elements.extensions

import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source

abstract class HeaderSource<Anchor: Any, Header: Any>() : Source<HeaderSource.Data<Anchor, Header>>() {

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        onPageChanged(page, dependencies)
    }

    override fun onPageChanged(page: Page, dependencies: List<Element<*>>) {
        @Suppress("UNCHECKED_CAST")
        val list = dependencies.filter { it.data != null }.map { it.data } as List<Anchor>
        val headers = computeHeaders(page, list)
        postResult(page, headers)
    }

    override fun insertBefore(page: Page, dependencies: List<Element<*>>, element: Element<*>, position: Int, available: Int): Int {
        @Suppress("UNCHECKED_CAST")
        val input = element.data as? Anchor
        val results = getResultsForPage(page)
        return if (results.any { it.data?.anchor == input }) 1 else 0
    }

    protected abstract fun computeHeaders(page: Page, list: List<Anchor>): List<Data<Anchor, Header>>

    override fun getElementType(data: Data<Anchor, Header>) = ELEMENT_TYPE

    data class Data<Anchor: Any, Header: Any>(val anchor: Anchor, val header: Header)

    companion object {
        public const val ELEMENT_TYPE = 127831782
    }
}