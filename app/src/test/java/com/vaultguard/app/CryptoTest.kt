package com.vaultguard.app

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.security.SecureRandom
import java.util.Arrays

class CryptoTest {

    @Test
    fun testArgon2idKeyDerivation() {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)

        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(2)
            .withMemoryAsKB(1024)
            .withParallelism(1)
            .withSalt(salt)
            .build()

        val generator = Argon2BytesGenerator()
        generator.init(params)

        val key1 = ByteArray(32)
        val passwordBytes = "SuperSecretMasterPassword123!".toByteArray(Charsets.UTF_8)
        generator.generateBytes(passwordBytes, key1, 0, key1.size)

        assertEquals(32, key1.size)

        // Derivation with same salt and password should produce identical key
        val generator2 = Argon2BytesGenerator()
        generator2.init(params)
        val key2 = ByteArray(32)
        generator2.generateBytes(passwordBytes, key2, 0, key2.size)

        assertTrue(Arrays.equals(key1, key2))

        // Different password should produce completely different key
        val generator3 = Argon2BytesGenerator()
        generator3.init(params)
        val key3 = ByteArray(32)
        generator3.generateBytes("DifferentPassword456".toByteArray(Charsets.UTF_8), key3, 0, key3.size)

        assertFalse(Arrays.equals(key1, key3))
    }
}
