package com.otaliastudios.elements

public abstract class Pager {

    internal lateinit var adapter: Adapter

    public abstract fun onElementBound(
            page: Page,
            element: Element<*>,
            presenter: Presenter<*>,
            absolutePosition: Int,
            pagePosition: Int
    )

    public fun requestPage() {
        adapter.requestPage()
    }
}