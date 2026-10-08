package com.vaultguard.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vaultguard.app.domain.model.FieldMapping
import com.vaultguard.app.domain.model.SecretType
import com.vaultguard.app.domain.model.VaultItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val name: String,
    val username: String = "",
    val password: String = "",
    val urlOrPackage: String = "",
    val apiKey: String = "",
    val notes: String = "",
    val folderOrTag: String = "",
    val isFavorite: Boolean = false,
    val customFieldMappingsJson: String = "[]",
    val customIconUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = 0L
) {
    fun toDomain(): VaultItem {
        val mappings = try {
            Json.decodeFromString<List<FieldMapping>>(customFieldMappingsJson)
        } catch (e: Exception) {
            emptyList()
        }
        val secretType = try {
            SecretType.valueOf(type)
        } catch (e: Exception) {
            SecretType.LOGIN
        }

        return VaultItem(
            id = id,
            type = secretType,
            name = name,
            username = username,
            password = password,
            urlOrPackage = urlOrPackage,
            apiKey = apiKey,
            notes = notes,
            folderOrTag = folderOrTag,
            isFavorite = isFavorite,
            customFieldMappings = mappings,
            customIconUri = customIconUri,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastUsedAt = lastUsedAt
        )
    }

    companion object {
        fun fromDomain(item: VaultItem): VaultItemEntity {
            val mappingsJson = try {
                Json.encodeToString(item.customFieldMappings)
            } catch (e: Exception) {
                "[]"
            }
            return VaultItemEntity(
                id = item.id,
                type = item.type.name,
                name = item.name,
                username = item.username,
                password = item.password,
                urlOrPackage = item.urlOrPackage,
                apiKey = item.apiKey,
                notes = item.notes,
                folderOrTag = item.folderOrTag,
                isFavorite = item.isFavorite,
                customFieldMappingsJson = mappingsJson,
                customIconUri = item.customIconUri,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
                lastUsedAt = item.lastUsedAt
            )
        }
    }
}
