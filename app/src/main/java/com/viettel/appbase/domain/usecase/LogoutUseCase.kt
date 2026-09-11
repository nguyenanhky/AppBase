package com.viettel.appbase.domain.usecase

import com.viettel.appbase.domain.repository.AuthRepository

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.logout()
}
