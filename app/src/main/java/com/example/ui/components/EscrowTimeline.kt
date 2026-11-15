package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

private val EscrowSteps = listOf(
    "تدفع النهارده",
    "المبلغ محفوظ بأمان",
    "يُحوَّل للمحل بعد استلامك",
)

@Composable
fun EscrowTimeline(
    activeStep: Int = 0,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.Ink,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "حماية الضمان",
                    tint = AppColors.BrassLight,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "المبلغ محفوظ في الضمان حتى الاستلام",
                    style = AppText.body.copy(
                        color = AppColors.BrassLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "يحتفظ King Gold بمبلغك بأمان، ولا يصل للمحل إلا بعد ما تستلم ذهبك وتأكّد برمز الاستلام.",
                style = AppText.body.copy(
                    color = AppColors.Muted,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                ),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EscrowSteps.forEachIndexed { index, label ->
                    val done = index <= activeStep
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (done) AppColors.BrassLight else AppColors.InkSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = AppText.body.copy(
                                    color = if (done) AppColors.Ink else AppColors.Muted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                ),
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            textAlign = TextAlign.Center,
                            style = AppText.body.copy(
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp,
                                color = if (done) Color.White else AppColors.Muted,
                            ),
                        )
                    }
                }
            }
        }
    }
}
