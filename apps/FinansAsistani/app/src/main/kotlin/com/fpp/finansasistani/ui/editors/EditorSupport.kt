package com.fpp.finansasistani.ui.editors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fpp.finans.core.Money

@Composable
internal fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Form doğrulama yardımcıları. Hatalar yalnızca kullanıcı "Kaydet"e bastıktan sonra gösterilir. */
internal object Validate {
    const val MAX_AMOUNT = 1e12

    fun name(value: String, show: Boolean): String? =
        if (show && value.isBlank()) "Bu alan zorunludur." else null

    /** Pozitif tutar zorunlu. */
    fun positive(value: String, show: Boolean): String? {
        if (!show) return null
        val v = Money.parse(value) ?: return "Geçerli bir tutar girin."
        return when {
            v <= 0.0 -> "Tutar sıfırdan büyük olmalı."
            v > MAX_AMOUNT -> "Tutar çok büyük."
            else -> null
        }
    }

    /** Boş bırakılabilir (0 kabul edilir), girildiyse geçerli olmalı. */
    fun optional(value: String, show: Boolean, max: Double = MAX_AMOUNT): String? {
        if (!show || value.isBlank()) return null
        val v = Money.parse(value) ?: return "Geçerli bir sayı girin."
        return if (v < 0.0 || v > max) "Değer 0 ile ${Money.formatInput(max)} arasında olmalı." else null
    }

    fun parseOrZero(value: String): Double = Money.parse(value)?.takeIf { it >= 0.0 } ?: 0.0
}
