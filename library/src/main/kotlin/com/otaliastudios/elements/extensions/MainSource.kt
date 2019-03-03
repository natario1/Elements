package com.otaliastudios.elements.extensions

import androidx.annotation.CallSuper
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source

/**
 * A [MainSource] provides utilities to deal with common needs when displaying lists.
 * It emits special [Element] objects that make paged navigation more pleasant:
 *
 * - when the list is empty, it emits [ELEMENT_TYPE_EMPTY]. You can then display an empty view using [EmptyPresenter]
 * - when there was an error, it emits [ELEMENT_TYPE_ERROR]. You can then display an error view using [ErrorPresenter]
 * - when the page was opened, it emits [ELEMENT_TYPE_LOADING]. You can then display loading indicators using [LoadingPresenter]
 *
 * You typically don't want more than one [MainSource] in a single list.
 *
 * @property loadingIndicatorsEnabled whether we should add loading indicators when the page is opened and changed
 * @property errorIndicatorEnabled whether we should emit an error indicator when the source posts an error
 * @property emptyIndicatorEnabled whether we should emit an empty indicator when the source posts an empty list
 */
abstract class MainSource<T: Any>(
        protected var loadingIndicatorsEnabled: Boolean = true,
        protected var errorIndicatorEnabled: Boolean = true,
        protected var emptyIndicatorEnabled: Boolean = true
) : Source<T>() {

    companion object {
        /**
         * Constant for the loading element that is provided during
         * data fetching.
         */
        const val ELEMENT_TYPE_LOADING = 1936817

        /**
         * Constant for the empty element that is provided when the
         * list for the first page was empty.
         */
        const val ELEMENT_TYPE_EMPTY = 1936818

        /**
         * Constant for the error element that is provided when the
         * list for the first page gave an Exception.
         */
        const val ELEMENT_TYPE_ERROR = 1936819
    }

    @CallSuper
    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        if (loadingIndicatorsEnabled) notifyLoading(page)
    }

    @CallSuper
    override fun onPageChanged(page: Page, dependencies: List<Element<*>>) {
        if (loadingIndicatorsEnabled) notifyLoading(page)
    }

    /**
     * When [loadingIndicatorsEnabled] is true (default), this is called automatically
     * during [onPageOpened] and [onPageChanged] events. Provides a temporary loading
     * element.
     */
    protected open fun notifyLoading(page: Page) {
        val list = listOf(createEmptyElement(ELEMENT_TYPE_LOADING))
        postResult(page, list)
    }

    override fun onPostResult(page: Page, result: Result<T>): List<Element<T>> {
        val hasResults = result.values.isNotEmpty()
        val hasError = result.error != null

        // If first and only page, add the error/empty indicators.
        if (page.isFirstPage() && page.isLastPage()) {
            if (hasError && errorIndicatorEnabled) {
                return listOf(createEmptyElement(ELEMENT_TYPE_ERROR, result.error))
            } else if (!hasResults && emptyIndicatorEnabled) {
                return listOf(createEmptyElement(ELEMENT_TYPE_EMPTY))
            }
        }

        // If second page, remove stuff from the previous page,
        // as long as the new page has data.
        if (!page.isFirstPage()
                && page.previous()!!.isFirstPage()
                && hasResults) {
            val first = page.previous()!!
            if (errorIndicatorEnabled) {
                first.elements().filter {
                    it.type == ELEMENT_TYPE_ERROR && it.source === this
                }.forEach {
                    first.removeElement(it)
                }
            }
            if (emptyIndicatorEnabled) {
                first.elements().filter {
                    it.type == ELEMENT_TYPE_EMPTY && it.source === this
                }.forEach {
                    first.removeElement(it)
                }
            }
        }
        return super.onPostResult(page, result)
    }

}