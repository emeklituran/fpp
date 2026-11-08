package com.galaxyoffline.scientificcalculator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
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
import com.galaxyoffline.core.math.AngleMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScientificScreen(vm: CalculatorViewModel, onOpenHistory: () -> Unit, onOpenSettings: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    val h = s.haptics
    val inv = s.inverse
    val k: (Key) -> Unit = vm::onKey
    val f = KeyStyle.Function

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bilimsel") },
                actions = {
                    IconButton(onClick = onOpenHistory) { Icon(Icons.Outlined.History, contentDescription = "Geçmiş") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Ayarlar") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().navigationBarsPadding()) {
            CalcDisplay(s, Modifier.weight(0.75f)) {
                AssistChip(onClick = { k(Key.ToggleAngle) }, label = { Text(if (s.angle == AngleMode.DEG) "Derece" else "Radyan") })
                if (inv) {
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick = { k(Key.ToggleInverse) }, label = { Text("2nd") })
                }
            }
            Column(Modifier.weight(2.25f).fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
                val fw = 0.62f
                KeyRow(fw) {
                    CalcKey("2nd", if (inv) KeyStyle.Accent else f, h, { k(Key.ToggleInverse) }, small = true)
                    CalcKey(if (s.angle == AngleMode.DEG) "DEG" else "RAD", f, h, { k(Key.ToggleAngle) }, small = true)
                    CalcKey("π", f, h, { k(Key.Insert("π")) }, small = true)
                    CalcKey("e", f, h, { k(Key.Insert("e")) }, small = true)
                    CalcKey("Ans", f, h, { k(Key.Insert("Ans")) }, small = true)
                }
                KeyRow(fw) {
                    CalcKey(if (inv) "sin⁻¹" else "sin", f, h, { k(Key.Insert(if (inv) "asin(" else "sin(")) }, small = true)
                    CalcKey(if (inv) "cos⁻¹" else "cos", f, h, { k(Key.Insert(if (inv) "acos(" else "cos(")) }, small = true)
                    CalcKey(if (inv) "tan⁻¹" else "tan", f, h, { k(Key.Insert(if (inv) "atan(" else "tan(")) }, small = true)
                    CalcKey(if (inv) "eˣ" else "ln", f, h, { k(Key.Insert(if (inv) "e^(" else "ln(")) }, small = true)
                    CalcKey(if (inv) "10ˣ" else "log", f, h, { k(Key.Insert(if (inv) "10^(" else "log(")) }, small = true)
                }
                KeyRow(fw) {
                    CalcKey("sinh", f, h, { k(Key.Insert("sinh(")) }, small = true)
                    CalcKey("cosh", f, h, { k(Key.Insert("cosh(")) }, small = true)
                    CalcKey("tanh", f, h, { k(Key.Insert("tanh(")) }, small = true)
                    CalcKey("log₂", f, h, { k(Key.Insert("log2(")) }, small = true)
                    CalcKey("|x|", f, h, { k(Key.Insert("abs(")) }, small = true)
                }
                KeyRow(fw) {
                    CalcKey(if (inv) "x³" else "x²", f, h, { k(Key.Insert(if (inv) "^3" else "^2", operandFirst = true)) }, small = true)
                    CalcKey(if (inv) "ʸ√x" else "xʸ", f, h, {
                        if (inv) k(Key.Insert("^(1÷", operandFirst = true)) else k(Key.Op('^'))
                    }, small = true)
                    CalcKey("√", f, h, { k(Key.Insert("√(")) }, small = true)
                    CalcKey("∛", f, h, { k(Key.Insert("∛(")) }, small = true)
                    CalcKey("1/x", f, h, { k(Key.Insert("^(−1)", operandFirst = true)) }, small = true)
                }
                KeyRow(fw) {
                    CalcKey("(", f, h, { k(Key.Open) }, small = true)
                    CalcKey(")", f, h, { k(Key.Close) }, small = true)
                    CalcKey("x!", f, h, { k(Key.Postfix('!')) }, small = true)
                    CalcKey("%", f, h, { k(Key.Postfix('%')) }, small = true)
                    CalcKey("E", f, h, { k(Key.Exponent) }, small = true)
                }
                KeyRow {
                    CalcKey("7", KeyStyle.Number, h, { k(Key.Digit('7')) })
                    CalcKey("8", KeyStyle.Number, h, { k(Key.Digit('8')) })
                    CalcKey("9", KeyStyle.Number, h, { k(Key.Digit('9')) })
                    CalcKey("⌫", KeyStyle.Operator, h, { k(Key.Backspace) }, onLongClick = { k(Key.Clear) })
                    CalcKey("AC", KeyStyle.Danger, h, { k(Key.Clear) })
                }
                KeyRow {
                    CalcKey("4", KeyStyle.Number, h, { k(Key.Digit('4')) })
                    CalcKey("5", KeyStyle.Number, h, { k(Key.Digit('5')) })
                    CalcKey("6", KeyStyle.Number, h, { k(Key.Digit('6')) })
                    CalcKey("×", KeyStyle.Operator, h, { k(Key.Op('×')) })
                    CalcKey("÷", KeyStyle.Operator, h, { k(Key.Op('÷')) })
                }
                KeyRow {
                    CalcKey("1", KeyStyle.Number, h, { k(Key.Digit('1')) })
                    CalcKey("2", KeyStyle.Number, h, { k(Key.Digit('2')) })
                    CalcKey("3", KeyStyle.Number, h, { k(Key.Digit('3')) })
                    CalcKey("+", KeyStyle.Operator, h, { k(Key.Op('+')) })
                    CalcKey("−", KeyStyle.Operator, h, { k(Key.Op('−')) })
                }
                KeyRow {
                    CalcKey("0", KeyStyle.Number, h, { k(Key.Digit('0')) })
                    CalcKey(",", KeyStyle.Number, h, { k(Key.Decimal) })
                    CalcKey("±", KeyStyle.Number, h, { k(Key.Sign) })
                    CalcKey("=", KeyStyle.Accent, h, { k(Key.Equals) }, weight = 2f)
                }
            }
        }
    }
}
