package com.otaliastudios.elements

/**
 * Results returned by [Adapter.elementAt].
 */
public class ElementAtResult {

    public lateinit var page: Page
        internal set

    public lateinit var element: Element<*>
        internal set

    public var positionInPage: Int = 0
        internal set

    public var position: Int = 0
        internal set
}