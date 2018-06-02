package com.otaliastudios.elements.sample.sources

import android.graphics.Color
import android.os.Handler
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source
import com.otaliastudios.elements.extensions.BaseSource
import com.otaliastudios.elements.extensions.ListSource
import com.otaliastudios.elements.sample.Cheese
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.nextDown

class ShuffleColorsShource(private val delayMillis: Long = 3000) : Source<Int>() {

    private val handler = Handler()
    private var colors = Array(8, { randomColor() }).toList()

    private fun randomColor() = Color.argb(255,
            180 + (Math.random() * 70).toInt(),
            130 + (Math.random() * 120).toInt(),
            90 + (Math.random() * 150).toInt())

    override fun dependsOn(source: Source<*>) = false

    override fun areItemsTheSame(first: Int, second: Int): Boolean {
        return first == second
    }

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        super.onPageOpened(page, dependencies)
        if (page.isFirstPage()) {
            postResult(page, colors)
            handler.postDelayed( {
                colors = colors.shuffled()
                onPageOpened(page, dependencies)
            }, delayMillis)
        }
    }
}