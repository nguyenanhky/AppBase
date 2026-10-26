package com.viettel.appbase.ui.error

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.viettel.appbase.R
import com.viettel.appbase.domain.model.AppError

@Composable
fun AppError.toUserMessage(): String = when (this) {
    is AppError.InvalidEmail -> stringResource(R.string.error_invalid_email)
    is AppError.PasswordTooShort -> pluralStringResource(R.plurals.error_password_too_short, minLength, minLength)
    is AppError.DisplayNameRequired -> stringResource(R.string.error_display_name_required)
    is AppError.InvalidCredentials -> stringResource(R.string.error_invalid_credentials)
    is AppError.EmailAlreadyInUse -> stringResource(R.string.error_email_already_in_use)
    is AppError.WeakPassword -> stringResource(R.string.error_weak_password)
    is AppError.Network -> stringResource(R.string.error_network)
    is AppError.Unauthorized -> stringResource(R.string.error_session_expired)
    is AppError.Server -> stringResource(R.string.error_server)
    is AppError.QuotaExceeded -> stringResource(R.string.error_quota_exceeded)
    is AppError.ContentBlocked -> stringResource(R.string.error_content_blocked)
    is AppError.InvalidImage -> stringResource(R.string.error_invalid_image)
    is AppError.Unknown -> stringResource(R.string.error_unknown)
}
