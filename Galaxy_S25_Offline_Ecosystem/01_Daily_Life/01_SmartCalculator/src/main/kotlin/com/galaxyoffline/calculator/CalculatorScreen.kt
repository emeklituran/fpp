package com.galaxyoffline.calculator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.calculator.CalcDisplay
import com.galaxyoffline.core.calculator.CalcKey
import com.galaxyoffline.core.calculator.CalculatorViewModel
import com.galaxyoffline.core.calculator.Key
import com.galaxyoffline.core.calculator.KeyRow
import com.galaxyoffline.core.calculator.KeyStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(vm: CalculatorViewModel, onOpenHistory: () -> Unit, onOpenSettings: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    val h = state.haptics
    val key: (Key) -> Unit = vm::onKey

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hesap Makinesi") },
                actions = {
                    IconButton(onClick = onOpenHistory) { Icon(Icons.Outlined.History, contentDescription = "Geçmiş") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Ayarlar") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().navigationBarsPadding()) {
            CalcDisplay(state, Modifier.weight(0.9f))
            Column(Modifier.weight(2.1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                KeyRow(0.55f) {
                    CalcKey("MC", KeyStyle.Function, h, { key(Key.MemoryClear) }, small = true)
                    CalcKey("MR", KeyStyle.Function, h, { key(Key.MemoryRecall) }, small = true)
                    CalcKey("M+", KeyStyle.Function, h, { key(Key.MemoryAdd) }, small = true)
                    CalcKey("M−", KeyStyle.Function, h, { key(Key.MemorySubtract) }, small = true)
                }
                KeyRow {
                    CalcKey("AC", KeyStyle.Danger, h, { key(Key.Clear) })
                    CalcKey("( )", KeyStyle.Operator, h, { key(Key.SmartParen) })
                    CalcKey("%", KeyStyle.Operator, h, { key(Key.Postfix('%')) })
                    CalcKey("÷", KeyStyle.Operator, h, { key(Key.Op('÷')) })
                }
                KeyRow {
                    CalcKey("7", KeyStyle.Number, h, { key(Key.Digit('7')) })
                    CalcKey("8", KeyStyle.Number, h, { key(Key.Digit('8')) })
                    CalcKey("9", KeyStyle.Number, h, { key(Key.Digit('9')) })
                    CalcKey("×", KeyStyle.Operator, h, { key(Key.Op('×')) })
                }
                KeyRow {
                    CalcKey("4", KeyStyle.Number, h, { key(Key.Digit('4')) })
                    CalcKey("5", KeyStyle.Number, h, { key(Key.Digit('5')) })
                    CalcKey("6", KeyStyle.Number, h, { key(Key.Digit('6')) })
                    CalcKey("−", KeyStyle.Operator, h, { key(Key.Op('−')) })
                }
                KeyRow {
                    CalcKey("1", KeyStyle.Number, h, { key(Key.Digit('1')) })
                    CalcKey("2", KeyStyle.Number, h, { key(Key.Digit('2')) })
                    CalcKey("3", KeyStyle.Number, h, { key(Key.Digit('3')) })
                    CalcKey("+", KeyStyle.Operator, h, { key(Key.Op('+')) })
                }
                KeyRow {
                    CalcKey("±", KeyStyle.Number, h, { key(Key.Sign) })
                    CalcKey("0", KeyStyle.Number, h, { key(Key.Digit('0')) })
                    CalcKey(",", KeyStyle.Number, h, { key(Key.Decimal) })
                    CalcKey("⌫", KeyStyle.Number, h, { key(Key.Backspace) }, onLongClick = { key(Key.Clear) })
                }
                KeyRow {
                    CalcKey("=", KeyStyle.Accent, h, { key(Key.Equals) }, weight = 4f)
                }
            }
        }
    }
}
