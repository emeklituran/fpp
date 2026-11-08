package com.galaxyoffline.chess.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.chess.data.GameMode
import com.galaxyoffline.chess.data.LEVELS
import com.galaxyoffline.chess.rules.ChessPiece
import com.galaxyoffline.chess.rules.Color as Side
import com.galaxyoffline.chess.rules.Fen
import com.galaxyoffline.chess.rules.FenResult
import com.galaxyoffline.chess.rules.GameResult
import com.galaxyoffline.chess.rules.PieceKind
import com.galaxyoffline.chess.rules.Sq

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameScreen(vm: GameViewModel, onEditor: () -> Unit, onSaved: () -> Unit, onSettings: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    var newGame by remember { mutableStateOf(false) }
    var fenDialog by remember { mutableStateOf(false) }
    var saveDialog by remember { mutableStateOf(false) }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    LifecycleResumeEffect(vm) { vm.onScreenVisible(); onPauseOrDispose { vm.onScreenHidden() } }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(s.mode.label) }, actions = {
                IconButton(onClick = vm::flip, modifier = Modifier.testTag("flip")) { Icon(Icons.Outlined.SwapVert, "Tahtayı çevir") }
                IconButton(onClick = { menu = true }, modifier = Modifier.testTag("menu")) { Icon(Icons.Outlined.MoreVert, "Menü") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Yeni oyun / analiz") }, { menu = false; newGame = true }, Modifier.testTag("m_new"))
                    DropdownMenuItem({ Text("Konum düzenleyici") }, { menu = false; onEditor() }, Modifier.testTag("m_editor"))
                    DropdownMenuItem({ Text("FEN göster / yükle") }, { menu = false; fenDialog = true }, Modifier.testTag("m_fen"))
                    DropdownMenuItem({ Text("Oyunu kaydet") }, { menu = false; saveDialog = true }, Modifier.testTag("m_save"))
                    DropdownMenuItem({ Text("Kayıtlı oyunlar") }, { menu = false; onSaved() }, Modifier.testTag("m_saved"))
                    DropdownMenuItem({ Text("Ayarlar") }, { menu = false; onSettings() })
                }
            })
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())) {
            val shown = s.previewFen?.let { (Fen.parse(it) as FenResult.Ok).position } ?: s.position
            val checkSq = if (s.previewFen == null && s.status?.inCheck == true)
                (0..63).map(::Sq).firstOrNull { shown.pieceAt(it) == ChessPiece(shown.sideToMove, PieceKind.KING) } else null
            ChessBoard(
                position = shown, flipped = s.flipped, selected = s.selected,
                targets = s.selected?.let { s.targetsFrom(it) } ?: emptyList(),
                lastMove = if (s.preview != null) s.preview!!.line.uci.getOrNull(s.preview!!.step - 1) else s.lastMove,
                checkSquare = checkSq, onTap = vm::tap,
            )
            Text(s.fen, Modifier.padding(horizontal = 8.dp).testTag("fen_text"), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            StatusRow(s, vm)
            if (s.preview != null) PreviewBar(s, vm)
            NavRow(s, vm)
            if (s.mode == GameMode.VS_COMPUTER) Text("Seviye: ${LEVELS[s.level].label}" + if (s.computerThinking) " · bilgisayar düşünüyor…" else "",
                Modifier.padding(horizontal = 12.dp).testTag("computer_state"), style = MaterialTheme.typography.bodySmall)
            AnalysisPanel(s, vm)
            MoveList(s, vm)
        }
    }

    if (s.pendingPromotion != null) PromotionDialog(s.sideToMove, vm)
    if (newGame) NewGameDialog(s.level, onDismiss = { newGame = false }) { mode, white, level -> newGame = false; vm.newGame(mode, white, level) }
    if (fenDialog) FenDialog(s.fen, onDismiss = { fenDialog = false }) { text -> vm.loadFen(text).also { if (it == null) fenDialog = false } }
    if (saveDialog) SaveDialog(onDismiss = { saveDialog = false }) { saveDialog = false; vm.save(it) }
}

@Composable
private fun StatusRow(s: GameUiState, vm: GameViewModel) {
    val st = s.status
    val text = when {
        s.claimedDraw != null -> "Berabere – ${s.claimedDraw} (talep)"
        st == null -> ""
        st.result == GameResult.WHITE_WINS -> "Mat – Beyaz kazandı"
        st.result == GameResult.BLACK_WINS -> "Mat – Siyah kazandı"
        st.result == GameResult.DRAW -> "Berabere – ${st.termination?.text}"
        else -> (if (s.sideToMove == Side.WHITE) "Sıra: Beyaz" else "Sıra: Siyah") + if (st.inCheck) " · Şah!" else ""
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f).testTag("status"), style = MaterialTheme.typography.titleSmall)
        if (!s.gameOver && s.atEnd) st?.claimable?.forEach { c ->
            TextButton(onClick = { vm.claimDraw(c) }, modifier = Modifier.testTag("claim")) { Text("Beraberlik talep et") }
        }
    }
}

@Composable
private fun NavRow(s: GameUiState, vm: GameViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(onClick = { vm.goTo(0) }, enabled = s.cursor > 0) { Icon(Icons.Filled.FirstPage, "Başa") }
        IconButton(onClick = vm::back, enabled = s.cursor > 0, modifier = Modifier.testTag("nav_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
        IconButton(onClick = vm::forward, enabled = !s.atEnd, modifier = Modifier.testTag("nav_forward")) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "İleri") }
        IconButton(onClick = { vm.goTo(s.moves.size) }, enabled = !s.atEnd) { Icon(Icons.Filled.LastPage, "Sona") }
    }
}

@Composable
private fun PreviewBar(s: GameUiState, vm: GameViewModel) {
    val p = s.preview ?: return
    Card(Modifier.fillMaxWidth().padding(8.dp).testTag("preview_bar"), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Varyant önizleme: ${p.step}/${p.line.uci.size} (oyun değişmez)", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            IconButton(onClick = { vm.previewStep(-1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Önceki") }
            IconButton(onClick = { vm.previewStep(1) }, modifier = Modifier.testTag("preview_next")) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Sonraki") }
            IconButton(onClick = vm::endPreview, modifier = Modifier.testTag("preview_close")) { Icon(Icons.Outlined.Close, "Önizlemeyi kapat") }
        }
    }
}

@Composable
private fun AnalysisPanel(s: GameUiState, vm: GameViewModel) {
    Card(Modifier.fillMaxWidth().padding(8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Motor analizi", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Switch(s.analysisOn, { vm.toggleAnalysis() }, Modifier.testTag("analysis_switch"))
            }
            val a = s.analysis
            when {
                !s.analysisOn -> Text("Kapalı", style = MaterialTheme.typography.bodySmall)
                s.gameOver -> Text("Oyun bitti.", style = MaterialTheme.typography.bodySmall)
                a == null -> Text("Hesaplanıyor…", style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("analysis_wait"))
                a.error != null -> Text(a.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("analysis_error"))
                else -> {
                    a.lines.forEach { l ->
                        Row(Modifier.fillMaxWidth().clickable { vm.startPreview(l) }.padding(vertical = 4.dp).testTag("pv_${l.rank}"), verticalAlignment = Alignment.CenterVertically) {
                            Text(l.scoreText, Modifier.width(64.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(numbered(s, l.san), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text("Derinlik ${a.depth}" + (a.nps?.let { " · ${it / 1000} kN/sn" } ?: "") + if (a.final) " · tamamlandı" else "",
                        style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("analysis_meta"))
                }
            }
        }
    }
}

/** SAN listesini hamle numaralarıyla yazar (konumdaki hamle numarası ve sıra dikkate alınır). */
private fun numbered(s: GameUiState, san: List<String>): String = buildString {
    var n = s.position.fullmoveNumber; var white = s.sideToMove == Side.WHITE
    san.forEachIndexed { i, m ->
        if (white) append("$n. ") else if (i == 0) append("$n… ")
        append(m).append(' ')
        if (!white) n++
        white = !white
    }
}.trim()

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoveList(s: GameUiState, vm: GameViewModel) {
    if (s.sanMoves.isEmpty()) return
    val startWhite = (Fen.parse(s.startFen) as FenResult.Ok).position.let { it.sideToMove == Side.WHITE }
    val startNo = (Fen.parse(s.startFen) as FenResult.Ok).position.fullmoveNumber
    FlowRow(Modifier.fillMaxWidth().padding(8.dp).testTag("move_list"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        s.sanMoves.forEachIndexed { i, m ->
            val ply = i + if (startWhite) 0 else 1
            val label = (if (ply % 2 == 0) "${startNo + ply / 2}. " else if (i == 0) "${startNo}… " else "") + m
            FilterChip(selected = s.cursor == i + 1, onClick = { vm.goTo(i + 1) }, label = { Text(label) })
        }
    }
}

@Composable
private fun PromotionDialog(side: Side, vm: GameViewModel) {
    AlertDialog(onDismissRequest = vm::cancelPromotion, title = { Text("Terfi") }, confirmButton = {}, dismissButton = { TextButton(onClick = vm::cancelPromotion) { Text("Vazgeç") } },
        text = {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                listOf(PieceKind.QUEEN, PieceKind.ROOK, PieceKind.BISHOP, PieceKind.KNIGHT).forEach { k ->
                    val p = ChessPiece(side, k)
                    Image(painterResource(pieceDrawable(p)), pieceName(p), Modifier.size(56.dp).clickable { vm.promote(k) }.testTag("promo_${k.fenLower}"))
                }
            }
        })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewGameDialog(level: Int, onDismiss: () -> Unit, onStart: (GameMode, Boolean, Int) -> Unit) {
    var mode by remember { mutableStateOf(GameMode.ANALYSIS) }
    var white by remember { mutableStateOf(true) }
    var lvl by remember { mutableStateOf(level) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Yeni") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GameMode.entries.forEach { m -> FilterChip(mode == m, { mode = m }, { Text(m.label) }, Modifier.testTag("mode_${m.name}")) }
                }
                if (mode == GameMode.VS_COMPUTER) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(white, { white = true }, { Text("Beyaz oyna") }); FilterChip(!white, { white = false }, { Text("Siyah oyna") }, Modifier.testTag("play_black"))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LEVELS.forEachIndexed { i, l -> AssistChip(onClick = { lvl = i }, label = { Text(if (i == lvl) "✓ ${l.label}" else l.label) }) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onStart(mode, white, lvl) }, modifier = Modifier.testTag("start_game")) { Text("Başla") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } })
}

@Composable
private fun FenDialog(current: String, onDismiss: () -> Unit, onLoad: (String) -> String?) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf(current) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("FEN") },
        text = {
            Column {
                OutlinedTextField(text, { text = it; error = null }, Modifier.fillMaxWidth().testTag("fen_input"), isError = error != null, minLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("fen_error")) }
                Spacer(Modifier.size(8.dp))
                TextButton(onClick = {
                    ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("FEN", current))
                }) { Text("Mevcut FEN'i kopyala") }
            }
        },
        confirmButton = { TextButton(onClick = { error = onLoad(text) }, modifier = Modifier.testTag("fen_load")) { Text("Yükle") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Kapat") } })
}

@Composable
private fun SaveDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var title by remember { mutableStateOf("Oyun ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.forLanguageTag("tr")).format(java.util.Date())}") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Oyunu kaydet") },
        text = { OutlinedTextField(title, { title = it.take(60) }, singleLine = true, modifier = Modifier.testTag("save_title")) },
        confirmButton = { TextButton(onClick = { onSave(title) }, modifier = Modifier.testTag("save_ok")) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } })
}
