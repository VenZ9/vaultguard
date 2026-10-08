package com.vaultguard.app.security

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

data class GeneratedPasskey(
    val credentialId: String,
    val relyingParty: String,
    val userHandle: String,
    val publicKeyPem: String,
    val privateKeyPkcs8Base64: String,
    val keyFingerprint: String,
    val algorithm: String = "ES256 (ECDSA P-256)"
)

object PasskeyCrypto {

    private val secureRandom = SecureRandom()

    /**
     * Generates a modern FIDO2 / WebAuthn ES256 (secp256r1) keypair for passkey authentication.
     */
    fun generatePasskey(relyingParty: String, username: String): GeneratedPasskey {
        val keyPairGen = KeyPairGenerator.getInstance("EC")
        val ecSpec = ECGenParameterSpec("secp256r1")
        keyPairGen.initialize(ecSpec, secureRandom)
        val keyPair = keyPairGen.generateKeyPair()

        val privateKey = keyPair.private as ECPrivateKey
        val publicKey = keyPair.public as ECPublicKey

        // Generate a 32-byte secure random credential ID
        val credentialIdBytes = ByteArray(32).apply { secureRandom.nextBytes(this) }
        val credentialId = Base64.encodeToString(credentialIdBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

        // Generate user handle
        val userHandleBytes = username.toByteArray(Charsets.UTF_8)
        val userHandle = Base64.encodeToString(userHandleBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

        // Public key PEM
        val pubKeyBase64 = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        val publicKeyPem = "-----BEGIN PUBLIC KEY-----\n$pubKeyBase64\n-----END PUBLIC KEY-----"

        // Private key in PKCS#8 format
        val privateKeyPkcs8Base64 = Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP)

        // Key fingerprint (first 16 hex chars of SHA-256)
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val fingerprintBytes = md.digest(publicKey.encoded)
        val fingerprint = fingerprintBytes.take(8).joinToString(":") { "%02X".format(it) }

        return GeneratedPasskey(
            credentialId = credentialId,
            relyingParty = relyingParty,
            userHandle = userHandle,
            publicKeyPem = publicKeyPem,
            privateKeyPkcs8Base64 = privateKeyPkcs8Base64,
            keyFingerprint = fingerprint
        )
    }

    /**
     * Signs a WebAuthn authentication client data challenge using SHA256withECDSA.
     */
    fun signChallenge(privateKeyPkcs8Base64: String, challenge: ByteArray): ByteArray {
        val keyBytes = Base64.decode(privateKeyPkcs8Base64, Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        val privateKey = keyFactory.generatePrivate(keySpec)

        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(privateKey)
        signer.update(challenge)
        return signer.sign()
    }

    /**
     * Verifies a WebAuthn signature against the public key.
     */
    fun verifySignature(publicKeyPem: String, challenge: ByteArray, signature: ByteArray): Boolean {
        val cleanPem = publicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .trim()
        val keyBytes = Base64.decode(cleanPem, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        val publicKey = keyFactory.generatePublic(keySpec)

        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(publicKey)
        verifier.update(challenge)
        return verifier.verify(signature)
    }
}
