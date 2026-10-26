package com.viettel.appbase.core.network

import com.viettel.appbase.domain.model.AppError
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/** Wrap every Retrofit call: transport and HTTP failures are rethrown as [AppError]. */
inline fun <T> apiCall(block: () -> T): T = try {
    block()
} catch (exception: CancellationException) {
    throw exception
} catch (exception: HttpException) {
    throw if (exception.code() == HTTP_UNAUTHORIZED) {
        AppError.Unauthorized(exception)
    } else {
        AppError.Server(exception.code(), exception)
    }
} catch (exception: IOException) {
    throw AppError.Network(exception)
} catch (exception: SerializationException) {
    throw AppError.Server(cause = exception)
}

@PublishedApi
internal const val HTTP_UNAUTHORIZED = 401
