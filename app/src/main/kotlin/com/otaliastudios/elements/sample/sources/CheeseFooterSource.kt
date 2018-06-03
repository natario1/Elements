package com.otaliastudios.elements.sample.sources

import android.util.Log
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Source
import com.otaliastudios.elements.extensions.FooterSource
import com.otaliastudios.elements.extensions.HeaderSource

class CheeseFooterSource(private val elementType: Int): FooterSource<String, String>() {

    // Store the last footer that was added, even if it belongs to a previous page.
    private var lastAnchor: String = ""
    private var lastLetter: String = ""

    override fun dependsOn(source: Source<*>): Boolean {
        return source is CheeseSource
    }

    override fun areItemsTheSame(first: Data<String, String>, second: Data<String, String>): Boolean {
        return first == second
    }

    override fun getElementType(data: Data<String, String>) = elementType

    override fun computeFooters(page: Page, list: List<String>): List<Data<String, String>> {
        val results = arrayListOf<Data<String, String>>()
        for (cheese in list) {
            val letter = cheese.substring(0, 1).toUpperCase()
            if (lastLetter.isNotEmpty() && letter != lastLetter) {
                Log.e("Footer", "letter: $letter, lastAnchor: $lastAnchor, lastLetter: $lastLetter")
                results.add(Data(lastAnchor, lastLetter))
            }
            lastAnchor = cheese
            lastLetter = letter
        }
        return results
    }
}