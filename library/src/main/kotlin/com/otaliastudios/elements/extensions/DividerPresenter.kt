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
 * A [DividerPresenter] deals with [DividerSource.ELEMENT_TYPE] elements that are
 * provided by a [DividerSource].
 */
class DividerPresenter(
        context: Context,
        private val layout: Int
) : Presenter<Void>(context) {

    override val elementTypes = listOf(DividerSource.ELEMENT_TYPE)

    override fun onCreate(parent: ViewGroup, elementType: Int): Holder {
        return Holder(getLayoutInflater().inflate(layout, parent, false))
    }
}