package com.galaxyoffline.core.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM şifreleme ve PBKDF2-HMAC-SHA256 anahtar türetme (yalnızca JDK/Android standart sağlayıcıları).
 * Biçim: [12 bayt IV][şifreli metin + 16 bayt doğrulama etiketi]. Yanlış anahtar veya bozulmuş veri AEADBadTagException fırlatır.
 */
object CryptoBox {
    const val KEY_BYTES = 32
    const val IV_BYTES = 12
    const val TAG_BITS = 128
    const val SALT_BYTES = 16
    /** Galaxy S25 sınıfı cihazda yaklaşık 0,3–0,6 sn; kaba kuvvet denemelerini pahalı hale getirir. */
    const val DEFAULT_ITERATIONS = 310_000

    private val random = SecureRandom()

    fun randomBytes(n: Int) = ByteArray(n).also { random.nextBytes(it) }
    fun newKey(): ByteArray = randomBytes(KEY_BYTES)

    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = DEFAULT_ITERATIONS): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BYTES * 8)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun encrypt(key: ByteArray, plain: ByteArray, aad: ByteArray? = null): ByteArray {
        val iv = randomBytes(IV_BYTES)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        if (aad != null) c.updateAAD(aad)
        return iv + c.doFinal(plain)
    }

    fun decrypt(key: ByteArray, data: ByteArray, aad: ByteArray? = null): ByteArray {
        require(data.size > IV_BYTES) { "Şifreli veri çok kısa" }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES))
        if (aad != null) c.updateAAD(aad)
        return c.doFinal(data, IV_BYTES, data.size - IV_BYTES)
    }

    fun encryptString(key: ByteArray, text: String): String = b64(encrypt(key, text.toByteArray(Charsets.UTF_8)))
    fun decryptString(key: ByteArray, data: String): String = String(decrypt(key, unb64(data)), Charsets.UTF_8)

    fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    fun unb64(s: String): ByteArray = Base64.getDecoder().decode(s)

    /** Sabit süreli karşılaştırma (zamanlama saldırılarına karşı). */
    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var r = 0
        for (i in a.indices) r = r or (a[i].toInt() xor b[i].toInt())
        return r == 0
    }

    fun wipe(bytes: ByteArray?) { bytes?.fill(0) }
    fun wipe(chars: CharArray?) { chars?.fill('\u0000') }
}
