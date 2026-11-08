package com.galaxyoffline.chess.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxyoffline.chess.R
import com.galaxyoffline.chess.rules.ChessPiece
import com.galaxyoffline.chess.rules.Color as Side
import com.galaxyoffline.chess.rules.FenPosition
import com.galaxyoffline.chess.rules.PieceKind
import com.galaxyoffline.chess.rules.Sq

private val LIGHT = Color(0xFFF0D9B5)
private val DARK = Color(0xFFB58863)
private val SELECT = Color(0x99F6F669)
private val LAST = Color(0x66CDD26A)
private val CHECK = Color(0xCCE53935)

fun pieceDrawable(p: ChessPiece): Int = when (p.color) {
    Side.WHITE -> when (p.kind) { PieceKind.KING -> R.drawable.piece_wk; PieceKind.QUEEN -> R.drawable.piece_wq; PieceKind.ROOK -> R.drawable.piece_wr
        PieceKind.BISHOP -> R.drawable.piece_wb; PieceKind.KNIGHT -> R.drawable.piece_wn; PieceKind.PAWN -> R.drawable.piece_wp }
    Side.BLACK -> when (p.kind) { PieceKind.KING -> R.drawable.piece_bk; PieceKind.QUEEN -> R.drawable.piece_bq; PieceKind.ROOK -> R.drawable.piece_br
        PieceKind.BISHOP -> R.drawable.piece_bb; PieceKind.KNIGHT -> R.drawable.piece_bn; PieceKind.PAWN -> R.drawable.piece_bp }
}

fun pieceName(p: ChessPiece): String = (if (p.color == Side.WHITE) "beyaz " else "siyah ") + when (p.kind) {
    PieceKind.KING -> "şah"; PieceKind.QUEEN -> "vezir"; PieceKind.ROOK -> "kale"; PieceKind.BISHOP -> "fil"; PieceKind.KNIGHT -> "at"; PieceKind.PAWN -> "piyon"
}

/**
 * Satranç tahtası. [flipped]: siyah aşağıda. Her kare `sq_<ad>` test etiketi ve erişilebilirlik açıklaması taşır.
 */
@Composable
fun ChessBoard(
    position: FenPosition,
    flipped: Boolean,
    selected: Sq?,
    targets: List<Sq>,
    lastMove: String?,
    checkSquare: Sq?,
    onTap: (Sq) -> Unit,
    modifier: Modifier = Modifier,
) {
    val last = lastMove?.let { listOfNotNull(Sq.parse(it.substring(0, 2)), Sq.parse(it.substring(2, 4))) } ?: emptyList()
    Column(modifier.fillMaxWidth().aspectRatio(1f).border(1.dp, DARK)) {
        for (row in 0..7) {
            Row(Modifier.weight(1f)) {
                for (col in 0..7) {
                    val file = if (flipped) 7 - col else col
                    val rank = if (flipped) row else 7 - row
                    val sq = Sq.of(file, rank)
                    val piece = position.pieceAt(sq)
                    val base = if ((file + rank) % 2 == 0) DARK else LIGHT
                    Box(
                        Modifier.weight(1f).fillMaxSize().background(base)
                            .background(when { sq == selected -> SELECT; sq in last -> LAST; else -> Color.Transparent })
                            .then(if (sq == checkSquare) Modifier.background(CHECK.copy(alpha = 0.55f)) else Modifier)
                            .clickable { onTap(sq) }
                            .semantics { contentDescription = sq.name + ": " + (piece?.let(::pieceName) ?: "boş") }
                            .testTag("sq_${sq.name}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (piece != null) Image(painterResource(pieceDrawable(piece)), null, Modifier.fillMaxSize().padding(2.dp))
                        if (sq in targets) {
                            Box(Modifier.fillMaxSize(if (piece == null) 0.3f else 0.9f).clip(CircleShape)
                                .background(if (piece == null) Color(0x55000000) else Color.Transparent)
                                .then(if (piece != null) Modifier.border(3.dp, Color(0x77000000), CircleShape) else Modifier))
                        }
                        if (col == 0) Text("${rank + 1}", Modifier.align(Alignment.TopStart).padding(1.dp), fontSize = 9.sp, color = if (base == DARK) LIGHT else DARK)
                        if (row == 7) Text("${'a' + file}", Modifier.align(Alignment.BottomEnd).padding(1.dp), fontSize = 9.sp, color = if (base == DARK) LIGHT else DARK)
                    }
                }
            }
        }
    }
}
