package com.otaliastudios.elements.extensions

import android.content.Context
import android.view.ViewGroup
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
public open class PaginationPresenter(
        context: Context,
        private val layout: Int
) : Presenter<Unit>(context) {

    override val elementTypes: List<Int> = listOf(PaginationSource.ELEMENT_TYPE)

    override fun onCreate(parent: ViewGroup, elementType: Int): Holder {
        return Holder(getLayoutInflater().inflate(layout, parent, false))
    }

    init {
        setOnElementClickListener { page, _, _ ->
            if (page.isLastPage()) {
                getAdapter().requestPage()
            }
        }
    }
}