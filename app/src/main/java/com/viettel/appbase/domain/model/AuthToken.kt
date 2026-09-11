package com.viettel.appbase.domain.model

data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
)
