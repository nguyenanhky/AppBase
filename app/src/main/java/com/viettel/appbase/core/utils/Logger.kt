package com.viettel.appbase.core.utils

import timber.log.Timber

object Logger {
    fun debug(message: String) = Timber.d(message)
    fun info(message: String) = Timber.i(message)
    fun error(throwable: Throwable, message: String? = null) = Timber.e(throwable, message)
}
