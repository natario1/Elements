package com.otaliastudios.elements.sample.sources

import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source

class TopMessageSource(private val message: String) : Source<String>() {

    override fun dependsOn(source: Source<*>) = true

    override fun getElementType(data: String) = 1

    override fun areItemsTheSame(first: String, second: String): Boolean {
        return first == second
    }

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        super.onPageOpened(page, dependencies)
        if (page.isFirstPage()) postResult(page, listOf(message))
    }

    override fun insertBefore(page: Page, dependencies: List<Element<*>>, element: Element<*>, position: Int, available: Int): Int {
        return Math.min(1, available)
    }
}