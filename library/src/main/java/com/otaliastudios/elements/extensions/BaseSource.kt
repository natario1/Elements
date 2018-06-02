package com.otaliastudios.elements.extensions

import android.support.annotation.CallSuper
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source


abstract class BaseSource<T: Any>(private val loadingEnabled: Boolean = true) : Source<T>() {

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
            val list = listOf(createElementWithType(ELEMENT_TYPE_LOADING, null))
            postResult(page, list)
        }
    }

    override fun onPostResult(page: Page, result: Result<T>): List<Element<T>> {
        if (page.previous() == null) {
            if (result.error != null) {
                val error = result.error
                return listOf(createElementWithType(ELEMENT_TYPE_ERROR, null, error))
            } else if (result.values.isEmpty()) {
                return listOf(createElementWithType(ELEMENT_TYPE_EMPTY, null))
            }
        }
        return super.onPostResult(page, result)
    }

}