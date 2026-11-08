package com.galaxyoffline.pdfreader.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Pages
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.designsystem.components.BackButton
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

private val invert = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f)))

@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.FlowPreview::class)
@Composable
fun ViewerScreen(vm: PdfViewModel, open: OpenDoc, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val night by vm.night.collectAsStateWithLifecycle()
    val list = rememberLazyListState(initialFirstVisibleItemIndex = open.startPage)
    var zoom by remember { mutableFloatStateOf(1f) }
    var goTo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val current by remember { derivedStateOf { list.firstVisibleItemIndex } }
    LaunchedEffect(open.uri) { snapshotFlow { list.firstVisibleItemIndex }.debounce(600).collect { vm.savePage(it) } }
    BackHandler(onBack = onClose)
    Scaffold(topBar = {
        TopAppBar(title = { Column { Text(open.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text("Sayfa ${current + 1} / ${open.doc.pageCount}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.testTag("page_indicator")) } },
            navigationIcon = { BackButton(onClose) }, actions = {
                IconButton(onClick = { zoom = (zoom / 1.25f).coerceAtLeast(1f) }, enabled = zoom > 1f) { Icon(Icons.Outlined.ZoomOut, "Uzaklaştır") }
                IconButton(onClick = { zoom = (zoom * 1.25f).coerceAtMost(4f) }, enabled = zoom < 4f) { Icon(Icons.Outlined.ZoomIn, "Yakınlaştır") }
                IconButton(onClick = { goTo = true }) { Icon(Icons.Outlined.Pages, "Sayfaya git") }
                IconButton(onClick = { vm.setNight(!night) }) { Icon(if (night) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, "Gece modu") }
                IconButton(onClick = {
                    runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf")
                        .putExtra(Intent.EXTRA_STREAM, open.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "PDF paylaş")) }
                }) { Icon(Icons.Outlined.Share, "Paylaş") }
            })
    }) { p ->
        BoxWithConstraints(Modifier.padding(p).fillMaxSize().background(if (night) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceContainerHighest)
            .pointerInput(Unit) {
                // İki parmakla yakınlaştırma; tek parmak kaydırması listeye kalır.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val e = awaitPointerEvent()
                        if (e.changes.count { it.pressed } >= 2) {
                            zoom = (zoom * e.calculateZoom()).coerceIn(1f, 4f)
                            e.changes.forEach { it.consume() }
                        }
                    } while (e.changes.any { it.pressed })
                }
            }) {
            val density = LocalDensity.current
            val pageWidth = maxWidth * zoom
            val widthPx = with(density) { (pageWidth - 16.dp).roundToPx() }
            Box(Modifier.fillMaxSize().then(if (zoom > 1f) Modifier.horizontalScroll(rememberScrollState()) else Modifier)) {
                LazyColumn(state = list, modifier = Modifier.width(pageWidth).fillMaxSize(), contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(open.doc.pageCount, key = { it }) { i ->
                        PageItem(open, i, widthPx, night)
                    }
                }
            }
        }
    }
    if (goTo) {
        var t by remember { mutableStateOf("${current + 1}") }
        AlertDialog(onDismissRequest = { goTo = false }, title = { Text("Sayfaya git") },
            text = { OutlinedTextField(t, { t = it.filter(Char::isDigit).take(5) }, label = { Text("1 – ${open.doc.pageCount}") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
            confirmButton = { TextButton(onClick = {
                t.toIntOrNull()?.let { n -> scope.launch { list.scrollToItem((n - 1).coerceIn(0, open.doc.pageCount - 1)) } }; goTo = false
            }) { Text("Git") } },
            dismissButton = { TextButton(onClick = { goTo = false }) { Text("Vazgeç") } })
    }
}

@Composable
private fun PageItem(open: OpenDoc, index: Int, widthPx: Int, night: Boolean) {
    val bmp by produceState<Bitmap?>(null, open.uri, index, widthPx) { value = open.doc.render(index, widthPx) }
    Surface(Modifier.fillMaxWidth().aspectRatio(1f / open.doc.ratios[index].coerceIn(0.2f, 5f)), color = Color.White, shadowElevation = 2.dp) {
        val b = bmp
        if (b == null) Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else Image(b.asImageBitmap(), "Sayfa ${index + 1}", Modifier.fillMaxSize(), colorFilter = if (night) invert else null)
    }
}
