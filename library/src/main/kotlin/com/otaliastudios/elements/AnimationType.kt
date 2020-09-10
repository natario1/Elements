package com.otaliastudios.elements

/**
 * The animation type.
 */
public enum class AnimationType {
    ADD, REMOVE;

    public fun isAdd(): Boolean = this == ADD

    public fun isRemove(): Boolean = this == REMOVE
}