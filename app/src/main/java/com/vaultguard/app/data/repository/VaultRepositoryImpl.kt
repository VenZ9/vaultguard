package com.vaultguard.app.data.repository

import com.vaultguard.app.data.local.DatabaseManager
import com.vaultguard.app.data.local.VaultItemEntity
import com.vaultguard.app.domain.model.SecretType
import com.vaultguard.app.domain.model.VaultItem
import com.vaultguard.app.domain.repository.VaultRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val databaseManager: DatabaseManager
) : VaultRepository {

    override fun getAllItems(): Flow<List<VaultItem>> {
        return databaseManager.isUnlocked.flatMapLatest { unlocked ->
            if (!unlocked) {
                flowOf(emptyList())
            } else {
                databaseManager.getDao()?.getAllItems()?.map { list ->
                    list.map { it.toDomain() }
                } ?: flowOf(emptyList())
            }
        }
    }

    override fun getItemById(id: String): Flow<VaultItem?> {
        return databaseManager.isUnlocked.flatMapLatest { unlocked ->
            if (!unlocked) {
                flowOf(null)
            } else {
                databaseManager.getDao()?.getItemById(id)?.map { it?.toDomain() }
                    ?: flowOf(null)
            }
        }
    }

    override suspend fun getItemByIdSync(id: String): VaultItem? {
        return databaseManager.getDao()?.getItemByIdSync(id)?.toDomain()
    }

    override fun searchItems(query: String): Flow<List<VaultItem>> {
        return databaseManager.isUnlocked.flatMapLatest { unlocked ->
            if (!unlocked) {
                flowOf(emptyList())
            } else {
                databaseManager.getDao()?.searchItems(query)?.map { list ->
                    list.map { it.toDomain() }
                } ?: flowOf(emptyList())
            }
        }
    }

    override fun getItemsByType(type: SecretType): Flow<List<VaultItem>> {
        return databaseManager.isUnlocked.flatMapLatest { unlocked ->
            if (!unlocked) {
                flowOf(emptyList())
            } else {
                databaseManager.getDao()?.getItemsByType(type.name)?.map { list ->
                    list.map { it.toDomain() }
                } ?: flowOf(emptyList())
            }
        }
    }

    override fun getFavorites(): Flow<List<VaultItem>> {
        return databaseManager.isUnlocked.flatMapLatest { unlocked ->
            if (!unlocked) {
                flowOf(emptyList())
            } else {
                databaseManager.getDao()?.getFavorites()?.map { list ->
                    list.map { it.toDomain() }
                } ?: flowOf(emptyList())
            }
        }
    }

    override suspend fun findMatchingItemsForAutofill(query: String): List<VaultItem> {
        val dao = databaseManager.getDao() ?: return emptyList()
        return dao.findMatchingItemsForAutofill(query).map { it.toDomain() }
    }

    override suspend fun getAllItemsSnapshot(): List<VaultItem> {
        val dao = databaseManager.getDao() ?: return emptyList()
        return dao.getAllItemsSnapshot().map { it.toDomain() }
    }

    override suspend fun saveItem(item: VaultItem) {
        val dao = databaseManager.getDao() ?: throw IllegalStateException("Database is locked")
        val entity = VaultItemEntity.fromDomain(item)
        dao.insert(entity)
    }

    override suspend fun saveAllItems(items: List<VaultItem>) {
        val dao = databaseManager.getDao() ?: throw IllegalStateException("Database is locked")
        val entities = items.map { VaultItemEntity.fromDomain(it) }
        dao.insertAll(entities)
    }

    override suspend fun deleteItem(item: VaultItem) {
        val dao = databaseManager.getDao() ?: throw IllegalStateException("Database is locked")
        dao.delete(VaultItemEntity.fromDomain(item))
    }

    override suspend fun deleteItemById(id: String) {
        val dao = databaseManager.getDao() ?: throw IllegalStateException("Database is locked")
        dao.deleteById(id)
    }

    override suspend fun updateLastUsed(id: String, timestamp: Long) {
        val dao = databaseManager.getDao() ?: return
        dao.updateLastUsed(id, timestamp)
    }
}
