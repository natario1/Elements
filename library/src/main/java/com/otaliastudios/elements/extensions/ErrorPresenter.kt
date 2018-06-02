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

class ErrorPresenter(
        context: Context,
        private val layout: Int,
        private val bind: ((View, Exception) -> Unit)? = null
) : Presenter<Void>(context) {

    override val elementTypes = listOf(BaseSource.ELEMENT_TYPE_ERROR)

    override fun onCreateHolder(parent: ViewGroup, elementType: Int): Holder {
        return Holder(getLayoutInflater().inflate(layout, parent, false))
    }

    override fun onBind(page: Page, holder: Holder, element: Element<Void>) {
        super.onBind(page, holder, element)
        bind?.invoke(holder.itemView, element.extra as Exception)
    }
}