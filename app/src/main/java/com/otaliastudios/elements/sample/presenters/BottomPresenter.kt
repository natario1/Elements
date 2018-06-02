package com.otaliastudios.elements.sample.presenters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.extensions.SimplePresenter
import com.otaliastudios.elements.sample.R
import java.lang.reflect.Type

/**
 * A presenter for the bottom horizontal list.
 * This implements some basic selection management.
 * In the future this will be managed by the lib.
 */
class BottomPresenter(context: Context, listener: ((Page, Holder, Element<Int>) -> Unit)? = null) : Presenter<Int>(context) {

    private var selectedPosition: Int = 0

    init {
        setOnElementClickListener({page, holder, element ->
            page.notifyItemChanged(selectedPosition)
            selectedPosition = holder.adapterPosition
            page.notifyItemChanged(selectedPosition)
            listener?.invoke(page, holder, element)
        })
    }

    override fun onCreate(parent: ViewGroup, elementType: Int): Holder {
        return Holder(getLayoutInflater().inflate(R.layout.item_bottom, parent, false))
    }

    override fun onBind(page: Page, holder: Holder, element: Element<Int>) {
        super.onBind(page, holder, element)
        val isSelected = holder.adapterPosition == selectedPosition
        val text = holder.itemView as TextView
        text.setText(element.data!!)
        if (isSelected) {
            text.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            text.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
        } else {
            text.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
            text.setTextColor(Color.GRAY)
        }
    }

    override val elementTypes = listOf(0)
}