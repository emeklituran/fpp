package com.fpp.finansasistani.util

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Hataları yakalayan coroutine başlatıcı: beklenmeyen bir istisna uygulamayı çökertmez,
 * kullanıcıya anlaşılır bir mesaj gösterilir.
 */
fun ViewModel.launchSafe(
    bus: MessageBus,
    errorMessage: String = "İşlem tamamlanamadı. Lütfen tekrar deneyin.",
    block: suspend CoroutineScope.() -> Unit,
): Job {
    val handler = CoroutineExceptionHandler { _, throwable ->
        Log.e("FinansAsistani", errorMessage, throwable)
        bus.post(errorMessage)
    }
    return viewModelScope.launch(handler, block = block)
}
