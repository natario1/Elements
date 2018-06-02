package com.otaliastudios.elements.extensions

import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source

abstract class FooterSource<Anchor: Any, Footer: Any>() : Source<FooterSource.Data<Anchor, Footer>>() {

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        onPageChanged(page, dependencies)
    }

    override fun onPageChanged(page: Page, dependencies: List<Element<*>>) {
        @Suppress("UNCHECKED_CAST")
        val list = dependencies.filter { it.data != null }.map { it.data } as List<Anchor>
        val headers = computeFooters(page, list)
        postResult(page, headers)
    }

    override fun insertAfter(page: Page, dependencies: List<Element<*>>, element: Element<*>, position: Int, available: Int): Int {
        @Suppress("UNCHECKED_CAST")
        val input = element.data as? Anchor
        val results = getResultsForPage(page)
        return if (results.any { it.data?.anchor == input }) 1 else 0
    }

    protected abstract fun computeFooters(page: Page, list: List<Anchor>): List<Data<Anchor, Footer>>

    override fun getElementType(data: Data<Anchor, Footer>) = ELEMENT_TYPE

    data class Data<Anchor: Any, Footer: Any>(val anchor: Anchor, val footer: Footer)

    companion object {
        public const val ELEMENT_TYPE = 127831783
    }
}