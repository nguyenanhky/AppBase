package com.viettel.appbase.data.repository

import com.viettel.appbase.core.network.apiCall
import com.viettel.appbase.data.remote.api.AuthApi
import com.viettel.appbase.data.remote.dto.AuthResponseDto
import com.viettel.appbase.data.remote.dto.LoginRequestDto
import com.viettel.appbase.data.remote.dto.RefreshTokenRequestDto
import com.viettel.appbase.data.remote.dto.RegisterRequestDto
import com.viettel.appbase.domain.model.AppError

/** [AuthBackend] for your own REST API. Adjust [AuthApi] and the DTOs to match the server contract. */
class RetrofitAuthBackend(private val api: AuthApi) : AuthBackend {
    override suspend fun login(email: String, password: String): BackendAuthSession =
        credentialsCall { api.login(LoginRequestDto(email, password)) }.toSession()

    override suspend fun register(email: String, password: String, displayName: String): BackendAuthSession =
        credentialsCall { api.register(RegisterRequestDto(email, password, displayName)) }.toSession()

    override suspend fun refresh(refreshToken: String): BackendAuthSession =
        apiCall { api.refresh(RefreshTokenRequestDto(refreshToken)) }.toSession()

    // Stateless tokens: nothing to revoke server-side. Call a logout endpoint here if the API has one.
    override fun logout() = Unit

    // On login/register a 401 means wrong credentials, not an expired session.
    private inline fun <T> credentialsCall(block: () -> T): T = try {
        apiCall(block)
    } catch (exception: AppError.Unauthorized) {
        throw AppError.InvalidCredentials(exception.cause)
    }

    private fun AuthResponseDto.toSession() = BackendAuthSession(
        userId = user.id,
        email = user.email,
        displayName = user.displayName.orEmpty(),
        accessToken = accessToken,
        refreshToken = refreshToken,
    )
}
