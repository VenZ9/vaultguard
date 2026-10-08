package com.vaultguard.app.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor() {

    private val secureRandom = SecureRandom()
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    companion object {
        private const val KEYSTORE_ALIAS = "VaultGuard_KeyStore_Key"
        private const val ARGON2_ITERATIONS = 2
        private const val ARGON2_MEMORY_KB = 16 * 1024 // 16MB for responsive mobile speed & security
        private const val ARGON2_PARALLELISM = 1
        private const val KEY_LENGTH_BYTES = 32 // 256 bits
        private const val SALT_LENGTH_BYTES = 16
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Derives a 256-bit key from password and salt using Argon2id
     */
    fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(ARGON2_ITERATIONS)
            .withMemoryAsKB(ARGON2_MEMORY_KB)
            .withParallelism(ARGON2_PARALLELISM)
            .withSalt(salt)
            .build()

        val generator = Argon2BytesGenerator()
        generator.init(params)

        val result = ByteArray(KEY_LENGTH_BYTES)
        val passwordBytes = String(password).toByteArray(Charsets.UTF_8)
        generator.generateBytes(passwordBytes, result, 0, result.size)
        return result
    }

    /**
     * Hashes master password for verification
     */
    fun hashPassword(password: String, salt: ByteArray): ByteArray {
        return deriveKey(password.toCharArray(), salt)
    }

    /**
     * Android Keystore Key for Biometric Key Wrapping
     */
    private fun getOrCreateKeyStoreSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
        return (keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Wraps raw database key using Android Keystore AES-GCM
     */
    fun wrapKeyWithKeyStore(rawKey: ByteArray): ByteArray {
        val secretKey = getOrCreateKeyStoreSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(rawKey)

        // Store [12 bytes IV][ciphertext + tag]
        val result = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)
        return result
    }

    /**
     * Unwraps raw database key using Android Keystore AES-GCM
     */
    fun unwrapKeyWithKeyStore(wrappedKey: ByteArray): ByteArray? {
        return try {
            val secretKey = getOrCreateKeyStoreSecretKey()
            if (wrappedKey.size < GCM_IV_LENGTH_BYTES) return null

            val iv = ByteArray(GCM_IV_LENGTH_BYTES)
            val cipherText = ByteArray(wrappedKey.size - GCM_IV_LENGTH_BYTES)
            System.arraycopy(wrappedKey, 0, iv, 0, GCM_IV_LENGTH_BYTES)
            System.arraycopy(wrappedKey, GCM_IV_LENGTH_BYTES, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            cipher.doFinal(cipherText)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Encrypts arbitrary plaintext (e.g. backup JSON) using AES-256-GCM derived from user passphrase
     */
    fun encryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray {
        val salt = generateSalt()
        val keyBytes = deriveKey(passphrase.toCharArray(), salt)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)

        // Output: [16 bytes salt][12 bytes IV][ciphertext + tag]
        val output = ByteArray(salt.size + iv.size + encrypted.size)
        System.arraycopy(salt, 0, output, 0, salt.size)
        System.arraycopy(iv, 0, output, salt.size, iv.size)
        System.arraycopy(encrypted, 0, output, salt.size + iv.size, encrypted.size)
        return output
    }

    /**
     * Decrypts ciphertext (e.g. backup JSON) using AES-256-GCM derived from user passphrase
     */
    fun decryptWithPassphrase(encryptedPackage: ByteArray, passphrase: String): ByteArray {
        val minLength = SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES + 16
        if (encryptedPackage.size < minLength) {
            throw IllegalArgumentException("Invalid encrypted package length")
        }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        val cipherText = ByteArray(encryptedPackage.size - SALT_LENGTH_BYTES - GCM_IV_LENGTH_BYTES)

        System.arraycopy(encryptedPackage, 0, salt, 0, SALT_LENGTH_BYTES)
        System.arraycopy(encryptedPackage, SALT_LENGTH_BYTES, iv, 0, GCM_IV_LENGTH_BYTES)
        System.arraycopy(
            encryptedPackage,
            SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES,
            cipherText,
            0,
            cipherText.size
        )

        val keyBytes = deriveKey(passphrase.toCharArray(), salt)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(cipherText)
    }
}
