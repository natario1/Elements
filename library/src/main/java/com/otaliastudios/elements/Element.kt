package com.otaliastudios.elements

/**
 * The base block of the library. Elements are created by sources,
 * either internally or through [Source.createElement], [Source.createElements],
 * and [Source.createEmptyElement].
 *
 * However, the [clone] function can help creating a new element from outside
 * sources, by just having a reference to another element.
 *
 * @param T the model class
 * @property source the source that created this element
 * @property type the element type
 * @property data the actual data object. Can be null
 * @property extra any extra information that might be passed to presenters
 */
public class Element<T: Any> internal constructor(
        public val source: Source<T>,
        public val type: Int,
        public val data: T?,
        public val extra: Any? = null) {

    /**
     * Creates a new element with same characteristics,
     * but replaces its data.
     */
    fun clone(newData: T?): Element<T> {
        return Element(source, type, newData, extra)
    }
}