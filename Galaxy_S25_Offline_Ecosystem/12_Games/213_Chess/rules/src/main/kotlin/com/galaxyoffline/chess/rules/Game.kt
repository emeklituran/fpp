package com.galaxyoffline.chess.rules

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList

/**
 * Bir oyunun durumu. Hamle üretimi chesslib'e, kurallar (beraberlik, tekrar, doğrulama) bu modüle aittir.
 * Yalnızca [PositionValidator]'dan geçen konumlarla oluşturulabilir.
 */
class Game private constructor(val start: FenPosition) {
    private val board: Board = Chesslib.board(start)
    private val moves = mutableListOf<Move>()
    private val keys = mutableListOf(Equality.keyOf(start))

    val moveCount get() = moves.size

    /** Mevcut konum (geçerken alma alanı, iki kare piyon hamlesinden sonra her zaman yazılır – standart FEN). */
    fun position(): FenPosition = (Fen.parse(board.getFen(true)) as FenResult.Ok).position
    fun fen(): String = Fen.write(position())

    /** Yasal hamleler UCI biçiminde (örn. e2e4, e7e8q). */
    fun legalMoves(): List<String> = board.legalMoves().map(::uci)

    /** UCI hamlesini oynar; yasal değilse false. */
    fun play(uciMove: String): Boolean {
        val m = board.legalMoves().firstOrNull { uci(it) == uciMove.lowercase() } ?: return false
        board.doMove(m); moves += m; keys += currentKey()
        return true
    }

    /** SAN hamlesini oynar (+, #, !, ? işaretleri yok sayılır); yasal ve tek anlamlı değilse false. */
    fun playSan(san: String): Boolean {
        val clean = normalizeSan(san)
        val matches = board.legalMoves().filter { normalizeSan(sanOf(it)) == clean }
        if (matches.size != 1) return false
        return play(uci(matches[0]))
    }

    fun undo(): Boolean {
        if (moves.isEmpty()) return false
        board.undoMove(); moves.removeAt(moves.lastIndex); keys.removeAt(keys.lastIndex)
        return true
    }

    /** Mevcut konumdaki hamlenin SAN gösterimi. */
    fun san(uciMove: String): String? = board.legalMoves().firstOrNull { uci(it) == uciMove.lowercase() }?.let(::sanOf)

    fun history(): List<String> = moves.map(::uci)

    /** Mevcut konumun oyunda kaç kez oluştuğu (başlangıç dahil, FIDE konum kimliğiyle). */
    fun repetitionCount(): Int = keys.count { it == keys.last() }

    fun status(): GameStatus {
        val pos = position()
        val inCheck = board.isKingAttacked
        val hasMoves = board.legalMoves().isNotEmpty()
        val mover = board.sideToMove
        // Sıra: mat/pat önce (75 hamle kuralında "son hamle mat ise mat geçerli" – FIDE 9.6.2).
        if (!hasMoves) {
            return if (inCheck) GameStatus(if (mover == com.github.bhlangonijr.chesslib.Side.WHITE) GameResult.BLACK_WINS else GameResult.WHITE_WINS, Termination.CHECKMATE, emptyList(), true)
            else GameStatus(GameResult.DRAW, Termination.STALEMATE, emptyList(), false)
        }
        if (repetitionCount() >= 5) return GameStatus(GameResult.DRAW, Termination.FIVEFOLD_REPETITION, emptyList(), inCheck)
        if (pos.halfmoveClock >= 150) return GameStatus(GameResult.DRAW, Termination.SEVENTY_FIVE_MOVES, emptyList(), inCheck)
        if (DeadPosition.isDead(pos)) return GameStatus(GameResult.DRAW, Termination.DEAD_POSITION, emptyList(), inCheck)
        val claims = buildList {
            if (repetitionCount() >= 3) add(Termination.THREEFOLD_CLAIMABLE)
            if (pos.halfmoveClock >= 100) add(Termination.FIFTY_MOVES_CLAIMABLE)
        }
        return GameStatus(GameResult.ONGOING, null, claims, inCheck)
    }

    private fun currentKey() = Equality.keyOf(position())

    private fun sanOf(m: Move): String = MoveList(board.getFen(true)).apply { add(m) }.toSanArray()[0]

    /** Aynı başlangıç ve hamlelerle bağımsız bir kopya (varyant önizleme gibi işlemler asıl oyunu değiştirmez). */
    fun copy(): Game = replay(Fen.write(start), history())!!

    /** Verilen UCI hamle dizisinin mevcut konumdan SAN gösterimi; yasal olmayan ilk hamlede durur. Asıl oyun değişmez. */
    fun sanLine(uciMoves: List<String>): List<String> {
        val g = copy(); val out = mutableListOf<String>()
        for (m in uciMoves) { val san = g.san(m) ?: break; if (!g.play(m)) break; out += san }
        return out
    }

    companion object {
        fun standard(): Game = (fromFen(Fen.START) as GameLoad.Ok).game

        /** Başlangıç FEN'i ve UCI hamlelerinden oyunu yeniden kurar; geçersiz FEN veya yasal olmayan hamlede null. */
        fun replay(startFen: String, uciMoves: List<String>): Game? {
            val g = (fromFen(startFen) as? GameLoad.Ok)?.game ?: return null
            for (m in uciMoves) if (!g.play(m)) return null
            return g
        }

        fun fromFen(text: String): GameLoad = when (val r = Fen.parse(text)) {
            is FenResult.Error -> GameLoad.InvalidFen(r.error)
            is FenResult.Ok -> {
                val problems = PositionValidator.validate(r.position)
                if (problems.isNotEmpty()) GameLoad.IllegalPosition(problems) else GameLoad.Ok(Game(r.position))
            }
        }

        internal fun uci(m: Move): String = buildString {
            append(m.from.value().lowercase()); append(m.to.value().lowercase())
            if (m.promotion != com.github.bhlangonijr.chesslib.Piece.NONE) append(m.promotion.fenSymbol.lowercase())
        }

        private fun normalizeSan(s: String) = s.trim().trimEnd('+', '#', '!', '?').replace("0-0-0", "O-O-O").replace("0-0", "O-O")
    }
}

sealed interface GameLoad {
    data class Ok(val game: Game) : GameLoad
    data class InvalidFen(val error: FenError) : GameLoad
    data class IllegalPosition(val problems: List<String>) : GameLoad
}
