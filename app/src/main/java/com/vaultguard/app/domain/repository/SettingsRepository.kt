package com.vaultguard.app.domain.repository

import com.vaultguard.app.domain.model.VaultSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<VaultSettings>
    suspend fun getSettings(): VaultSettings
    suspend fun setMasterPassword(password: String)
    suspend fun verifyMasterPassword(password: String): Boolean
    suspend fun isMasterPasswordConfigured(): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean, derivedKey: ByteArray?)
    suspend fun getWrappedDbKey(): ByteArray?
    suspend fun setAutoLockTimeout(seconds: Int)
    suspend fun setClipboardClearTimeout(seconds: Int)
    suspend fun setFaviconFetchEnabled(enabled: Boolean)
}
