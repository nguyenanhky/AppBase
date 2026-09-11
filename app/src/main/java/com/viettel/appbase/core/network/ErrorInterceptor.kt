package com.viettel.appbase.core.network

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = try {
        chain.proceed(chain.request())
    } catch (exception: IOException) {
        throw NetworkConnectionException(exception)
    }
}

class NetworkConnectionException(cause: IOException) : IOException("Unable to reach server.", cause)
