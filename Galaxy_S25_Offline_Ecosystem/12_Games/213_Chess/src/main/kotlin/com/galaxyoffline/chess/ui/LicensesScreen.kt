package com.galaxyoffline.chess.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.galaxyoffline.chess.R
import com.galaxyoffline.core.designsystem.components.BackButton

/** Açık kaynak lisansları: GPL-3 (uygulama + Stockfish), Apache-2.0 (chesslib, commons-lang3 ve NOTICE), cburnett (GPLv2+). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val text = remember {
        listOf(R.raw.notices, R.raw.commons_lang_notice, R.raw.gpl3, R.raw.apache2).joinToString("\n\n──────────\n\n") { id ->
            ctx.resources.openRawResource(id).bufferedReader().use { it.readText() }
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Açık kaynak lisansları") }, navigationIcon = { BackButton(onBack) }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("licenses_text"))
        }
    }
}
