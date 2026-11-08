package com.galaxyoffline.core.scanner

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

data class ScanResult(val text: String, val format: String)

/** ZXing ile tamamen cihaz üstü kod çözme ve QR üretimi (ağ bağlantısı yok). */
object QrCodes {
    private val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(
            BarcodeFormat.QR_CODE, BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E,
            BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.ITF, BarcodeFormat.DATA_MATRIX, BarcodeFormat.PDF_417, BarcodeFormat.AZTEC,
        ),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to "UTF-8",
    )

    fun reader() = MultiFormatReader().apply { setHints(hints) }

    fun formatName(f: BarcodeFormat) = when (f) {
        BarcodeFormat.QR_CODE -> "QR kod"; BarcodeFormat.EAN_13, BarcodeFormat.EAN_8 -> "EAN barkod"
        BarcodeFormat.UPC_A, BarcodeFormat.UPC_E -> "UPC barkod"; BarcodeFormat.DATA_MATRIX -> "Data Matrix"
        else -> f.name.replace('_', ' ')
    }

    /** Galeriden seçilen görselden kod okur. */
    fun decodeBitmap(bitmap: Bitmap): ScanResult? {
        val scaled = if (maxOf(bitmap.width, bitmap.height) > 1600) {
            val r = 1600f / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * r).toInt(), (bitmap.height * r).toInt(), true)
        } else bitmap
        val px = IntArray(scaled.width * scaled.height)
        scaled.getPixels(px, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        val src = RGBLuminanceSource(scaled.width, scaled.height, px)
        return try {
            val r = reader().decode(BinaryBitmap(HybridBinarizer(src)))
            ScanResult(r.text, formatName(r.barcodeFormat))
        } catch (e: NotFoundException) {
            runCatching { reader().decode(BinaryBitmap(HybridBinarizer(src.invert()))) }.getOrNull()?.let { ScanResult(it.text, formatName(it.barcodeFormat)) }
        }
    }

    fun encode(text: String, size: Int = 768, dark: Int = Color.BLACK, light: Int = Color.WHITE): Bitmap {
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 2, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
        val px = IntArray(m.width * m.height) { i -> if (m.get(i % m.width, i / m.width)) dark else light }
        return Bitmap.createBitmap(px, m.width, m.height, Bitmap.Config.ARGB_8888)
    }
}

/** Kamera karelerini (Y düzlemi) ZXing'e verir; aynı sonucu tekrar bildirmez. */
class QrAnalyzer(private val onResult: (ScanResult) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = QrCodes.reader()
    @Volatile private var done = false

    override fun analyze(image: ImageProxy) {
        if (done) { image.close(); return }
        try {
            val plane = image.planes[0]
            val buf = plane.buffer
            val data = ByteArray(buf.remaining()).also { buf.get(it) }
            val src = PlanarYUVLuminanceSource(data, plane.rowStride, image.height, 0, 0, image.width, image.height, false)
            val r = try { reader.decodeWithState(BinaryBitmap(HybridBinarizer(src))) } catch (e: NotFoundException) { null } finally { reader.reset() }
            if (r != null) { done = true; onResult(ScanResult(r.text, QrCodes.formatName(r.barcodeFormat))) }
        } catch (e: Exception) {
            // Bozuk kare: atla
        } finally {
            image.close()
        }
    }

    fun reset() { done = false }
}
