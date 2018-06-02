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

class EmptyPresenter(
        context: Context,
        private val layout: Int
) : Presenter<Void>(context) {

    override val elementTypes = listOf(BaseSource.ELEMENT_TYPE_EMPTY)

    override fun onCreateHolder(parent: ViewGroup, elementType: Int): Holder {
        return Holder(getLayoutInflater().inflate(layout, parent, false))
    }

}