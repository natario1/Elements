package com.otaliastudios.elements

abstract class Pager {

    internal lateinit var adapter: Adapter

    abstract fun onElementBound(page: Page, element: Element<*>, presenter: Presenter<*>, absolutePosition: Int, pagePosition: Int)

    fun requestPage() {
        adapter.requestPage()
    }
}