package com.example.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityCrypto {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    data class SaltedHash(val saltBase64: String, val hashBase64: String)

    fun hashPassword(password: CharArray): SaltedHash {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val derived = deriveKey(password, salt)
        return SaltedHash(
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            hashBase64 = Base64.encodeToString(derived, Base64.NO_WRAP)
        )
    }

    fun verifyPassword(password: CharArray, saltBase64: String, expectedHashBase64: String): Boolean {
        return try {
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val expected = Base64.decode(expectedHashBase64, Base64.NO_WRAP)
            val actual = deriveKey(password, salt)
            MessageDigest.isEqual(actual, expected)
        } catch (_: Exception) {
            false
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
