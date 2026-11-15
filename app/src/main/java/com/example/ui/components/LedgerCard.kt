package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import java.text.NumberFormat
import java.util.Locale

data class LedgerLine(
    val label: String,
    val value: String,
    val isTotal: Boolean = false,
)

fun formatCurrency(amount: Double, decimals: Int = 2): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }
    return formatter.format(amount)
}

/**
 * Goldsmith ledger receipt card with crisp 4.dp corners and perforated dotted dividers.
 */
@Composable
fun LedgerCard(
    lines: List<LedgerLine>,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.CardWhite, shape)
            .border(1.dp, AppColors.Line, shape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        lines.forEachIndexed { index, line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = line.label,
                    modifier = Modifier.weight(1f),
                    style = AppText.body.copy(
                        fontSize = if (line.isTotal) 15.sp else 13.5.sp,
                        fontWeight = if (line.isTotal) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (line.isTotal) AppColors.Ink else AppColors.Muted,
                    ),
                )
                Text(
                    text = line.value,
                    style = AppText.body.copy(
                        fontSize = if (line.isTotal) 17.sp else 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (line.isTotal) AppColors.Brass else AppColors.Ink,
                    ),
                )
            }
            if (index != lines.lastIndex) {
                DottedDivider()
            }
        }
    }
}

@Composable
fun DottedDivider(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        var x = 0f
        val dash = 3.dp.toPx()
        val gap = 4.dp.toPx()
        while (x < size.width) {
            drawLine(
                color = AppColors.Line,
                start = Offset(x, 0f),
                end = Offset((x + dash).coerceAtMost(size.width), 0f),
                strokeWidth = 1.dp.toPx(),
            )
            x += dash + gap
        }
    }
}
