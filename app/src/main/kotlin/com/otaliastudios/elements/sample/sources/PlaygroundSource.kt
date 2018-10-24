package com.otaliastudios.elements.sample.sources

import android.os.Handler
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source
import com.otaliastudios.elements.extensions.MainSource
import com.otaliastudios.elements.sample.Cheese

/**
 * Bug being investigated: when we have a CHANGED event, the old item is still drawn,
 * together with the new representation.
 *
 * This only happens if the presenter returns 'false' to the animators.
 */
open class PlaygroundSource : Source<PlaygroundSource.Data>() {

    protected val handler = Handler()

    override fun dependsOn(source: Source<*>) = false

    override fun areItemsTheSame(first: Data, second: Data): Boolean {
        return first.id == second.id
    }

    override fun areContentsTheSame(first: Data, second: Data): Boolean {
        return first == second
    }

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        super.onPageOpened(page, dependencies)

        if (page.isFirstPage()) {
            val list = mutableListOf<Data>()
            var index = 0
            list.add(Data(index++, " First item "))
            list.add(Data(index++, " Second item "))
            list.add(Data(index++, " Third item "))
            list.add(Data(index++, " Fourth item "))
            list.add(Data(index++, " Fifth item "))
            var count = 0
            val maxCount = 8
            val runnable = object: Runnable {
                override fun run() {
                    val last = list.last()
                    val replace = Data(last.id, "X" + last.name)
                    list.set(list.lastIndex, replace)
                    postResult(page, list)
                    if (count < maxCount) {
                        handler.postDelayed(this, 2000)
                    }
                    count++
                }
            }
            runnable.run()
        }
    }

    private fun split(source: List<String>, from: Int): List<String> {
        val max = source.size - 1
        return source.subList(Math.min(from, max), Math.min(from + 1, max))
    }

    class Data(val id: Int, var name: String) {

        override fun equals(other: Any?): Boolean {
            return other is Data && other.id == id && other.name == name
        }

        override fun toString(): String {
            return name
        }

    }
}