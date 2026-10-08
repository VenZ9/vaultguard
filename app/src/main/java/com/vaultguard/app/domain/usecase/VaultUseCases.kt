package com.vaultguard.app.domain.usecase

import com.vaultguard.app.domain.model.PasswordGeneratorConfig
import com.vaultguard.app.domain.model.SecretType
import com.vaultguard.app.domain.model.SortOrder
import com.vaultguard.app.domain.model.VaultItem
import com.vaultguard.app.domain.repository.VaultRepository
import com.vaultguard.app.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetVaultItemsUseCase @Inject constructor(
    private val vaultRepository: VaultRepository
) {
    operator fun invoke(
        query: String = "",
        typeFilter: SecretType? = null,
        favoriteOnly: Boolean = false,
        tagFilter: String? = null,
        sortOrder: SortOrder = SortOrder.NAME
    ): Flow<List<VaultItem>> {
        val baseFlow = if (query.isNotBlank()) {
            vaultRepository.searchItems(query.trim())
        } else if (typeFilter != null) {
            vaultRepository.getItemsByType(typeFilter)
        } else if (favoriteOnly) {
            vaultRepository.getFavorites()
        } else {
            vaultRepository.getAllItems()
        }

        return baseFlow.map { list ->
            var filtered = list

            if (typeFilter != null && query.isNotBlank()) {
                filtered = filtered.filter { it.type == typeFilter }
            }

            if (favoriteOnly && (query.isNotBlank() || typeFilter != null)) {
                filtered = filtered.filter { it.isFavorite }
            }

            if (!tagFilter.isNullOrBlank()) {
                filtered = filtered.filter { it.folderOrTag.equals(tagFilter, ignoreCase = true) }
            }

            when (sortOrder) {
                SortOrder.NAME -> filtered.sortedBy { it.name.lowercase() }
                SortOrder.DATE_ADDED -> filtered.sortedByDescending { it.createdAt }
                SortOrder.LAST_USED -> filtered.sortedByDescending { it.lastUsedAt }
            }
        }
    }
}

@Singleton
class SaveVaultItemUseCase @Inject constructor(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(item: VaultItem) {
        val updatedItem = item.copy(
            updatedAt = System.currentTimeMillis()
        )
        vaultRepository.saveItem(updatedItem)
    }
}

@Singleton
class DeleteVaultItemUseCase @Inject constructor(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(item: VaultItem) {
        vaultRepository.deleteItem(item)
    }

    suspend fun byId(id: String) {
        vaultRepository.deleteItemById(id)
    }
}

data class PasswordStrength(
    val score: Int, // 0 to 4
    val label: String,
    val entropyBits: Double
)

@Singleton
class GeneratePasswordUseCase @Inject constructor() {
    private val secureRandom = SecureRandom()

    private val upperChars = "ABCDEFGHJKLMNPQRSTUVWXYZ" // O omitted to avoid confusion
    private val lowerChars = "abcdefghijkmnopqrstuvwxyz" // l omitted
    private val numberChars = "23456789" // 0, 1 omitted
    private val symbolChars = "!@#$%^&*()-_=+[]{}|;:,.<>?"

    operator fun invoke(config: PasswordGeneratorConfig): String {
        val pool = StringBuilder()
        val guaranteed = mutableListOf<Char>()

        if (config.includeUppercase) {
            pool.append(upperChars)
            guaranteed.add(upperChars[secureRandom.nextInt(upperChars.length)])
        }
        if (config.includeLowercase) {
            pool.append(lowerChars)
            guaranteed.add(lowerChars[secureRandom.nextInt(lowerChars.length)])
        }
        if (config.includeNumbers) {
            pool.append(numberChars)
            guaranteed.add(numberChars[secureRandom.nextInt(numberChars.length)])
        }
        if (config.includeSymbols) {
            pool.append(symbolChars)
            guaranteed.add(symbolChars[secureRandom.nextInt(symbolChars.length)])
        }

        if (pool.isEmpty()) {
            pool.append(lowerChars)
            guaranteed.add(lowerChars[secureRandom.nextInt(lowerChars.length)])
        }

        val poolStr = pool.toString()
        val result = mutableListOf<Char>()
        result.addAll(guaranteed)

        while (result.size < config.length) {
            result.add(poolStr[secureRandom.nextInt(poolStr.length)])
        }

        // Shuffle securely
        for (i in result.indices.reversed()) {
            val j = secureRandom.nextInt(i + 1)
            val temp = result[i]
            result[i] = result[j]
            result[j] = temp
        }

        return result.take(config.length).joinToString("")
    }

    fun calculateStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength(0, "Empty", 0.0)

        var poolSize = 0
        if (password.any { it.isUpperCase() }) poolSize += 26
        if (password.any { it.isLowerCase() }) poolSize += 26
        if (password.any { it.isDigit() }) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32

        if (poolSize == 0) poolSize = 26

        val entropy = password.length * (Math.log(poolSize.toDouble()) / Math.log(2.0))

        val (score, label) = when {
            entropy < 28 -> 0 to "Very Weak"
            entropy < 40 -> 1 to "Weak"
            entropy < 60 -> 2 to "Fair"
            entropy < 80 -> 3 to "Strong"
            else -> 4 to "Very Strong"
        }

        return PasswordStrength(score, label, entropy)
    }
}

@Singleton
class ExportVaultUseCase @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val cryptoManager: CryptoManager
) {
    suspend operator fun invoke(passphrase: String): ByteArray {
        val items = vaultRepository.getAllItemsSnapshot()
        val json = Json.encodeToString(items)
        val jsonBytes = json.toByteArray(Charsets.UTF_8)
        return cryptoManager.encryptWithPassphrase(jsonBytes, passphrase)
    }
}

@Singleton
class ImportVaultUseCase @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val cryptoManager: CryptoManager
) {
    suspend operator fun invoke(encryptedPackage: ByteArray, passphrase: String): Int {
        val decryptedBytes = cryptoManager.decryptWithPassphrase(encryptedPackage, passphrase)
        val json = String(decryptedBytes, Charsets.UTF_8)
        val items = Json.decodeFromString<List<VaultItem>>(json)
        vaultRepository.saveAllItems(items)
        return items.size
    }
}
