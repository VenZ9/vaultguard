package com.vaultguard.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [VaultItemEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
}
