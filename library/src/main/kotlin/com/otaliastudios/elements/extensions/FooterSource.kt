package com.otaliastudios.elements.extensions

import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source

/**
 * An abstract source class that helps in adding elements *below* elements of another source.
 * This works by declaring a dependency to the other source using [dependsOn],
 * then using a simple implementation of [insertAfter].
 *
 * Subclasses must simple implement [computeFooters]: cycle through the dependency data
 * and identify the anchors and the data we are interested in displaying.
 *
 * This source emits instances of [Data] objects, that give access to both the anchor
 * and the footer data.
 *
 * @param Anchor the type for the dependency list
 * @param Footer the type for the footer list (most frequently, a String or Date)
 */
abstract class FooterSource<Anchor: Any, Footer: Any> : Source<FooterSource.Data<Anchor, Footer>>() {

    /**
     * A [Data] object will be passed to presenters to have access to both the anchor
     * and the footer data.
     *
     * @property anchor the anchor data as computed by [computeFooters]
     * @property footer the footer data extracted from the anchors
     */
    public data class Data<Anchor: Any, Footer: Any>(val anchor: Anchor, val footer: Footer)

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

    override fun getElementType(data: Data<Anchor, Footer>) = ELEMENT_TYPE

    /**
     * Implement to extract footers and anchors from the list of dependency data.
     * For example, for a list of objects that are divided in groups, the anchor
     * will be the last item of each group.
     */
    protected abstract fun computeFooters(page: Page, list: List<Anchor>): List<Data<Anchor, Footer>>

    companion object {

        /**
         * The element type used by this source.
         * Can be changed by overriding [getElementType].
         */
        public const val ELEMENT_TYPE = 127831783
    }
}