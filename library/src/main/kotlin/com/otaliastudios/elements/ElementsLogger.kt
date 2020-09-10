package com.otaliastudios.elements

import android.util.Log


/**
 * Lazy logger used across the library.
 * Use [setLevel] to configure the verbosity.
 */
public class ElementsLogger internal constructor(private val tag: String) {

    public companion object {
        public const val VERBOSE: Int = Log.VERBOSE
        public const val INFO: Int = Log.INFO
        public const val WARN: Int = Log.WARN
        public const val ERROR: Int = Log.ERROR

        private var level = WARN

        @JvmStatic
        public fun setLevel(level: Int) {
            this.level = level
        }
    }

    internal fun w(message: () -> String) {
        if (level <= WARN) Log.w(tag, message())
    }

    internal fun w(throwable: Throwable, message: () -> String) {
        if (level <= WARN) Log.w(tag, message(), throwable)
    }

    internal fun e(message: () -> String) {
        if (level <= ERROR) Log.e(tag, message())
    }

    internal fun e(throwable: Throwable, message: () -> String) {
        if (level <= ERROR) Log.e(tag, message(), throwable)
    }

    internal fun i(message: () -> String) {
        if (level <= INFO) Log.i(tag, message())
    }

    internal fun i(throwable: Throwable, message: () -> String) {
        if (level <= INFO) Log.i(tag, message(), throwable)
    }

    internal fun v(message: () -> String) {
        if (level <= VERBOSE) Log.v(tag, message())
    }

    internal fun v(throwable: Throwable, message: () -> String) {
        if (level <= VERBOSE) Log.v(tag, message(), throwable)
    }
}