package com.galaxyoffline.passwordvault.data

import android.content.Context
import com.galaxyoffline.core.crypto.CryptoBox
import com.galaxyoffline.core.security.KeystoreKeys
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher

/**
 * Anahtar hiyerarşisi:
 *  - Rastgele 256 bit veri anahtarı (DEK) tüm kayıtları şifreler.
 *  - DEK, ana şifreden PBKDF2 ile türetilen anahtarla (KEK) AES-GCM ile sarılır.
 *  - İsteğe bağlı: DEK ayrıca biyometriye bağlı Keystore anahtarıyla sarılır.
 * Ana şifre hiçbir yerde saklanmaz.
 */
class VaultKeys(context: Context) {
    private val prefs = context.getSharedPreferences("kasa_anahtarlari", Context.MODE_PRIVATE)

    val isSetUp: Boolean get() = prefs.contains(K_WRAPPED)
    val biometricEnabled: Boolean get() = prefs.contains(K_BIO) && KeystoreKeys.exists(BIO_ALIAS)

    fun setup(password: CharArray): ByteArray {
        val dek = CryptoBox.newKey()
        wrapWithPassword(dek, password)
        return dek
    }

    private fun wrapWithPassword(dek: ByteArray, password: CharArray) {
        val salt = CryptoBox.randomBytes(CryptoBox.SALT_BYTES)
        val kek = CryptoBox.deriveKey(password, salt, CryptoBox.DEFAULT_ITERATIONS)
        val wrapped = CryptoBox.encrypt(kek, dek, AAD)
        CryptoBox.wipe(kek)
        prefs.edit().putString(K_SALT, CryptoBox.b64(salt)).putInt(K_ITER, CryptoBox.DEFAULT_ITERATIONS)
            .putString(K_WRAPPED, CryptoBox.b64(wrapped)).apply()
    }

    /** Yanlış şifrede null. */
    fun unlock(password: CharArray): ByteArray? {
        val salt = CryptoBox.unb64(prefs.getString(K_SALT, null) ?: return null)
        val kek = CryptoBox.deriveKey(password, salt, prefs.getInt(K_ITER, CryptoBox.DEFAULT_ITERATIONS))
        return try {
            CryptoBox.decrypt(kek, CryptoBox.unb64(prefs.getString(K_WRAPPED, null)!!), AAD)
        } catch (e: AEADBadTagException) {
            null
        } finally {
            CryptoBox.wipe(kek)
        }
    }

    fun changePassword(dek: ByteArray, newPassword: CharArray) = wrapWithPassword(dek, newPassword)

    fun bioEncryptCipher(): Cipher = KeystoreKeys.encryptCipher(BIO_ALIAS, requireBiometric = true)
    fun bioDecryptCipher(): Cipher? {
        val iv = prefs.getString(K_BIO_IV, null)?.let(CryptoBox::unb64) ?: return null
        return KeystoreKeys.decryptCipher(BIO_ALIAS, iv).also { if (it == null) disableBiometric() }
    }

    fun enableBiometric(authenticatedEncrypt: Cipher, dek: ByteArray) {
        val wrapped = authenticatedEncrypt.doFinal(dek)
        prefs.edit().putString(K_BIO, CryptoBox.b64(wrapped)).putString(K_BIO_IV, CryptoBox.b64(authenticatedEncrypt.iv)).apply()
    }

    fun unlockBiometric(authenticatedDecrypt: Cipher): ByteArray? =
        runCatching { authenticatedDecrypt.doFinal(CryptoBox.unb64(prefs.getString(K_BIO, null)!!)) }.getOrNull()

    fun disableBiometric() {
        prefs.edit().remove(K_BIO).remove(K_BIO_IV).apply()
        KeystoreKeys.delete(BIO_ALIAS)
    }

    // Hatalı deneme sınırlaması (kaba kuvvete karşı)
    val failures: Int get() = prefs.getInt(K_FAIL, 0)
    val lockedUntil: Long get() = prefs.getLong(K_LOCK_UNTIL, 0)
    fun recordFailure() {
        val f = failures + 1
        val wait = if (f >= 5) (30_000L shl (f - 5).coerceAtMost(5)).coerceAtMost(15 * 60_000L) else 0L
        prefs.edit().putInt(K_FAIL, f).putLong(K_LOCK_UNTIL, if (wait > 0) System.currentTimeMillis() + wait else 0).apply()
    }
    fun resetFailures() = prefs.edit().putInt(K_FAIL, 0).putLong(K_LOCK_UNTIL, 0).apply()

    private companion object {
        const val BIO_ALIAS = "kasa_biyometrik"
        const val K_SALT = "tuz"; const val K_ITER = "tur"; const val K_WRAPPED = "sarili_anahtar"
        const val K_BIO = "bio_sarili"; const val K_BIO_IV = "bio_iv"; const val K_FAIL = "hata"; const val K_LOCK_UNTIL = "bekleme"
        val AAD = "galaxy-kasa-v1".toByteArray()
    }
}
