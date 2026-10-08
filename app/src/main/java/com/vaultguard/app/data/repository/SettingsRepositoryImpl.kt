package com.vaultguard.app.data.repository

import android.util.Base64
import com.vaultguard.app.data.local.DatabaseManager
import com.vaultguard.app.data.local.SettingsDataStore
import com.vaultguard.app.domain.model.VaultSettings
import com.vaultguard.app.domain.repository.SettingsRepository
import com.vaultguard.app.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val cryptoManager: CryptoManager,
    private val databaseManager: DatabaseManager
) : SettingsRepository {

    override val settingsFlow: Flow<VaultSettings> = settingsDataStore.settingsFlow

    override suspend fun getSettings(): VaultSettings = settingsDataStore.getSettings()

    override suspend fun setMasterPassword(password: String) = withContext(Dispatchers.Default) {
        val salt = cryptoManager.generateSalt()
        val derivedKey = cryptoManager.deriveKey(password.toCharArray(), salt)

        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(derivedKey, Base64.NO_WRAP)

        settingsDataStore.setMasterPasswordSecurity(hashB64, saltB64)
        databaseManager.openDatabase(derivedKey)
    }

    override suspend fun verifyMasterPassword(password: String): Boolean = withContext(Dispatchers.Default) {
        val hashB64 = settingsDataStore.getMasterPasswordHash() ?: return@withContext false
        val saltB64 = settingsDataStore.getMasterPasswordSalt() ?: return@withContext false

        val storedHash = Base64.decode(hashB64, Base64.NO_WRAP)
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)

        val derivedKey = cryptoManager.deriveKey(password.toCharArray(), salt)
        val matches = MessageDigest.isEqual(storedHash, derivedKey)

        if (matches) {
            databaseManager.openDatabase(derivedKey)
        }

        matches
    }

    override suspend fun isMasterPasswordConfigured(): Boolean {
        return !settingsDataStore.getMasterPasswordHash().isNullOrEmpty()
    }

    override suspend fun setBiometricEnabled(enabled: Boolean, derivedKey: ByteArray?) {
        if (enabled && derivedKey != null) {
            val wrappedKey = cryptoManager.wrapKeyWithKeyStore(derivedKey)
            val wrappedKeyB64 = Base64.encodeToString(wrappedKey, Base64.NO_WRAP)
            settingsDataStore.setWrappedDbKey(wrappedKeyB64)
            settingsDataStore.setBiometricEnabled(true)
        } else {
            settingsDataStore.setBiometricEnabled(false)
            settingsDataStore.setWrappedDbKey(null)
        }
    }

    override suspend fun getWrappedDbKey(): ByteArray? {
        val wrappedB64 = settingsDataStore.getWrappedDbKey() ?: return null
        return try {
            Base64.decode(wrappedB64, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setAutoLockTimeout(seconds: Int) {
        settingsDataStore.setAutoLockTimeoutSeconds(seconds)
    }

    override suspend fun setClipboardClearTimeout(seconds: Int) {
        settingsDataStore.setClipboardClearTimeoutSeconds(seconds)
    }

    override suspend fun setFaviconFetchEnabled(enabled: Boolean) {
        settingsDataStore.setFaviconFetchEnabled(enabled)
    }
}
