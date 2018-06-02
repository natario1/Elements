package com.otaliastudios.elements.extensions

import android.support.annotation.CallSuper
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source


abstract class MainSource<T: Any>(private val loadingEnabled: Boolean = true) : Source<T>() {

    companion object {
        const val ELEMENT_TYPE_LOADING = 1936817
        const val ELEMENT_TYPE_EMPTY = 1936818
        const val ELEMENT_TYPE_ERROR = 1936819
    }

    @CallSuper
    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        notifyLoading(page)
    }

    @CallSuper
    override fun onPageChanged(page: Page, dependencies: List<Element<*>>) {
        notifyLoading(page)
    }

    protected fun notifyLoading(page: Page) {
        if (loadingEnabled) {
            val list = listOf(createEmptyElement(ELEMENT_TYPE_LOADING))
            postResult(page, list)
        }
    }

    override fun onPostResult(page: Page, result: Result<T>): List<Element<T>> {
        if (page.isFirstPage()) {
            if (result.error != null) {
                val error = result.error
                return listOf(createEmptyElement(ELEMENT_TYPE_ERROR, error))
            } else if (result.values.isEmpty()) {
                return listOf(createEmptyElement(ELEMENT_TYPE_EMPTY))
            }
        }
        return super.onPostResult(page, result)
    }

}