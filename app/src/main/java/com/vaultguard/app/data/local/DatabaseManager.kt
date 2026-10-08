package com.vaultguard.app.data.local

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.util.Arrays
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var database: AppDatabase? = null
    private var activePassphrase: ByteArray? = null

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    @Synchronized
    fun openDatabase(passphrase: ByteArray): AppDatabase {
        database?.let {
            if (activePassphrase != null && Arrays.equals(activePassphrase, passphrase)) {
                return it
            }
            closeDatabase()
        }

        val passphraseCopy = passphrase.clone()
        val factory = SupportOpenHelperFactory(passphraseCopy)

        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vaultguard_encrypted.db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()

        database = db
        activePassphrase = passphraseCopy
        _isUnlocked.value = true
        return db
    }

    @Synchronized
    fun getDatabase(): AppDatabase? {
        return database
    }

    @Synchronized
    fun getDao(): VaultDao? {
        return database?.vaultDao()
    }

    @Synchronized
    fun closeDatabase() {
        try {
            database?.close()
        } catch (e: Exception) {
            // Ignore close exception
        }
        database = null
        activePassphrase?.fill(0)
        activePassphrase = null
        _isUnlocked.value = false
    }
}
