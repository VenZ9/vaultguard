package com.vaultguard.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_items ORDER BY name COLLATE NOCASE ASC")
    fun getAllItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    fun getItemById(id: String): Flow<VaultItemEntity?>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getItemByIdSync(id: String): VaultItemEntity?

    @Query("SELECT * FROM vault_items WHERE name LIKE '%' || :query || '%' OR urlOrPackage LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR folderOrTag LIKE '%' || :query || '%'")
    fun searchItems(query: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE type = :type ORDER BY name COLLATE NOCASE ASC")
    fun getItemsByType(type: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE isFavorite = 1 ORDER BY name COLLATE NOCASE ASC")
    fun getFavorites(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE urlOrPackage LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%'")
    suspend fun findMatchingItemsForAutofill(query: String): List<VaultItemEntity>

    @Query("SELECT * FROM vault_items")
    suspend fun getAllItemsSnapshot(): List<VaultItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VaultItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VaultItemEntity>)

    @Update
    suspend fun update(item: VaultItemEntity)

    @Delete
    suspend fun delete(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE vault_items SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)
}
