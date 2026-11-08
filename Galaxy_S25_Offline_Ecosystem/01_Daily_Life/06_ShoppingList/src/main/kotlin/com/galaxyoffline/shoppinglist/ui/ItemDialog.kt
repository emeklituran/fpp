package com.galaxyoffline.shoppinglist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.shoppinglist.data.Category
import com.galaxyoffline.shoppinglist.data.Item
import com.galaxyoffline.shoppinglist.data.UNITS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDialog(item: Item, onDismiss: () -> Unit, onSave: (Item) -> Unit, onDelete: () -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var qty by remember { mutableStateOf(TrFormat.number(item.quantity, 2, grouping = false)) }
    var unit by remember { mutableStateOf(item.unit) }
    var category by remember { mutableStateOf(Category.of(item.category)) }
    var price by remember { mutableStateOf(if (item.price > 0) TrFormat.number(item.price, 2, grouping = false) else "") }
    val qtyValue = TrFormat.parseDecimal(qty)
    val priceValue = if (price.isBlank()) 0.0 else TrFormat.parseDecimal(price)
    val valid = name.isNotBlank() && qtyValue != null && qtyValue > 0 && priceValue != null && priceValue >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ürünü düzenle") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(80) }, label = { Text("Ürün") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = { qtyValue?.let { qty = TrFormat.number((it - 1).coerceAtLeast(1.0), 2, false) } }) { Icon(Icons.Outlined.Remove, "Azalt") }
                    OutlinedTextField(qty, { qty = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(8) }, label = { Text("Miktar") },
                        singleLine = true, isError = qtyValue == null || qtyValue <= 0, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f))
                    FilledTonalIconButton(onClick = { qty = TrFormat.number((qtyValue ?: 0.0) + 1, 2, false) }) { Icon(Icons.Outlined.Add, "Artır") }
                }
                Picker("Birim", UNITS, unit, { it }) { unit = it }
                Picker("Kategori", Category.entries, category, { "${it.emoji} ${it.label}" }) { category = it }
                OutlinedTextField(price, { price = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(12) },
                    label = { Text("Tahmini birim fiyat (₺, isteğe bağlı)") }, singleLine = true, isError = priceValue == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(item.copy(name = name, quantity = qtyValue ?: 1.0, unit = unit, category = category.name, price = priceValue ?: 0.0))
            }) { Text("Kaydet") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Sil") }
                TextButton(onClick = onDismiss) { Text("Vazgeç") }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Picker(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(text(selected), {}, readOnly = true, label = { Text(label) }, singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(text(o)) }, onClick = { onSelect(o); open = false }) }
        }
    }
}
