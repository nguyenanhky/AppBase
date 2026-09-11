package com.viettel.appbase.feature.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viettel.appbase.R
import com.viettel.appbase.ui.components.AppButton
import com.viettel.appbase.ui.components.AppButtonStyle
import com.viettel.appbase.ui.components.AppTextField
import com.viettel.appbase.ui.components.LoadingDialog
import com.viettel.appbase.ui.theme.LocalAppSpacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun RegisterScreen(
    onAuthenticated: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isAuthenticated) { if (state.isAuthenticated) onAuthenticated() }
    Column(
        modifier = Modifier.fillMaxSize().padding(LocalAppSpacing.current.large),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.register_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(LocalAppSpacing.current.large))
        AppTextField(state.displayName, viewModel::onDisplayNameChanged, stringResource(R.string.display_name))
        Spacer(Modifier.height(LocalAppSpacing.current.small))
        AppTextField(
            state.email,
            viewModel::onEmailChanged,
            stringResource(R.string.email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(LocalAppSpacing.current.small))
        AppTextField(
            state.password,
            viewModel::onPasswordChanged,
            stringResource(R.string.password),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
        )
        state.errorMessage?.let {
            Spacer(Modifier.height(LocalAppSpacing.current.small))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(LocalAppSpacing.current.medium))
        AppButton(stringResource(R.string.create_account), onClick = viewModel::submit)
        AppButton(stringResource(R.string.back_to_login), onClick = onBack, style = AppButtonStyle.TEXT)
    }
    LoadingDialog(state.isLoading)
}
