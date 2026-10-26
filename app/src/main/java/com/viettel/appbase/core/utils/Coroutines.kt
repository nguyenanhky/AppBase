package com.viettel.appbase.core.utils

import kotlinx.coroutines.CancellationException

/** Like [runCatching], but rethrows [CancellationException] so coroutine cancellation keeps working. */
inline fun <T> suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (exception: CancellationException) {
    throw exception
} catch (exception: Throwable) {
    Result.failure(exception)
}
