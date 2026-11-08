package com.galaxyoffline.core.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GSize
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.galaxyoffline.core.designsystem.components.EmptyState
import java.util.concurrent.Executors

/**
 * Kamera önizlemesi + QR/barkod tarayıcı. Kamera izni yoksa ister; reddedilirse açıklama gösterir.
 * Görüntüler yalnızca bellekte işlenir, kaydedilmez.
 */
@Composable
fun QrScannerView(onResult: (ScanResult) -> Unit, modifier: Modifier = Modifier, torch: Boolean = false) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; asked = true }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    if (!granted) {
        Box(modifier) {
            EmptyState(Icons.Outlined.CameraAlt, "Kamera izni gerekli",
                if (asked) "Kod taramak için kamera iznine ihtiyaç var. İzin vermezseniz galeriden görsel seçebilir veya kodu elle girebilirsiniz."
                else "Kod taramak için kamera izni isteniyor.",
                actionLabel = "İzin ver", onAction = { launcher.launch(Manifest.permission.CAMERA) })
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember { QrAnalyzer { r -> ContextCompat.getMainExecutor(context).execute { onResult(r) } } }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    LaunchedEffect(torch, camera) { camera?.cameraControl?.enableTorch(torch) }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val view = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    runCatching {
                        val provider = future.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                        val analysis = ImageAnalysis.Builder()
                            .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
                                ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build())
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                            .also { it.setAnalyzer(executor, analyzer) }
                        provider.unbindAll()
                        camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                view
            },
        )
        // Hedef çerçeve
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            val side = size.minDimension * 0.68f
            val tl = Offset((size.width - side) / 2, (size.height - side) / 2)
            drawRect(Color.Black.copy(alpha = 0.45f))
            drawRoundRect(Color.Transparent, tl, GSize(side, side), CornerRadius(32.dp.toPx()), blendMode = BlendMode.Clear)
            drawRoundRect(Color.White, tl, GSize(side, side), CornerRadius(32.dp.toPx()), style = Stroke(3.dp.toPx()))
        }
    }
}
