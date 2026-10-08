package com.vaultguard.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SecretType {
    LOGIN,
    API_KEY,
    APP_PASSWORD,
    PASSKEY
}

@Serializable
data class FieldMapping(
    val viewId: String = "",
    val hint: String = "",
    val inputType: Int = 0,
    val targetField: String = "apiKey" // "apiKey", "username", "password"
)

@Serializable
data class VaultItem(
    val id: String,
    val type: SecretType,
    val name: String,
    val username: String = "",
    val password: String = "",
    val urlOrPackage: String = "",
    val apiKey: String = "",
    val notes: String = "",
    val folderOrTag: String = "",
    val isFavorite: Boolean = false,
    val customFieldMappings: List<FieldMapping> = emptyList(),
    val customIconUri: String? = null,
    val passkeyCredentialId: String = "",
    val passkeyRelyingParty: String = "",
    val passkeyUserHandle: String = "",
    val passkeyPublicKey: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = 0L
)

enum class SortOrder {
    NAME,
    DATE_ADDED,
    LAST_USED
}

data class VaultSettings(
    val autoLockTimeoutSeconds: Int = 60,
    val isBiometricEnabled: Boolean = false,
    val clipboardClearTimeoutSeconds: Int = 30,
    val isFaviconFetchEnabled: Boolean = false,
    val isMasterPasswordSet: Boolean = false
)

data class PasswordGeneratorConfig(
    val length: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true
)
