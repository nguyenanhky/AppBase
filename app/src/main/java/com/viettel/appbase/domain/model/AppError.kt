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

    /** Daily AI quota used up. [resetAt] is an ISO-8601 time from the server, when known. */
    class QuotaExceeded(val resetAt: String? = null, cause: Throwable? = null) : AppError(cause)

    /** The AI provider refused the input or output (safety filters). */
    class ContentBlocked(cause: Throwable? = null) : AppError(cause)

    class InvalidImage(cause: Throwable? = null) : AppError(cause)

    /** App Check rejected the request: modified app, emulator, or not installed from Google Play. */
    class AppNotVerified(cause: Throwable? = null) : AppError(cause)

    /** [code] is null when the response could not be parsed. */
    class Server(val code: Int? = null, cause: Throwable? = null) : AppError(cause)
    class Unknown(cause: Throwable? = null) : AppError(cause)
}

fun Throwable.asAppError(): AppError = this as? AppError ?: AppError.Unknown(this)
