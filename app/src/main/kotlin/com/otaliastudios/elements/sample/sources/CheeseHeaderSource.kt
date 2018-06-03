package com.otaliastudios.elements.sample.sources

import android.util.Log
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source
import com.otaliastudios.elements.extensions.HeaderSource

class CheeseHeaderSource(private val elementType: Int): HeaderSource<String, String>() {

    // Store the last header that was added, even if it belongs to a previous page.
    private var lastHeader: String = ""

    override fun dependsOn(source: Source<*>): Boolean {
        return source is CheeseSource
    }

    override fun areItemsTheSame(first: Data<String, String>, second: Data<String, String>): Boolean {
        return first == second
    }

    override fun getElementType(data: Data<String, String>) = elementType

    override fun computeHeaders(page: Page, list: List<String>): List<Data<String, String>> {
        val results = arrayListOf<Data<String, String>>()
        for (cheese in list) {
            val letter = cheese.substring(0, 1).toUpperCase()
            if (letter != lastHeader) {
                results.add(Data(cheese, letter))
                lastHeader = letter
            }
        }
        return results
    }
}