package com.vaultguard.app.ui.screens.addedit

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vaultguard.app.domain.model.SecretType
import com.vaultguard.app.ui.components.SecureTextField
import com.vaultguard.app.ui.components.ServiceIconView
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    viewModel: AddEditViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    var showAddMappingDialog by remember { mutableStateOf(false) }
    var mappingViewId by remember { mutableStateOf("") }
    var mappingHint by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AddEditEvent.SaveSuccess -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateBack()
                }
                is AddEditEvent.ShowToast -> {}
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit Item" else "New Item",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onFavoriteToggle) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.save()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Secret Type Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecretType.entries.forEach { secretType ->
                    val isSelected = uiState.type == secretType
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onTypeChanged(secretType) },
                        label = {
                            Text(
                                text = when (secretType) {
                                    SecretType.LOGIN -> "Login"
                                    SecretType.API_KEY -> "API Key"
                                    SecretType.APP_PASSWORD -> "App Password"
                                    SecretType.PASSKEY -> "Passkey"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name with Live Icon Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ServiceIconView(
                    name = uiState.name,
                    urlOrPackage = uiState.urlOrPackage,
                    customIconUri = uiState.customIconUri,
                    size = 56.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChanged,
                    label = { Text("Name / Service") },
                    placeholder = {
                        Text(
                            when (uiState.type) {
                                SecretType.LOGIN -> "e.g. Gemini, GitHub"
                                SecretType.API_KEY -> "e.g. OpenAI API, Stripe"
                                SecretType.APP_PASSWORD -> "e.g. Banking App, WhatsApp"
                                SecretType.PASSKEY -> "e.g. Google, GitHub Passkey"
                            }
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(visible = uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, start = 72.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Fields by SecretType
            when (uiState.type) {
                SecretType.LOGIN -> {
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = viewModel::onUsernameChanged,
                        label = { Text("Username or Email") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SecureTextField(
                            value = uiState.password,
                            onValueChange = viewModel::onPasswordChanged,
                            label = "Password",
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.generatePassword()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = "Generate Password",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.urlOrPackage,
                        onValueChange = viewModel::onUrlOrPackageChanged,
                        label = { Text("Website Domain or Package") },
                        placeholder = { Text("e.g. github.com or com.github.android") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Language, contentDescription = null)
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SecretType.API_KEY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SecureTextField(
                            value = uiState.apiKey,
                            onValueChange = viewModel::onApiKeyChanged,
                            label = "API Key / Token Secret",
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Key, contentDescription = null)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.generatePassword()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = "Generate Random Key",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.urlOrPackage,
                        onValueChange = viewModel::onUrlOrPackageChanged,
                        label = { Text("Target URL or Package Name") },
                        placeholder = { Text("e.g. api.openai.com") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Language, contentDescription = null)
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // API Key Field Mappings Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Autofill Field Mappings",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                TextButton(onClick = { showAddMappingDialog = true }) {
                                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Mapping")
                                }
                            }

                            Text(
                                text = "Teach Autofill to inject this API key into specific app or website input fields.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (uiState.customFieldMappings.isEmpty()) {
                                Text(
                                    text = "No mappings configured. Tap 'Add Mapping' to match by field ID or hint.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                uiState.customFieldMappings.forEachIndexed { index, mapping ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface,
                                                MaterialTheme.shapes.small
                                            )
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = if (mapping.viewId.isNotBlank()) "ID: ${mapping.viewId}" else "Hint: ${mapping.hint}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (mapping.hint.isNotBlank() && mapping.viewId.isNotBlank()) {
                                                Text(
                                                    text = "Hint: ${mapping.hint}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        IconButton(onClick = { viewModel.removeFieldMapping(index) }) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Remove mapping",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SecretType.APP_PASSWORD -> {
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = viewModel::onUsernameChanged,
                        label = { Text("Account / User / PIN") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SecureTextField(
                            value = uiState.password,
                            onValueChange = viewModel::onPasswordChanged,
                            label = "App Password / Passcode",
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.generatePassword()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = "Generate",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.urlOrPackage,
                        onValueChange = viewModel::onUrlOrPackageChanged,
                        label = { Text("Android Package Name") },
                        placeholder = { Text("e.g. com.whatsapp") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.PhoneAndroid, contentDescription = null)
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SecretType.PASSKEY -> {
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = viewModel::onUsernameChanged,
                        label = { Text("User / Account Handle") },
                        placeholder = { Text("e.g. alex@example.com") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.urlOrPackage,
                        onValueChange = viewModel::onUrlOrPackageChanged,
                        label = { Text("Relying Party / Domain") },
                        placeholder = { Text("e.g. github.com, google.com") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Language, contentDescription = null)
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FIDO2 / WebAuthn Passkey",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Passkeys use phishing-resistant public-key cryptography (ES256 / ECDSA P-256) stored securely in your vault.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (uiState.passkeyCredentialId.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Credential ID: ${uiState.passkeyCredentialId.take(16)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Algorithm: ES256 (P-256)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.generatePasskey()
                                },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Filled.Key, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (uiState.passkeyCredentialId.isNotBlank()) "Regenerate Keypair" else "Generate Cryptographic Passkey")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Folder or Tag Field
            OutlinedTextField(
                value = uiState.folderOrTag,
                onValueChange = viewModel::onFolderOrTagChanged,
                label = { Text("Folder / Tag (Optional)") },
                placeholder = { Text("e.g. Work, Personal, Cloud") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Tag, contentDescription = null)
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Notes Field
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Additional secret notes or recovery codes") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Notes, contentDescription = null)
                },
                shape = MaterialTheme.shapes.medium,
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.save()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    text = if (uiState.isEditMode) "Save Changes" else "Create Item",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add API Key Field Mapping Dialog
    if (showAddMappingDialog) {
        AlertDialog(
            onDismissRequest = { showAddMappingDialog = false },
            title = { Text("Add Field Mapping") },
            text = {
                Column {
                    Text(
                        text = "Specify view ID or input hint that identifies this field in the target app or browser form.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = mappingViewId,
                        onValueChange = { mappingViewId = it },
                        label = { Text("View ID / Resource Entry") },
                        placeholder = { Text("e.g. api_key_input, edit_token") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = mappingHint,
                        onValueChange = { mappingHint = it },
                        label = { Text("Field Hint / Placeholder") },
                        placeholder = { Text("e.g. API Key, Enter Secret") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (mappingViewId.isNotBlank() || mappingHint.isNotBlank()) {
                            viewModel.addFieldMapping(mappingViewId, mappingHint)
                            mappingViewId = ""
                            mappingHint = ""
                            showAddMappingDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMappingDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = MaterialTheme.shapes.medium
        )
    }
}
