package com.galaxyoffline.core.security

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.view.WindowManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/** Ekran görüntüsü, ekran kaydı ve son uygulamalar önizlemesini engeller. */
fun Activity.applySecureWindow(enabled: Boolean = true) {
    if (enabled) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
}

/** Hassas metni panoya kopyalar (klavye önerilerinde gizlenir) ve süre dolunca, hâlâ bizimse, panoyu temizler. */
object SecureClipboard {
    private val handler = Handler(Looper.getMainLooper())

    fun copy(context: Context, label: String, text: String, clearAfterSeconds: Int = 30) {
        val cm = context.getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText(label, text).apply {
            description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        }
        cm.setPrimaryClip(clip)
        if (clearAfterSeconds > 0) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({
                runCatching {
                    val current = cm.primaryClip?.getItemAt(0)?.text?.toString()
                    if (current == text) cm.clearPrimaryClip()
                }
            }, clearAfterSeconds * 1000L)
        }
    }
}

/**
 * Uygulama arka plana geçtiğinde zamanı kaydeder; [timeoutMs] aşılırsa [onLock] çağrılır.
 * 0 = arka plana geçer geçmez kilitle.
 */
class AutoLock(private val timeoutMs: () -> Long, private val onLock: () -> Unit) : DefaultLifecycleObserver {
    private var backgroundAt = 0L
    override fun onStop(owner: LifecycleOwner) {
        backgroundAt = System.currentTimeMillis()
        if (timeoutMs() == 0L) onLock()
    }
    override fun onStart(owner: LifecycleOwner) {
        if (backgroundAt > 0 && System.currentTimeMillis() - backgroundAt >= timeoutMs()) onLock()
        backgroundAt = 0
    }
    fun install() = ProcessLifecycleOwner.get().lifecycle.addObserver(this)
}
