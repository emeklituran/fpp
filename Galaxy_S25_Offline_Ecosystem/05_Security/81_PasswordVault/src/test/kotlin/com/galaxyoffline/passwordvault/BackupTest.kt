package com.galaxyoffline.passwordvault

import com.galaxyoffline.passwordvault.data.ItemType
import com.galaxyoffline.passwordvault.data.VaultItem
import com.galaxyoffline.passwordvault.data.VaultRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

class BackupTest {
    @Test fun encryptedBackupRoundTrip() {
        val items = listOf(VaultItem(type = ItemType.LOGIN, title = "E-posta", username = "ali@ornek.com", secret = "Çok-Gizli-123!"),
            VaultItem(type = ItemType.CARD, title = "Kart", secret = "4111111111111111", pin = "123"))
        val json = VaultRepository.exportJson(items, "AnaŞifre2026".toCharArray())
        assertFalse("düz metin sızdı", json.contains("Çok-Gizli") || json.contains("ali@ornek.com"))
        val back = VaultRepository.importJson(json, "AnaŞifre2026".toCharArray())
        assertEquals(items.map { it.secret }, back.map { it.secret })
        assertEquals("123", back[1].pin)
        try { VaultRepository.importJson(json, "yanlis".toCharArray()); fail() } catch (e: javax.crypto.AEADBadTagException) { }
    }
}
