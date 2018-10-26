package com.otaliastudios.elements

/**
 * Results returned by [Adapter.elementAt].
 */
class ElementAtResult {

    lateinit var page: Page
        internal set

    lateinit var element: Element<*>
        internal set

    var positionInPage: Int = 0
        internal set

    var position: Int = 0
        internal set
}