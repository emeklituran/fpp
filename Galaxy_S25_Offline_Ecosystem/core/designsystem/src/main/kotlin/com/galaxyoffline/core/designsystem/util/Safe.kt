package com.galaxyoffline.core.designsystem.util

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** ViewModel'dan arayüze tek seferlik mesajlar (Snackbar). */
class UiMessages {
    private val channel = Channel<String>(Channel.BUFFERED)
    val flow: Flow<String> = channel.receiveAsFlow()
    fun post(message: String) { channel.trySend(message) }
}

/** Hata yakalayan coroutine: beklenmeyen istisna uygulamayı çökertmez, kullanıcıya mesaj gösterilir. */
fun ViewModel.launchSafe(
    messages: UiMessages? = null,
    errorMessage: String = "İşlem tamamlanamadı. Lütfen tekrar deneyin.",
    block: suspend CoroutineScope.() -> Unit,
): Job = viewModelScope.launch(
    CoroutineExceptionHandler { _, t ->
        Log.e("GalaxyOffline", errorMessage, t)
        messages?.post(errorMessage)
    },
    block = block,
)
