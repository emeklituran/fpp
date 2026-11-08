package com.galaxyoffline.chess.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.galaxyoffline.chess.data.ActiveGame
import com.galaxyoffline.chess.data.ActiveGameStore
import com.galaxyoffline.chess.data.ChessDatabase
import com.galaxyoffline.chess.data.EngineHolder
import com.galaxyoffline.chess.data.GameMode
import com.galaxyoffline.chess.data.LEVELS
import com.galaxyoffline.chess.data.SavedGame
import com.galaxyoffline.chess.engine.AnalysisUpdate
import com.galaxyoffline.chess.engine.Score
import com.galaxyoffline.chess.engine.SearchLimit
import com.galaxyoffline.chess.rules.Color
import com.galaxyoffline.chess.rules.Fen
import com.galaxyoffline.chess.rules.FenPosition
import com.galaxyoffline.chess.rules.Game
import com.galaxyoffline.chess.rules.GameLoad
import com.galaxyoffline.chess.rules.GameResult
import com.galaxyoffline.chess.rules.GameStatus
import com.galaxyoffline.chess.rules.PieceKind
import com.galaxyoffline.chess.rules.Sq
import com.galaxyoffline.chess.rules.Termination
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Ekrandaki tek analiz satırı (puan beyazın bakış açısından). */
data class LineView(val rank: Int, val scoreText: String, val score: Score, val depth: Int, val uci: List<String>, val san: List<String>)
data class AnalysisView(val fen: String, val lines: List<LineView>, val final: Boolean, val error: String?, val depth: Int, val nps: Long?)
/** Varyant önizlemesi: asıl oyun değişmez; [step] önizlenen yarım hamle sayısı. */
data class Preview(val line: LineView, val step: Int)

data class GameUiState(
    val startFen: String = Fen.START,
    val moves: List<String> = emptyList(),
    val cursor: Int = 0,
    val sanMoves: List<String> = emptyList(),
    val position: FenPosition = (com.galaxyoffline.chess.rules.Fen.parse(Fen.START) as com.galaxyoffline.chess.rules.FenResult.Ok).position,
    val fen: String = Fen.START,
    val status: GameStatus? = null,
    val legalMoves: List<String> = emptyList(),
    val mode: GameMode = GameMode.ANALYSIS,
    val userIsWhite: Boolean = true,
    val level: Int = 4,
    val flipped: Boolean = false,
    val selected: Sq? = null,
    val pendingPromotion: Pair<Sq, Sq>? = null,
    val analysisOn: Boolean = true,
    val analysis: AnalysisView? = null,
    val preview: Preview? = null,
    val previewFen: String? = null,
    val computerThinking: Boolean = false,
    val claimedDraw: String? = null,
    val savedId: Long? = null,
) {
    val sideToMove get() = position.sideToMove
    val userTurn get() = mode != GameMode.VS_COMPUTER || (sideToMove == Color.WHITE) == userIsWhite
    val gameOver get() = claimedDraw != null || status?.result?.let { it != GameResult.ONGOING } == true
    val atEnd get() = cursor == moves.size
    val lastMove get() = moves.getOrNull(cursor - 1)
    fun targetsFrom(sq: Sq) = legalMoves.filter { it.startsWith(sq.name) }.map { Sq.parse(it.substring(2, 4))!! }.distinct()
}

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val engine = EngineHolder.get(app)
    private val store = ActiveGameStore(app)
    private val dao = ChessDatabase.get(app).games()
    val messages = UiMessages()
    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()
    /** Cursor konumundaki oyun (geri/ileri gezinmede yeniden kurulur). */
    private var game: Game = Game.standard()
    private var requestedFen: String? = null

    init {
        val a = store.load()
        if (a != null && Game.replay(a.startFen, a.moves) != null) {
            _state.value = GameUiState(startFen = a.startFen, moves = a.moves, cursor = a.cursor.coerceIn(0, a.moves.size), mode = a.mode,
                userIsWhite = a.userIsWhite, level = a.level, flipped = a.flipped, analysisOn = a.analysisOn, claimedDraw = a.claimedDraw, savedId = a.savedId)
        }
        rebuild()
    }

    // --- dışarıdan çağrılanlar ---

    fun newGame(mode: GameMode, userIsWhite: Boolean = true, level: Int = _state.value.level, startFen: String = Fen.START) {
        _state.value = GameUiState(startFen = startFen, mode = mode, userIsWhite = userIsWhite, level = level,
            flipped = mode == GameMode.VS_COMPUTER && !userIsWhite, analysisOn = mode == GameMode.ANALYSIS)
        rebuild()
    }

    /** Doğrulanmış FEN'den yeni analiz/oyun; hata mesajı (Türkçe) ya da null döner. */
    fun loadFen(text: String, mode: GameMode = GameMode.ANALYSIS): String? = when (val r = Game.fromFen(text)) {
        is GameLoad.InvalidFen -> "FEN hatalı – ${r.error}"
        is GameLoad.IllegalPosition -> "Konum geçersiz – " + r.problems.joinToString(" ")
        is GameLoad.Ok -> { newGame(mode, startFen = r.game.fen()); null }
    }

    fun loadSaved(g: SavedGame) {
        if (Game.replay(g.startFen, g.moveList) == null) { messages.post("Kayıt okunamadı (geçersiz hamle)."); return }
        _state.value = GameUiState(startFen = g.startFen, moves = g.moveList, cursor = g.moveList.size,
            mode = runCatching { GameMode.valueOf(g.mode) }.getOrDefault(GameMode.ANALYSIS), analysisOn = true, savedId = g.id)
        rebuild()
    }

    fun tap(sq: Sq) {
        val s = _state.value
        if (s.preview != null || s.gameOver || !s.userTurn || s.computerThinking) return
        val sel = s.selected
        if (sel != null && sq in s.targetsFrom(sel)) {
            val base = sel.name + sq.name
            if (s.legalMoves.any { it.length == 5 && it.startsWith(base) }) { _state.value = s.copy(pendingPromotion = sel to sq); return }
            play(base); return
        }
        val piece = s.position.pieceAt(sq)
        _state.value = s.copy(selected = if (piece != null && piece.color == s.sideToMove && sq != sel) sq else null)
    }

    fun promote(kind: PieceKind) {
        val (from, to) = _state.value.pendingPromotion ?: return
        _state.value = _state.value.copy(pendingPromotion = null)
        play(from.name + to.name + kind.fenLower)
    }
    fun cancelPromotion() { _state.value = _state.value.copy(pendingPromotion = null, selected = null) }

    /** UCI hamlesi oynar; geçmişin ortasındaysa sonraki hamleler atılır (yeni dal). */
    fun play(uci: String): Boolean {
        val s = _state.value
        if (uci !in s.legalMoves) return false
        _state.value = s.copy(moves = s.moves.take(s.cursor) + uci, cursor = s.cursor + 1, selected = null, claimedDraw = null)
        rebuild()
        return true
    }

    fun goTo(cursor: Int) {
        val s = _state.value
        if (s.computerThinking) engine.cancel()
        _state.value = s.copy(cursor = cursor.coerceIn(0, s.moves.size), selected = null, preview = null, previewFen = null, computerThinking = false)
        rebuild()
    }
    fun back() = goTo(_state.value.cursor - 1)
    fun forward() = goTo(_state.value.cursor + 1)
    fun flip() { _state.value = _state.value.copy(flipped = !_state.value.flipped); persist() }

    fun toggleAnalysis() {
        _state.value = _state.value.copy(analysisOn = !_state.value.analysisOn, analysis = null)
        if (!_state.value.analysisOn) { engine.cancel(); requestedFen = null }
        persist(); requestEngine()
    }

    fun setLevel(level: Int) { _state.value = _state.value.copy(level = level.coerceIn(0, LEVELS.lastIndex)); persist() }

    fun claimDraw(t: Termination) {
        if (t !in (_state.value.status?.claimable ?: emptyList())) return
        engine.cancel()
        _state.value = _state.value.copy(claimedDraw = t.text.substringBefore(" –"), computerThinking = false)
        persist()
    }

    fun startPreview(line: LineView) {
        _state.value = _state.value.copy(preview = Preview(line, 1), selected = null)
        updatePreviewFen()
    }
    fun previewStep(delta: Int) {
        val p = _state.value.preview ?: return
        _state.value = _state.value.copy(preview = p.copy(step = (p.step + delta).coerceIn(0, p.line.uci.size)))
        updatePreviewFen()
    }
    fun endPreview() { _state.value = _state.value.copy(preview = null, previewFen = null) }

    fun save(title: String) = launchSafe(messages, "Oyun kaydedilemedi") {
        val s = _state.value
        val result = s.claimedDraw ?: s.status?.termination?.text ?: "Devam ediyor"
        val id = dao.upsert(SavedGame(id = s.savedId ?: 0, title = title.ifBlank { "Oyun" }, startFen = s.startFen,
            moves = s.moves.joinToString(" "), mode = s.mode.name, result = result, updatedAt = System.currentTimeMillis()))
        _state.value = _state.value.copy(savedId = if (s.savedId == null) id else s.savedId); persist()
        messages.post("Oyun kaydedildi")
    }

    fun onScreenVisible() = requestEngine()
    fun onScreenHidden() { engine.cancel(); requestedFen = null; _state.value = _state.value.copy(computerThinking = false) }

    // --- iç işleyiş ---

    private fun rebuild() {
        val s = _state.value
        game = Game.replay(s.startFen, s.moves.take(s.cursor)) ?: Game.standard()
        val g0 = Game.replay(s.startFen, emptyList()) ?: Game.standard()
        val san = s.moves.map { m -> val x = g0.san(m) ?: m; g0.play(m); x }
        val pos = game.position()
        _state.value = s.copy(position = pos, fen = Fen.write(pos), status = game.status(), legalMoves = game.legalMoves(), sanMoves = san,
            analysis = s.analysis?.takeIf { it.fen == Fen.write(pos) }, preview = null, previewFen = null)
        persist()
        requestEngine()
    }

    private fun persist() {
        val s = _state.value
        store.save(ActiveGame(s.startFen, s.moves, s.cursor, s.mode, s.userIsWhite, s.level, s.flipped, s.analysisOn, s.claimedDraw, s.savedId))
    }

    private fun requestEngine() {
        val s = _state.value
        if (s.gameOver) { engine.cancel(); requestedFen = null; return }
        val computerTurn = s.mode == GameMode.VS_COMPUTER && !s.userTurn && s.atEnd
        when {
            computerTurn -> {
                if (s.computerThinking && requestedFen == s.fen) return
                val lvl = LEVELS[s.level.coerceIn(0, LEVELS.lastIndex)]
                requestedFen = s.fen
                _state.value = s.copy(computerThinking = true, analysis = null)
                engine.analyze(s.fen, SearchLimit.Time(lvl.moveMs), multiPv = 1, strengthElo = lvl.elo) { u -> onComputerUpdate(u) }
            }
            s.analysisOn -> {
                if (requestedFen == s.fen && s.analysis != null) return
                requestedFen = s.fen
                val ms = engine.settings.value.analysisMs
                engine.analyze(s.fen, if (ms <= 0) SearchLimit.Infinite else SearchLimit.Time(ms), multiPv = 3) { u -> onAnalysisUpdate(u) }
            }
            else -> { engine.cancel(); requestedFen = null }
        }
    }

    private fun onAnalysisUpdate(u: AnalysisUpdate) {
        val s = _state.value
        // Eski konuma ait sonuç asla gösterilmez (oturum zaten iptal eder; bu ikinci güvence).
        if (u.fen != s.fen || !s.analysisOn) return
        val white = s.sideToMove == Color.WHITE
        val lines = u.lines.map { l ->
            val sc = l.score.forWhite(white)
            LineView(l.multipv, formatScore(sc), sc, l.depth, l.pv, game.sanLine(l.pv.take(12)))
        }
        _state.value = s.copy(analysis = AnalysisView(u.fen, lines, u.final, u.error, u.lines.maxOfOrNull { it.depth } ?: 0, u.lines.firstOrNull()?.nps))
    }

    private fun onComputerUpdate(u: AnalysisUpdate) {
        val s = _state.value
        if (!u.final || u.fen != s.fen || !s.computerThinking) return
        _state.value = s.copy(computerThinking = false)
        if (u.error != null) { messages.post(u.error); return }
        // Bilgisayar, kendisi için değerlendirme ≤ 0 ise talep edilebilir beraberliği talep eder.
        val myScore = u.lines.firstOrNull()?.score
        val claim = s.status?.claimable?.firstOrNull()
        if (claim != null && myScore != null && myScore.sortValue() <= 0) { claimDraw(claim); messages.post("Bilgisayar beraberlik talep etti: ${claim.text.substringBefore(" –")}"); return }
        val mv = u.bestMove ?: return
        if (!play(mv)) messages.post("Motor geçersiz hamle önerdi: $mv")
    }

    private fun updatePreviewFen() {
        val p = _state.value.preview ?: return
        val g = game.copy()
        p.line.uci.take(p.step).forEach { if (!g.play(it)) return@forEach }
        _state.value = _state.value.copy(previewFen = g.fen())
    }

    override fun onCleared() { engine.cancel() }

    companion object {
        fun formatScore(s: Score): String = when (s) {
            is Score.Cp -> (if (s.centipawns >= 0) "+" else "−") + "%.2f".format(java.util.Locale.ROOT, kotlin.math.abs(s.centipawns) / 100.0)
            is Score.Mate -> if (s.moves > 0) "M${s.moves}" else if (s.moves < 0) "−M${-s.moves}" else "M0"
        }
    }
}
