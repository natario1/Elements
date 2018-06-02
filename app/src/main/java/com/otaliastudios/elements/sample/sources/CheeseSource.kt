package com.otaliastudios.elements.sample.sources

import android.os.Handler
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source
import com.otaliastudios.elements.extensions.BaseSource
import com.otaliastudios.elements.sample.Cheese

open class CheeseSource(private val pageSize: Int = 10, loadingEnabled: Boolean = true) : BaseSource<String>(loadingEnabled) {

    protected val handler = Handler()

    override fun dependsOn(source: Source<*>) = false

    override fun areItemsTheSame(first: String, second: String): Boolean {
        return first == second
    }

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        super.onPageOpened(page, dependencies)
        handler.postDelayed({
            val list = Cheese.LIST
            val result = if (page.isFirstPage()) {
                split(list, 0)
            } else {
                val key = getKey<String>(page.previous()!!)
                split(list, list.indexOf(key) + 1)
            }
            setKey(page, result.last())
            postResult(page, result)
        }, 1000)
    }

    private fun split(source: List<String>, from: Int): List<String> {
        val max = source.size - 1
        return source.subList(Math.min(from, max), Math.min(from + pageSize, max))
    }
}