package com.viettel.appbase.feature.auth.login

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
import com.viettel.appbase.BuildConfig
import com.viettel.appbase.R
import com.viettel.appbase.ui.components.AppButton
import com.viettel.appbase.ui.components.AppButtonStyle
import com.viettel.appbase.ui.components.AppTextField
import com.viettel.appbase.ui.components.LoadingDialog
import com.viettel.appbase.ui.theme.LocalAppSpacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    onRegister: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isAuthenticated) { if (state.isAuthenticated) onAuthenticated() }

    Column(
        modifier = Modifier.fillMaxSize().padding(LocalAppSpacing.current.large),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.login_subtitle), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(LocalAppSpacing.current.large))
        AppTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChanged,
            label = stringResource(R.string.email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(LocalAppSpacing.current.small))
        AppTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChanged,
            label = stringResource(R.string.password),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
        )
        state.errorMessage?.let {
            Spacer(Modifier.height(LocalAppSpacing.current.small))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (BuildConfig.USE_FAKE_AUTH) {
            Spacer(Modifier.height(LocalAppSpacing.current.small))
            Text(stringResource(R.string.demo_auth_hint), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(LocalAppSpacing.current.medium))
        AppButton(stringResource(R.string.login_action), onClick = viewModel::submit)
        AppButton(
            stringResource(R.string.register_action),
            onClick = onRegister,
            style = AppButtonStyle.TEXT,
        )
    }
    LoadingDialog(state.isLoading)
}
