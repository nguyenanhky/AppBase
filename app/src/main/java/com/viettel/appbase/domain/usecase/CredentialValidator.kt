package com.viettel.appbase.domain.usecase

import com.viettel.appbase.domain.model.AppError

internal object CredentialValidator {
    const val MIN_PASSWORD_LENGTH = 6
    private val EMAIL_PATTERN = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun validate(normalizedEmail: String, password: String): AppError? = when {
        !EMAIL_PATTERN.matches(normalizedEmail) -> AppError.InvalidEmail()
        password.length < MIN_PASSWORD_LENGTH -> AppError.PasswordTooShort(MIN_PASSWORD_LENGTH)
        else -> null
    }
}
