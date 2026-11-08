package com.fpp.finansasistani.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.fpp.finansasistani.ui.theme.LocalFinanceColors

enum class RecordKind(val tabLabel: String, val addLabel: String, val icon: ImageVector) {
    INCOME("Gelirler", "Gelir ekle", Icons.Outlined.Payments),
    DEBT("Borçlar", "Borç ekle", Icons.Outlined.CreditCard),
    EXPENSE("Giderler", "Gider ekle", Icons.Outlined.Home),
    SAVING("Birikimler", "Hedef ekle", Icons.Outlined.Savings);

    val tint: Color
        @Composable get() = when (this) {
            INCOME -> LocalFinanceColors.current.income
            DEBT -> LocalFinanceColors.current.debt
            EXPENSE -> LocalFinanceColors.current.expense
            SAVING -> LocalFinanceColors.current.saving
        }
}
