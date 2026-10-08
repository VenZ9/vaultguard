package com.vaultguard.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vaultguard.app.domain.model.VaultSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vaultguard_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_MASTER_PASSWORD_HASH = stringPreferencesKey("master_password_hash")
        val KEY_MASTER_PASSWORD_SALT = stringPreferencesKey("master_password_salt")
        val KEY_WRAPPED_DB_KEY = stringPreferencesKey("wrapped_db_key")
        val KEY_IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        val KEY_AUTO_LOCK_TIMEOUT = intPreferencesKey("auto_lock_timeout_seconds")
        val KEY_CLIPBOARD_CLEAR_TIMEOUT = intPreferencesKey("clipboard_clear_timeout_seconds")
        val KEY_IS_FAVICON_FETCH_ENABLED = booleanPreferencesKey("is_favicon_fetch_enabled")
    }

    val settingsFlow: Flow<VaultSettings> = context.dataStore.data.map { prefs ->
        val hasMasterPassword = !prefs[KEY_MASTER_PASSWORD_HASH].isNullOrEmpty()
        VaultSettings(
            autoLockTimeoutSeconds = prefs[KEY_AUTO_LOCK_TIMEOUT] ?: 60,
            isBiometricEnabled = prefs[KEY_IS_BIOMETRIC_ENABLED] ?: false,
            clipboardClearTimeoutSeconds = prefs[KEY_CLIPBOARD_CLEAR_TIMEOUT] ?: 30,
            isFaviconFetchEnabled = prefs[KEY_IS_FAVICON_FETCH_ENABLED] ?: false,
            isMasterPasswordSet = hasMasterPassword
        )
    }

    suspend fun getSettings(): VaultSettings = settingsFlow.first()

    suspend fun getMasterPasswordHash(): String? {
        return context.dataStore.data.first()[KEY_MASTER_PASSWORD_HASH]
    }

    suspend fun getMasterPasswordSalt(): String? {
        return context.dataStore.data.first()[KEY_MASTER_PASSWORD_SALT]
    }

    suspend fun getWrappedDbKey(): String? {
        return context.dataStore.data.first()[KEY_WRAPPED_DB_KEY]
    }

    suspend fun setMasterPasswordSecurity(hashB64: String, saltB64: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MASTER_PASSWORD_HASH] = hashB64
            prefs[KEY_MASTER_PASSWORD_SALT] = saltB64
        }
    }

    suspend fun setWrappedDbKey(wrappedKeyB64: String?) {
        context.dataStore.edit { prefs ->
            if (wrappedKeyB64 != null) {
                prefs[KEY_WRAPPED_DB_KEY] = wrappedKeyB64
            } else {
                prefs.remove(KEY_WRAPPED_DB_KEY)
            }
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_BIOMETRIC_ENABLED] = enabled
            if (!enabled) {
                prefs.remove(KEY_WRAPPED_DB_KEY)
            }
        }
    }

    suspend fun setAutoLockTimeoutSeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_LOCK_TIMEOUT] = seconds
        }
    }

    suspend fun setClipboardClearTimeoutSeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CLIPBOARD_CLEAR_TIMEOUT] = seconds
        }
    }

    suspend fun setFaviconFetchEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_FAVICON_FETCH_ENABLED] = enabled
        }
    }
}
