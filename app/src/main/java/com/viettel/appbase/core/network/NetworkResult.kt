package com.viettel.appbase.core.network

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(val error: NetworkError) : NetworkResult<Nothing>
}

data class NetworkError(
    val message: String,
    val code: Int? = null,
    val cause: Throwable? = null,
)

suspend inline fun <T> safeApiCall(block: () -> T): NetworkResult<T> = try {
    NetworkResult.Success(block())
} catch (exception: CancellationException) {
    throw exception
} catch (exception: HttpException) {
    NetworkResult.Error(NetworkError(exception.message(), exception.code(), exception))
} catch (exception: IOException) {
    NetworkResult.Error(NetworkError("Network connection failed.", cause = exception))
} catch (exception: SerializationException) {
    NetworkResult.Error(NetworkError("Invalid server response.", cause = exception))
}
