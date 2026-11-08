package com.galaxyoffline.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.diary.data.Mood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(vm: DiaryListViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val st = s.stats
    Scaffold(topBar = { TopAppBar(title = { Text("İstatistikler") }, navigationIcon = { BackButton(onBack) }) }) { p ->
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Toplam kayıt", "${st.total}", Modifier.weight(1f))
                Stat("Bu ay", "${st.thisMonth}", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Güncel seri", "${st.currentStreak} gün", Modifier.weight(1f))
                Stat("En uzun seri", "${st.longestStreak} gün", Modifier.weight(1f))
            }
            Stat("Ortalama ruh hali", st.averageMood?.let { avg -> "${Mood.of(Math.round(avg).toInt()).emoji} ${TrFormat.number(avg, 1)} / 5" } ?: "—",
                Modifier.fillMaxWidth())
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ruh hali dağılımı", style = MaterialTheme.typography.titleSmall)
                    val max = (st.moodCounts.values.maxOrNull() ?: 0).coerceAtLeast(1)
                    Mood.entries.reversed().forEach { m ->
                        val c = st.moodCounts[m] ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(m.emoji, modifier = Modifier.width(32.dp))
                            Text(m.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp))
                            Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                                Box(Modifier.fillMaxWidth(c.toFloat() / max).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("$c", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
