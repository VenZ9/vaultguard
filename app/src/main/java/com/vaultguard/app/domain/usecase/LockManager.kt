package com.vaultguard.app.domain.usecase

import com.vaultguard.app.data.local.DatabaseManager
import com.vaultguard.app.domain.repository.SettingsRepository
import com.vaultguard.app.security.CryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    suspend fun unlockWithMasterPassword(password: String): Boolean = withContext(Dispatchers.Default) {
        val success = settingsRepository.verifyMasterPassword(password)
        if (success) {
            recordUserInteraction()
        }
        success
    }

    suspend fun setupMasterPassword(password: String): Boolean = withContext(Dispatchers.Default) {
        try {
            settingsRepository.setMasterPassword(password)
            recordUserInteraction()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unlockWithBiometrics(): Boolean = withContext(Dispatchers.Default) {
        val wrappedKey = settingsRepository.getWrappedDbKey() ?: return@withContext false
        val unwrappedKey = cryptoManager.unwrapKeyWithKeyStore(wrappedKey) ?: return@withContext false
        databaseManager.openDatabase(unwrappedKey)
        recordUserInteraction()
        true
    }

    suspend fun enableBiometricWithPassword(password: String): Boolean = withContext(Dispatchers.Default) {
        val isCorrect = settingsRepository.verifyMasterPassword(password)
        if (!isCorrect) return@withContext false
        true
    }
}
