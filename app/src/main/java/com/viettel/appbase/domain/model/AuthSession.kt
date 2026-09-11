package com.viettel.appbase.domain.model

data class AuthSession(
    val user: User,
    val token: AuthToken,
)
