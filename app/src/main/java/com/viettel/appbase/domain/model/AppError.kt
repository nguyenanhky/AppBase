package com.viettel.appbase.domain.model

/**
 * Typed failures shared across layers. The UI maps each case to a localized message,
 * so never show [Throwable.message] to users directly.
 */
sealed class AppError(cause: Throwable? = null) : Exception(cause) {
    class InvalidEmail : AppError()
    class PasswordTooShort(val minLength: Int) : AppError()
    class DisplayNameRequired : AppError()
    class InvalidCredentials(cause: Throwable? = null) : AppError(cause)
    class EmailAlreadyInUse(cause: Throwable? = null) : AppError(cause)
    class WeakPassword(cause: Throwable? = null) : AppError(cause)
    class Network(cause: Throwable? = null) : AppError(cause)
    class Unauthorized(cause: Throwable? = null) : AppError(cause)

    /** [code] is null when the response could not be parsed. */
    class Server(val code: Int? = null, cause: Throwable? = null) : AppError(cause)
    class Unknown(cause: Throwable? = null) : AppError(cause)
}

fun Throwable.asAppError(): AppError = this as? AppError ?: AppError.Unknown(this)
