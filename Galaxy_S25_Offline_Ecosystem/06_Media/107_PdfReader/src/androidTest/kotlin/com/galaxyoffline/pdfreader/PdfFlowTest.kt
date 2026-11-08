package com.galaxyoffline.pdfreader

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    /** Test sırasında 3 sayfalık bir PDF üretir, "Birlikte aç" akışıyla açar ve sayfa göstergesini doğrular. */
    @Test fun opensGeneratedPdf() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val f = File(ctx.cacheDir, "deneme.pdf")
        PdfDocument().apply {
            repeat(3) { i ->
                val page = startPage(PdfDocument.PageInfo.Builder(595, 842, i + 1).create())
                page.canvas.drawText("Sayfa ${i + 1}", 100f, 100f, Paint().apply { textSize = 40f })
                finishPage(page)
            }
            f.outputStream().use { writeTo(it) }
            close()
        }
        val uri = android.net.Uri.fromFile(f)
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("fab_open").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnUiThread { rule.activity.incoming.value = uri }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Sayfa 1 / 3").fetchSemanticsNodes().isNotEmpty() }
    }

}
