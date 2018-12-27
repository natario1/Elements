package com.otaliastudios.elements

import android.util.Log
import timber.log.Timber

object ElementsLogger {

    const val VERBOSE = Log.VERBOSE
    const val WARN = Log.WARN
    const val ERROR = Log.ERROR

    private var level = ERROR

    fun setLevel(level: Int) {
        this.level = level
    }

    fun logs(level: Int): Boolean {
        return this.level <= level
    }

    fun verbose() = logs(VERBOSE)

    fun warns() = logs(WARN)

    fun errors() = logs(ERROR)

    internal fun w(message: String) {
        if (warns()) Timber.w(message)
    }

    internal fun w(throwable: Throwable, message: String) {
        if (warns()) Timber.w(throwable, message)
    }

    internal fun e(message: String) {
        if (errors()) Timber.e(message)
    }

    internal fun e(throwable: Throwable, message: String) {
        if (errors()) Timber.e(throwable, message)
    }

    internal fun v(message: String) {
        if (verbose()) Timber.v(message)
    }

    internal fun v(throwable: Throwable, message: String) {
        if (verbose()) Timber.v(throwable, message)
    }
}