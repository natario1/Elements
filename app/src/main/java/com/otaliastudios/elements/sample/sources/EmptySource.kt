package com.otaliastudios.elements.sample.sources

import android.annotation.SuppressLint
import android.os.Handler
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source
import com.otaliastudios.elements.extensions.BaseSource
import com.otaliastudios.elements.sample.Cheese

class EmptySource : BaseSource<String>() {

    override fun areItemsTheSame(first: String, second: String): Boolean {
        return first == second
    }

    @SuppressLint("MissingSuperCall")
    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        postResult(page, emptyList<Element<String>>())
    }
}