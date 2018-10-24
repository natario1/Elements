package com.otaliastudios.elements

/**
 * The animation type.
 */
enum class AnimationType {
    ADD, REMOVE;

    fun isAdd() = this == ADD

    fun isRemove() = this == REMOVE
}