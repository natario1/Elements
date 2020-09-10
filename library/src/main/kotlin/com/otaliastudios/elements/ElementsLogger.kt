package com.otaliastudios.elements

import android.util.Log
import timber.log.Timber

public object ElementsLogger {

    public const val VERBOSE: Int = Log.VERBOSE
    public const val INFO: Int = Log.INFO
    public const val WARN: Int = Log.WARN
    public const val ERROR: Int = Log.ERROR

    private var level = ERROR

    public fun setLevel(level: Int) {
        this.level = level
    }

    public fun logs(level: Int): Boolean {
        return this.level <= level
    }

    public fun verbose(): Boolean = logs(VERBOSE)

    public fun infos(): Boolean = logs(INFO)

    public fun warns(): Boolean = logs(WARN)

    public fun errors(): Boolean = logs(ERROR)

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

    internal fun i(message: String) {
        if (infos()) Timber.i(message)
    }

    internal fun i(throwable: Throwable, message: String) {
        if (infos()) Timber.i(throwable, message)
    }
}