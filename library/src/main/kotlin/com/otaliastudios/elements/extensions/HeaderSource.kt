package com.otaliastudios.elements.extensions

import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source
import com.otaliastudios.elements.extensions.FooterSource.Data

/**
 * An abstract source class that helps in adding elements *above* elements of another source.
 * This works by declaring a dependency to the other source using [dependsOn],
 * then using a simple implementation of [insertBefore].
 *
 * Subclasses must simple implement [computeHeaders]: cycle through the dependency data
 * and identify the anchors and the data we are interested in displaying.
 *
 * This source emits instances of [Data] objects, that give access to both the anchor
 * and the header data.
 *
 * @param Anchor the type for the dependency list
 * @param Header the type for the header list (most frequently, a String or Date)
 */
abstract class HeaderSource<Anchor: Any, Header: Any>() : Source<HeaderSource.Data<Anchor, Header>>() {

    /**
     * A [Data] object will be passed to presenters to have access to both the anchor
     * and the header data.
     *
     * @property anchor the anchor data as computed by [computeHeaders]
     * @property header the header data extracted from the anchors
     */
    data class Data<Anchor: Any, Header: Any>(val anchor: Anchor, val header: Header)

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

    override fun getElementType(data: Data<Anchor, Header>) = ELEMENT_TYPE

    /**
     * Implement to extract headers and anchors from the list of dependency data.
     * For example, for an alphabetically ordered list of Strings, you might want to extract
     * the first elements to start with a new letter, and use the letter as a header.
     */
    protected abstract fun computeHeaders(page: Page, list: List<Anchor>): List<Data<Anchor, Header>>

    companion object {

        /**
         * The element type used by this source.
         * Can be changed by overriding [getElementType].
         */
        public const val ELEMENT_TYPE = 127831782
    }
}