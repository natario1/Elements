package com.otaliastudios.elements.extensions

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Presenter

/**
 * The simplest possible presenter, that just loads a layout resource,
 * responds to a given [elementType], and performs an action when binding.
 *
 * See also [Presenter.simple].
 */
public open class SimplePresenter<T: Any>(
        context: Context,
        private val layout: Int,
        private val elementType: Int,
        private val bind: ((View, T) -> Unit)?
) : Presenter<T>(context) {

    override val elementTypes: List<Int> = listOf(elementType)

    override fun onCreate(parent: ViewGroup, elementType: Int): Holder {
        return Holder(getLayoutInflater().inflate(layout, parent, false))
    }

    override fun onBind(page: Page, holder: Holder, element: Element<T>) {
        super.onBind(page, holder, element)
        bind?.invoke(holder.itemView, element.data!!)
    }
}