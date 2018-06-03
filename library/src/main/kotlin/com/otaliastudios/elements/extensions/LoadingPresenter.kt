package com.otaliastudios.elements.extensions

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Presenter

/**
 * A [LoadingPresenter] responds to the [MainSource.ELEMENT_TYPE_LOADING] element.
 * See the [MainSource] class for documentation.
 *
 * It is basically meant to display a loading progress indicator while the pages
 * are loaded.
 *
 * @property layout the layout resource to be inflated.
 * @property bind what to do when binding the loading view.
 */
open class LoadingPresenter(
        context: Context,
        private val layout: Int,
        private val bind: ((View) -> Unit)? = null
) : Presenter<Void>(context) {

    override val elementTypes = listOf(MainSource.ELEMENT_TYPE_LOADING)

    override fun onCreate(parent: ViewGroup, elementType: Int): Holder {
        return Holder(getLayoutInflater().inflate(layout, parent, false))
    }

    override fun onBind(page: Page, holder: Holder, element: Element<Void>) {
        super.onBind(page, holder, element)
        bind?.invoke(holder.itemView)
    }
}