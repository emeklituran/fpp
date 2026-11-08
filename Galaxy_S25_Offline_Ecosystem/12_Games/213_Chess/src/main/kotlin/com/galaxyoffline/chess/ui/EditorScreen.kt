package com.galaxyoffline.chess.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.galaxyoffline.chess.data.GameMode
import com.galaxyoffline.chess.rules.CastlingRights
import com.galaxyoffline.chess.rules.ChessPiece
import com.galaxyoffline.chess.rules.Color as Side
import com.galaxyoffline.chess.rules.Fen
import com.galaxyoffline.chess.rules.FenPosition
import com.galaxyoffline.chess.rules.FenResult
import com.galaxyoffline.chess.rules.PieceKind
import com.galaxyoffline.chess.rules.Sq
import com.galaxyoffline.core.designsystem.components.BackButton

/**
 * Konum düzenleyici: taşları istediğiniz kareye koyun. Konum, analize gönderilmeden önce
 * PositionValidator ile denetlenir; geçersizse nedenleri Türkçe gösterilir.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(vm: GameViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val initial = vm.state.value.position
    val board = remember { mutableStateListOf<ChessPiece?>().apply { addAll(initial.board) } }
    var brush by remember { mutableStateOf<ChessPiece?>(ChessPiece(Side.WHITE, PieceKind.QUEEN)) }
    var side by remember { mutableStateOf(initial.sideToMove) }
    var wk by remember { mutableStateOf(initial.castling.whiteKing) }; var wq by remember { mutableStateOf(initial.castling.whiteQueen) }
    var bk by remember { mutableStateOf(initial.castling.blackKing) }; var bq by remember { mutableStateOf(initial.castling.blackQueen) }
    var ep by remember { mutableStateOf<Sq?>(initial.enPassant) }
    var problems by remember { mutableStateOf<List<String>>(emptyList()) }

    fun current() = FenPosition(board.toList(), side, CastlingRights(wk, wq, bk, bq), ep, 0, 1)
    fun submit(mode: GameMode) {
        val err = vm.loadFen(Fen.write(current()), mode)
        if (err == null) onDone() else problems = err.removePrefix("Konum geçersiz – ").split(". ").filter { it.isNotBlank() }
    }
    // Geçerken alma için olası kareler: hamle sırası olan tarafa göre 6. (beyaz) ya da 3. (siyah) sıra.
    val epCandidates = (0..7).map { Sq.of(it, if (side == Side.WHITE) 5 else 2) }

    Scaffold(topBar = { TopAppBar(title = { Text("Konum düzenleyici") }, navigationIcon = { BackButton(onBack) }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChessBoard(position = current(), flipped = false, selected = null, targets = emptyList(), lastMove = null, checkSquare = null,
                onTap = { sq -> board[sq.index] = if (board[sq.index] == brush) null else brush; problems = emptyList() })
            Text("Yerleştirilecek taş (aynı taşa tekrar dokunmak kaldırır):", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in Side.entries) for (k in PieceKind.entries.reversed()) {
                    val p = ChessPiece(c, k)
                    Box(Modifier.size(44.dp).border(if (brush == p) 3.dp else 1.dp, if (brush == p) MaterialTheme.colorScheme.primary else Color.Gray)
                        .background(Color(0xFFE8D9C0)).clickable { brush = p }.testTag("brush_${p.fen}"), contentAlignment = Alignment.Center) {
                        Image(painterResource(pieceDrawable(p)), pieceName(p), Modifier.fillMaxSize().padding(2.dp))
                    }
                }
                Box(Modifier.size(44.dp).border(if (brush == null) 3.dp else 1.dp, Color.Gray).clickable { brush = null }.testTag("brush_erase"), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Delete, "Silgi")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(side == Side.WHITE, { side = Side.WHITE; ep = null }, { Text("Sıra beyazda") })
                FilterChip(side == Side.BLACK, { side = Side.BLACK; ep = null }, { Text("Sıra siyahta") }, Modifier.testTag("side_black"))
            }
            Text("Rok hakları", style = MaterialTheme.typography.labelLarge)
            FlowRow {
                listOf(Triple("Beyaz O-O", wk) { v: Boolean -> wk = v }, Triple("Beyaz O-O-O", wq) { v: Boolean -> wq = v },
                    Triple("Siyah O-O", bk) { v: Boolean -> bk = v }, Triple("Siyah O-O-O", bq) { v: Boolean -> bq = v }).forEach { (label, value, set) ->
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(value, { set(it) }); Text(label) }
                }
            }
            Text("Geçerken alma karesi", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(ep == null, { ep = null }, { Text("Yok") })
                epCandidates.forEach { sq -> FilterChip(ep == sq, { ep = sq }, { Text(sq.name) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { board.indices.forEach { board[it] = null }; wk = false; wq = false; bk = false; bq = false; ep = null }, modifier = Modifier.testTag("editor_clear")) { Text("Temizle") }
                OutlinedButton(onClick = {
                    val st = (Fen.parse(Fen.START) as FenResult.Ok).position
                    board.indices.forEach { board[it] = st.board[it] }; side = Side.WHITE; wk = true; wq = true; bk = true; bq = true; ep = null
                }) { Text("Başlangıç") }
            }
            if (problems.isNotEmpty()) Column(Modifier.testTag("editor_problems")) {
                problems.forEach { Text("• $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { submit(GameMode.ANALYSIS) }, modifier = Modifier.testTag("editor_analyze")) { Text("Analiz et") }
                OutlinedButton(onClick = { submit(GameMode.TWO_PLAYER) }) { Text("Buradan oyna") }
            }
            Text(Fen.write(current()), style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("editor_fen"))
        }
    }
}
