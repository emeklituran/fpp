package com.galaxyoffline.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

object Biometrics {
    fun strongAvailable(context: Context) =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    fun anyAvailable(context: Context) =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    /** Kriptografik olarak bağlı biyometrik doğrulama: başarılı olursa Keystore şifresi açılmış [Cipher] döner. */
    fun authenticateCipher(
        activity: FragmentActivity,
        cipher: Cipher,
        title: String,
        subtitle: String,
        onSuccess: (Cipher) -> Unit,
        onError: (String) -> Unit,
    ) {
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val c = result.cryptoObject?.cipher
                if (c != null) onSuccess(c) else onError("Doğrulama sonucu alınamadı")
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED) onError(errString.toString())
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder().setTitle(title).setSubtitle(subtitle)
            .setNegativeButtonText("Ana şifreyi kullan").setAllowedAuthenticators(BIOMETRIC_STRONG).build()
        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    /** Uygulama kilidi: biyometri veya cihaz ekran kilidi (PIN/desen). */
    fun authenticateDevice(activity: FragmentActivity, title: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_CANCELED) onError(errString.toString())
            }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(title)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL).build())
    }
}
