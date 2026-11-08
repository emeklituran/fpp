package com.galaxyoffline.passwordvault.data

import com.galaxyoffline.core.crypto.CryptoBox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Açık kasanın veri anahtarı yalnızca bellekte tutulur; kilitlenince sıfırlanır. */
object VaultSession {
    @Volatile var autoLockMs: Long = 60_000
    private var dek: ByteArray? = null
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    fun open(key: ByteArray) { dek?.let(CryptoBox::wipe); dek = key; _unlocked.value = true }
    fun key(): ByteArray? = dek
    fun lock() { dek?.let(CryptoBox::wipe); dek = null; _unlocked.value = false }
}
