package com.otaliastudios.elements.extensions

import android.content.Context
import android.view.ViewGroup
import com.otaliastudios.elements.Presenter

/**
 * An [EmptyPresenter] responds to the [MainSource.ELEMENT_TYPE_EMPTY] element.
 * See the [MainSource] class for documentation.
 *
 * It is basically meant to display a 'this list is empty' indicator.
 *
 * @property layout the layout resource to be inflated.
 */
public open class EmptyPresenter(
        context: Context,
        private val layout: Int
) : Presenter<Void>(context) {

    override val elementTypes: List<Int> = listOf(MainSource.ELEMENT_TYPE_EMPTY)

    override fun onCreate(parent: ViewGroup, elementType: Int): Holder {
        return Holder(getLayoutInflater().inflate(layout, parent, false))
    }

}