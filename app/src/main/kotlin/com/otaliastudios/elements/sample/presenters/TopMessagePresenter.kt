package com.otaliastudios.elements.sample.presenters

import android.content.Context
import android.widget.TextView
import com.otaliastudios.elements.AnimationType
import com.otaliastudios.elements.extensions.SimplePresenter
import com.otaliastudios.elements.sample.R

class TopMessagePresenter(context: Context) : SimplePresenter<String>(
        context, R.layout.item_top, 1, { view, data -> (view as TextView).text = data }
) {

    override fun animates(animation: AnimationType, holder: Holder) = false
}