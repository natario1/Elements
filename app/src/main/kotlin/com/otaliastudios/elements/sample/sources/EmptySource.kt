package com.otaliastudios.elements.sample.sources

import android.annotation.SuppressLint
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.extensions.MainSource

class EmptySource : MainSource<String>() {

    override fun areItemsTheSame(first: String, second: String): Boolean {
        return first == second
    }

    @SuppressLint("MissingSuperCall")
    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        postResult(page, emptyList<Element<String>>())
    }
}