package com.viettel.appbase.domain.usecase

import com.viettel.appbase.domain.model.AppError
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
        if (displayName.isBlank()) return Result.failure(AppError.DisplayNameRequired())
        val normalizedEmail = CredentialValidator.normalizeEmail(email)
        CredentialValidator.validate(normalizedEmail, password)?.let { return Result.failure(it) }
        return repository.register(normalizedEmail, password, displayName.trim())
    }
}
