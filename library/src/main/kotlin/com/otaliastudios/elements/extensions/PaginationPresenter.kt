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
 * A [PaginationPresenter] deals with [PaginationSource.ELEMENT_TYPE] elements that are
 * provided by a [PaginationSource].
 *
 * This are meant to be 'Load more' buttons that, when clicked, will automatically
 * prompt the adapter to open a new page.
 *
 * This must be used with an unbound adapter, so we can manage the page creation ourselves.
 */
class PaginationPresenter(
        context: Context,
        private val layout: Int
) : Presenter<Void>(context) {

    override val elementTypes = listOf(PaginationSource.ELEMENT_TYPE)

    override fun onCreate(parent: ViewGroup, elementType: Int): Holder {
        return Holder(getLayoutInflater().inflate(layout, parent, false))
    }

    init {
        setOnElementClickListener { page, _, _ ->
            if (page.isLastPage()) {
                getAdapter().openPage()
            }
        }
    }
}