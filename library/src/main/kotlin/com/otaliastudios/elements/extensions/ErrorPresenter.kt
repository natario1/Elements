package com.otaliastudios.elements.extensions

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Presenter

/**
 * An [ErrorPresenter] responds to the [MainSource.ELEMENT_TYPE_ERROR] element.
 * See the [MainSource] class for documentation.
 *
 * It is basically meant to display a 'there was an error' indicator.
 * You also have access to the exception that was originally thrown by the source.
 *
 * @property layout the layout resource to be inflated.
 * @property bind what to do when binding the error view.
 */
public open class ErrorPresenter(
        context: Context,
        private val layout: Int,
        private val bind: ((View, Exception) -> Unit)? = null
) : Presenter<Void>(context) {

    override val elementTypes: List<Int> = listOf(MainSource.ELEMENT_TYPE_ERROR)

    override fun onCreate(parent: ViewGroup, elementType: Int): Holder {
        return Holder(getLayoutInflater().inflate(layout, parent, false))
    }

    override fun onBind(page: Page, holder: Holder, element: Element<Void>) {
        super.onBind(page, holder, element)
        bind?.invoke(holder.itemView, element.extra as Exception)
    }
}