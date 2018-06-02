package com.otaliastudios.elements

public class Element<T: Any> internal constructor(
        public val source: Source<T>,
        public val type: Int,
        public val data: T?,
        public val extra: Any? = null)