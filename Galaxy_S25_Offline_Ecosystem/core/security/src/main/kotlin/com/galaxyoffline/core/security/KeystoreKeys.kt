package com.galaxyoffline.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore'daki donanım destekli AES anahtarları. Anahtar materyali uygulamaya hiç verilmez;
 * şifreleme güvenli donanımda (TEE / StrongBox) yapılır.
 */
object KeystoreKeys {
    private const val PROVIDER = "AndroidKeyStore"
    private const val TRANSFORM = "AES/GCM/NoPadding"

    private fun keyStore() = KeyStore.getInstance(PROVIDER).apply { load(null) }

    fun exists(alias: String) = runCatching { keyStore().containsAlias(alias) }.getOrDefault(false)
    fun delete(alias: String) { runCatching { keyStore().deleteEntry(alias) } }

    /**
     * @param requireBiometric true ise anahtar yalnızca biyometrik doğrulamadan hemen sonra kullanılabilir ve
     * yeni parmak izi/yüz eklendiğinde geçersiz olur.
     */
    fun getOrCreate(alias: String, requireBiometric: Boolean): SecretKey {
        (keyStore().getKey(alias, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .apply {
                if (requireBiometric) {
                    setUserAuthenticationRequired(true)
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                    setInvalidatedByBiometricEnrollment(true)
                }
            }
            .build()
        gen.init(spec)
        return gen.generateKey()
    }

    fun encryptCipher(alias: String, requireBiometric: Boolean): Cipher =
        Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, getOrCreate(alias, requireBiometric)) }

    /** Biyometrik anahtar geçersizleşmişse (yeni parmak izi eklendi) null döner ve anahtar silinir. */
    fun decryptCipher(alias: String, iv: ByteArray): Cipher? = try {
        val key = keyStore().getKey(alias, null) as? SecretKey ?: return null
        Cipher.getInstance(TRANSFORM).apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv)) }
    } catch (e: KeyPermanentlyInvalidatedException) {
        delete(alias); null
    }

    /** Kullanıcı doğrulaması gerektirmeyen anahtarla doğrudan şifreleme ([iv][ct]). */
    fun seal(alias: String, plain: ByteArray): ByteArray {
        val c = encryptCipher(alias, requireBiometric = false)
        return c.iv + c.doFinal(plain)
    }

    fun open(alias: String, data: ByteArray): ByteArray {
        val c = decryptCipher(alias, data.copyOfRange(0, 12)) ?: error("Anahtar bulunamadı")
        return c.doFinal(data, 12, data.size - 12)
    }
}
