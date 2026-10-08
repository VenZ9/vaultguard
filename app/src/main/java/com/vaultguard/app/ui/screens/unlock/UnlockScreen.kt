package com.vaultguard.app.ui.screens.unlock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.vaultguard.app.security.BiometricHelper
import com.vaultguard.app.ui.components.SecureTextField
import kotlinx.coroutines.flow.collectLatest

@Composable
fun UnlockScreen(
    viewModel: UnlockViewModel,
    biometricHelper: BiometricHelper,
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is UnlockEvent.UnlockSuccess -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUnlockSuccess()
                }
                is UnlockEvent.ShowToast -> {}
            }
        }
    }

    // Auto-prompt biometric if enabled and not first-time
    LaunchedEffect(uiState.isBiometricAvailable, uiState.isFirstTimeSetup) {
        if (uiState.isBiometricAvailable && !uiState.isFirstTimeSetup) {
            val activity = context as? FragmentActivity
            if (activity != null && biometricHelper.isBiometricAvailable(context)) {
                biometricHelper.showBiometricPrompt(
                    activity = activity,
                    onSuccess = { viewModel.unlockWithBiometric() },
                    onError = { /* fallback to password */ }
                )
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Shield Logo Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "VaultGuard",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "VaultGuard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = if (uiState.isFirstTimeSetup) {
                        "Create a master password to secure your local encrypted vault"
                    } else {
                        "Your vault is locked. Enter your master password to unlock."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                SecureTextField(
                    value = uiState.passwordInput,
                    onValueChange = viewModel::onPasswordChanged,
                    label = if (uiState.isFirstTimeSetup) "Master Password" else "Enter Master Password",
                    placeholder = "••••••••••••",
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (uiState.isFirstTimeSetup) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!uiState.isFirstTimeSetup) viewModel.submit()
                        }
                    )
                )

                if (uiState.isFirstTimeSetup) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SecureTextField(
                        value = uiState.confirmPasswordInput,
                        onValueChange = viewModel::onConfirmPasswordChanged,
                        label = "Confirm Master Password",
                        placeholder = "••••••••••••",
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.submit() }
                        )
                    )
                }

                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.submit()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (uiState.isFirstTimeSetup) "Set Master Password" else "Unlock",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    if (!uiState.isFirstTimeSetup && uiState.isBiometricAvailable) {
                        FilledTonalIconButton(
                            onClick = {
                                val activity = context as? FragmentActivity
                                if (activity != null) {
                                    biometricHelper.showBiometricPrompt(
                                        activity = activity,
                                        onSuccess = { viewModel.unlockWithBiometric() },
                                        onError = { /* ignore */ }
                                    )
                                }
                            },
                            modifier = Modifier.size(52.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = "Biometric Unlock",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
