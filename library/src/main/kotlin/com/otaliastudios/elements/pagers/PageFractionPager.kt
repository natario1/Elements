package com.otaliastudios.elements.pagers

open class PageFractionPager(
        val expectedPageSize: Int,
        val fraction: Float
): PageSizePager((expectedPageSize * fraction).toInt())