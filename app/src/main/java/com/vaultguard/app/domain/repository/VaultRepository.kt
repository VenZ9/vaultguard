package com.vaultguard.app.domain.repository

import com.vaultguard.app.domain.model.SecretType
import com.vaultguard.app.domain.model.VaultItem
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    fun getAllItems(): Flow<List<VaultItem>>
    fun getItemById(id: String): Flow<VaultItem?>
    suspend fun getItemByIdSync(id: String): VaultItem?
    fun searchItems(query: String): Flow<List<VaultItem>>
    fun getItemsByType(type: SecretType): Flow<List<VaultItem>>
    fun getFavorites(): Flow<List<VaultItem>>
    suspend fun findMatchingItemsForAutofill(query: String): List<VaultItem>
    suspend fun getAllItemsSnapshot(): List<VaultItem>
    suspend fun saveItem(item: VaultItem)
    suspend fun saveAllItems(items: List<VaultItem>)
    suspend fun deleteItem(item: VaultItem)
    suspend fun deleteItemById(id: String)
    suspend fun updateLastUsed(id: String, timestamp: Long)
}
