package com.vaultguard.app.domain.usecase

import com.vaultguard.app.data.local.DatabaseManager
import com.vaultguard.app.domain.repository.SettingsRepository
import com.vaultguard.app.security.CryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockManager @Inject constructor(
    private val databaseManager: DatabaseManager,
    private val settingsRepository: SettingsRepository,
    private val cryptoManager: CryptoManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    val isUnlocked: StateFlow<Boolean> = databaseManager.isUnlocked

    private var lastInteractionTimestamp: Long = System.currentTimeMillis()
    private var autoLockTimeoutSeconds: Int = 60

    init {
        scope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                autoLockTimeoutSeconds = settings.autoLockTimeoutSeconds
            }
        }
    }

    fun recordUserInteraction() {
        lastInteractionTimestamp = System.currentTimeMillis()
    }

    fun checkAutoLock() {
        if (!isUnlocked.value) return
        val elapsed = (System.currentTimeMillis() - lastInteractionTimestamp) / 1000
        if (elapsed >= autoLockTimeoutSeconds) {
            lockVault()
        }
    }

    fun lockVault() {
        databaseManager.closeDatabase()
    }

    suspend fun unlockWithMasterPassword(password: String): Boolean {
        val success = settingsRepository.verifyMasterPassword(password)
        if (success) {
            recordUserInteraction()
        }
        return success
    }

    suspend fun setupMasterPassword(password: String): Boolean {
        return try {
            settingsRepository.setMasterPassword(password)
            recordUserInteraction()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unlockWithBiometrics(): Boolean {
        val wrappedKey = settingsRepository.getWrappedDbKey() ?: return false
        val unwrappedKey = cryptoManager.unwrapKeyWithKeyStore(wrappedKey) ?: return false
        databaseManager.openDatabase(unwrappedKey)
        recordUserInteraction()
        return true
    }

    suspend fun enableBiometricWithPassword(password: String): Boolean {
        val saltB64 = (settingsRepository as? com.vaultguard.app.data.repository.SettingsRepositoryImpl)?.let {
            // Salt is needed to derive the key
        }
        val isCorrect = settingsRepository.verifyMasterPassword(password)
        if (!isCorrect) return false

        // In openDatabase, activePassphrase was set. We can re-derive or wrap key.
        return true
    }
}
