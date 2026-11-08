package com.galaxyoffline.chess.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.galaxyoffline.chess.data.EngineHolder
import com.galaxyoffline.chess.data.EngineSettings
import com.galaxyoffline.chess.engine.AnalysisRequest
import com.galaxyoffline.chess.engine.AnalysisUpdate
import com.galaxyoffline.chess.engine.EngineSession
import com.galaxyoffline.chess.engine.Score
import com.galaxyoffline.chess.engine.SearchLimit
import com.galaxyoffline.core.designsystem.components.BackButton
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Motor testi: S25'te gerçek hız ölçümü. Sonuçlar panoya kopyalanıp geliştiriciye iletilir; iş parçacığı ve
 * bellek varsayılanları bu ölçümlere göre ayarlanacak. Ölçüm sırasında başka analiz yapılmaz.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineTestScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val holder = remember { EngineHolder.get(ctx) }
    var running by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf("") }
    var quick by remember { mutableStateOf(true) }
    Scaffold(topBar = { TopAppBar(title = { Text("Motor testi") }, navigationIcon = { BackButton(onBack) }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Bu test motorun bu cihazdaki hızını ölçer: ilk sonucun gelme süresi, 3 saniyede ulaşılan derinlik ve hız, " +
                "iş parçacığı sayısına göre ölçekleme ve mat bulmaları. Hızlı test yaklaşık 1,5 dakika, tam test yaklaşık 4 dakika sürer. " +
                "Test sırasında telefon ısınabilir.", style = MaterialTheme.typography.bodyMedium)
            Button(enabled = !running, onClick = {
                running = true; report = "Çalışıyor…"
                holder.withSession({ s -> if (s == null) "Motor başlatılamadı (${holder.state.value})." else runBenchmark(s, holder, quick) }) { r ->
                    report = r; running = false
                }
            }, modifier = Modifier.testTag("bench_start")) { Text(if (quick) "Hızlı testi başlat" else "Tam testi başlat") }
            OutlinedButton(enabled = !running, onClick = { quick = !quick }) { Text(if (quick) "Tam teste geç" else "Hızlı teste geç") }
            if (running) LinearProgressIndicator()
            if (report.isNotEmpty()) {
                Text(report, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("bench_report"))
                OutlinedButton(enabled = !running, onClick = {
                    ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Motor testi", report))
                }) { Text("Sonuçları kopyala") }
            }
        }
    }
}

private val BENCH_POSITIONS = listOf(
    "Başlangıç" to "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    "Orta oyun" to "r1bq1rk1/pp2bppp/2n1pn2/3p4/2PP4/2N1PN2/PP3PPP/R2QKB1R w KQ - 0 9",
    "Kiwipete" to "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
)
/** Bilinen matlar (mat sayısı, hamle sırası olan taraf için). */
private val MATE_POSITIONS = listOf(
    Triple("Mat 1", "6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1", 1),
    Triple("Mat 2", "r2qkb1r/pp2nppp/3p4/2pNN1B1/2BnP3/3P4/PPP2PPP/R2bK2R w KQkq - 1 1", 2),
    Triple("Mat 3", "r1b1kb1r/pppp1ppp/5q2/4n3/3KP3/2N3PN/PPP4P/R1BQ1B1R b kq - 0 1", 3),
)

/** Tek arama: (ilk sonuç ms, son güncelleme). */
private fun search(s: EngineSession, fen: String, limit: SearchLimit, multiPv: Int, until: ((AnalysisUpdate) -> Boolean)? = null): Pair<Long?, AnalysisUpdate?> {
    val done = CountDownLatch(1); var first: Long? = null; var last: AnalysisUpdate? = null
    val t0 = System.nanoTime()
    s.analyze(AnalysisRequest(fen, limit, multiPv)) { u ->
        if (first == null && u.lines.isNotEmpty()) first = (System.nanoTime() - t0) / 1_000_000
        last = u
        if (u.final || until?.invoke(u) == true) done.countDown()
    }
    done.await(30, TimeUnit.SECONDS)
    if (until != null) s.cancel()
    return first to last
}

private fun runBenchmark(s: EngineSession, holder: EngineHolder, quick: Boolean): String = buildString {
    val st = holder.settings.value
    appendLine("Cihaz: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}), çekirdek: ${EngineSettings.CORES}")
    appendLine("Motor: ${holder.engineName ?: "?"} · ayar: ${st.threads} iş parçacığı, ${st.hashMb} MB")
    s.setOption("UCI_LimitStrength", "false")
    appendLine("-- 3 sn analiz, MultiPV 3 --")
    for ((name, fen) in BENCH_POSITIONS.let { if (quick) it.take(2) else it }) {
        val (first, u) = search(s, fen, SearchLimit.Time(3_000), 3)
        val top = u?.lines?.firstOrNull()
        appendLine("$name: ilk sonuç ${first ?: "-"} ms · derinlik ${top?.depth ?: "-"} · ${(top?.nps ?: 0) / 1000} kN/sn · toplam ${u?.elapsedMs ?: "-"} ms${u?.error?.let { " · HATA $it" } ?: ""}")
    }
    appendLine("-- İş parçacığı ölçeklemesi (orta oyun, 3 sn, MultiPV 1) --")
    val threadsList = listOf(1, 2, 4, 6, EngineSettings.CORES - 1, EngineSettings.CORES).filter { it in 1..EngineSettings.CORES }.distinct().sorted()
    for (t in if (quick) threadsList.filter { it == 1 || it == st.threads || it == EngineSettings.CORES } else threadsList) {
        s.setOption("Threads", t.toString())
        val (_, u) = search(s, BENCH_POSITIONS[1].second, SearchLimit.Time(3_000), 1)
        val top = u?.lines?.firstOrNull()
        appendLine("$t iş parçacığı: ${(top?.nps ?: 0) / 1000} kN/sn · derinlik ${top?.depth ?: "-"}")
    }
    s.setOption("Threads", st.threads.toString())
    appendLine("-- Mat bulma (en fazla 3 sn) --")
    for ((name, fen, n) in MATE_POSITIONS) {
        val t0 = System.nanoTime()
        val (_, u) = search(s, fen, SearchLimit.Time(3_000), 1) { x -> (x.lines.firstOrNull()?.score as? Score.Mate)?.moves == n }
        val ms = (System.nanoTime() - t0) / 1_000_000
        val sc = u?.lines?.firstOrNull()?.score
        appendLine("$name: ${if ((sc as? Score.Mate)?.moves == n) "bulundu, $ms ms" else "3 sn içinde bulunamadı (puan: $sc)"}")
    }
}
