package com.viettel.appbase.domain.usecase

import com.viettel.appbase.domain.model.AuthSession
import com.viettel.appbase.domain.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<AuthSession> {
        val normalizedEmail = CredentialValidator.normalizeEmail(email)
        CredentialValidator.validate(normalizedEmail, password)?.let { return Result.failure(it) }
        return repository.login(normalizedEmail, password)
    }
}
