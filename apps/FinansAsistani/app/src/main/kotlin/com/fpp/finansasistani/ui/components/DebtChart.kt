package com.fpp.finansasistani.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.fpp.finans.core.Dates
import com.fpp.finans.core.Money
import java.time.YearMonth

/**
 * Toplam borcun aylara göre azalışını çizen hafif grafik. Harici grafik kütüphanesi kullanılmaz.
 * [values][0] başlangıç bakiyesi, sonraki her eleman bir ay sonrasıdır.
 */
@Composable
fun DebtLineChart(
    values: List<Double>,
    startDate: YearMonth,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    // Çok uzun planlarda çizim maliyetini sınırlamak için en fazla ~240 nokta kullanılır.
    val points = remember(values) {
        if (values.size <= 240) values.mapIndexed { i, v -> i to v }
        else {
            val step = values.size / 240.0
            (0 until 240).map { k -> val i = (k * step).toInt(); i to values[i] } + ((values.size - 1) to values.last())
        }
    }
    val description = "Borç grafiği: ${Money.format(values.firstOrNull() ?: 0.0)} borç ${values.size - 1} ayda sıfırlanıyor."

    Canvas(
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = description },
    ) {
        if (points.size < 2) return@Canvas
        val maxValue = (points.maxOf { it.second }).coerceAtLeast(1.0)
        val lastIndex = points.last().first.coerceAtLeast(1)
        val left = 60.dp.toPx()
        val right = 12.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = 26.dp.toPx()
        val w = size.width - left - right
        val h = size.height - top - bottom
        if (w <= 0f || h <= 0f) return@Canvas

        fun x(i: Int) = left + w * i / lastIndex
        fun y(v: Double) = top + (h * (1.0 - v / maxValue)).toFloat()

        // Yatay ızgara ve eksen etiketleri
        for (g in 0..4) {
            val gy = top + h * g / 4f
            drawLine(gridColor, Offset(left, gy), Offset(size.width - right, gy), strokeWidth = 1.dp.toPx())
            val layout = measurer.measure(Money.formatCompact(maxValue * (4 - g) / 4.0), labelStyle)
            drawText(layout, topLeft = Offset(left - layout.size.width - 6.dp.toPx(), gy - layout.size.height / 2f))
        }

        val line = Path().apply {
            moveTo(x(points[0].first), y(points[0].second))
            for (k in 1 until points.size) lineTo(x(points[k].first), y(points[k].second))
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(x(points.last().first), top + h)
            lineTo(x(points[0].first), top + h)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.30f), Color.Transparent), startY = top, endY = top + h))
        drawPath(line, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Zaman ekseni: başlangıç, orta ve bitiş
        listOf(0, lastIndex / 2, lastIndex).distinct().forEach { i ->
            val label = startDate.plusMonths(i.toLong()).format(Dates.shortMonthYear)
            val layout = measurer.measure(label, labelStyle)
            val lx = (x(i) - layout.size.width / 2f).coerceIn(left - 8.dp.toPx(), size.width - layout.size.width.toFloat())
            drawText(layout, topLeft = Offset(lx, top + h + 6.dp.toPx()))
        }
    }
}
