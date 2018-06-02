package com.otaliastudios.elements.extensions

import android.content.Context
import android.view.ViewGroup
import com.otaliastudios.elements.Presenter

open class EmptyPresenter(
        context: Context,
        private val layout: Int
) : Presenter<Void>(context) {

    override val elementTypes = listOf(MainSource.ELEMENT_TYPE_EMPTY)

    override fun onCreate(parent: ViewGroup, elementType: Int): Holder {
        return Holder(getLayoutInflater().inflate(layout, parent, false))
    }

}