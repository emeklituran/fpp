package com.galaxyoffline.core.calculator

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.designsystem.screens.SwitchRow

/** Hesap makinelerine özel ayarlar (Ayarlar ekranının "Uygulama" bölümü). */
@Composable
fun ColumnScope.CalcSettingsSection(vm: CalculatorViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    SwitchRow("Titreşimli geri bildirim", s.haptics, { vm.setHaptics(it) }, "Tuşlara basıldığında hafif titreşim.")
    SwitchRow("Binlik ayırıcı", s.grouping, { vm.setGrouping(it) }, "1234567 yerine 1.234.567 göster.")
}
