package com.viettel.appbase.domain.usecase

import com.viettel.appbase.domain.repository.AuthRepository

class RefreshTokenUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<String> = repository.refreshToken()
}
