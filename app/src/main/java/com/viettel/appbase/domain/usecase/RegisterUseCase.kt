package com.viettel.appbase.domain.usecase

import com.viettel.appbase.domain.model.AuthSession
import com.viettel.appbase.domain.repository.AuthRepository

class RegisterUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String,
    ): Result<AuthSession> {
        if (displayName.isBlank()) {
            return Result.failure(IllegalArgumentException("Display name is required."))
        }
        val normalizedEmail = email.trim().lowercase()
        if (!EMAIL_PATTERN.matches(normalizedEmail)) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(IllegalArgumentException("Password must contain at least 6 characters."))
        }
        return repository.register(normalizedEmail, password, displayName.trim())
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private val EMAIL_PATTERN = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
    }
}
