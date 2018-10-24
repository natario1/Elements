package com.otaliastudios.elements.sample.sources

import android.os.Handler
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source
import com.otaliastudios.elements.extensions.MainSource
import com.otaliastudios.elements.sample.Cheese

open class AnimatedCheeseSource : Source<String>() {

    protected val handler = Handler()

    override fun dependsOn(source: Source<*>) = false

    override fun areItemsTheSame(first: String, second: String): Boolean {
        return first == second
    }

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        super.onPageOpened(page, dependencies)
        /* val list = Cheese.LIST
        val result = if (page.isFirstPage()) {
            split(list, 0)
        } else {
            val key = getKey<String>(page.previous()!!)
            split(list, list.indexOf(key) + 1)
        }
        setKey(page, result.last())
        postResult(page, result)

        handler.postDelayed({
            requestPage(page)
        }, 1000) */

        if (page.isFirstPage()) {
            val all = Cheese.LIST
            val list = mutableListOf<String>()
            val max = 10
            var adding = true
            val runnable = object: Runnable {
                override fun run() {
                    if (list.isEmpty() && !adding) {
                        adding = true
                    } else if (list.size == max && adding) {
                        adding = false
                    }
                    if (adding) {
                        val newItem = split(all, list.size)[0]
                        list.add(newItem)
                        postResult(page, list)
                    } else if (!adding) {
                        list.remove(list.last())
                        postResult(page, list)
                    }
                    handler.postDelayed(this, 400)
                }
            }
            runnable.run()
        }
    }

    private fun split(source: List<String>, from: Int): List<String> {
        val max = source.size - 1
        return source.subList(Math.min(from, max), Math.min(from + 1, max))
    }
}