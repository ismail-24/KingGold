package com.example.ui.screens.checkout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GoldOrder
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.openDirections
import kotlinx.coroutines.launch

@Composable
fun SuccessScreen(
    order: GoldOrder,
    onViewAllOrders: () -> Unit,
) {
    val scale = remember { Animatable(0.4f) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    Scaffold(
        containerColor = AppColors.Parchment,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = AppColors.Parchment,
                shadowElevation = 4.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    OutlinedButton(
                        onClick = onViewAllOrders,
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("view_all_orders_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Ink),
                    ) {
                        Text(
                            text = "عرض كل الطلبات",
                            style = AppText.body.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.scale(scale.value),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(AppColors.TrustSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "تم الدفع",
                                tint = AppColors.Trust,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "تم الدفع بنجاح",
                            style = AppText.display.copy(fontSize = 24.sp),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(AppColors.Ink)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "${formatCurrency(order.total)} ج.م محفوظة في الضمان",
                                style = AppText.body.copy(
                                    color = AppColors.BrassLight,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = AppColors.Ink,
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "رمز الاستلام",
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            PickupQrPattern(
                                seedKey = order.id,
                                modifier = Modifier.size(144.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = order.id.replace("-", " "),
                                style = AppText.display.copy(
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    letterSpacing = 3.sp,
                                ),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = "موعد الاستلام",
                                    tint = AppColors.BrassLight,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "الاستلام ${order.pickupWindow}",
                                    style = AppText.body.copy(
                                        color = AppColors.BrassLight,
                                        fontSize = 12.5.sp,
                                    ),
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "اعرض الرمز عند الكاونتر، ومتديهوش لحد إلا وأنت جوه المحل.",
                        textAlign = TextAlign.Center,
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                    )
                }

                item {
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(Color.White)
                            .border(1.dp, AppColors.Line, shape)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = order.shop.name,
                                style = AppText.body.copy(fontWeight = FontWeight.ExtraBold),
                            )
                            Text(
                                text = order.shop.address,
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                openDirections(context, order.shop.address) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("العنوان: ${order.shop.address}")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = "اتجاهات",
                                style = AppText.body.copy(
                                    color = AppColors.Ink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickupQrPattern(
    seedKey: String,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cells = 8
        val cellSize = size.minDimension / cells
        drawRoundRect(
            color = AppColors.Parchment,
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
        )
        var x = seedKey.hashCode() and 0x7fffffff
        if (x == 0) x = 42
        for (r in 0 until cells) {
            for (c in 0 until cells) {
                x = ((x.toLong() * 1103515245L + 12345L) and 0x7fffffffL).toInt()
                val isFinder = (r < 2 && c < 2) || (r < 2 && c >= cells - 2) || (r >= cells - 2 && c < 2)
                if (isFinder || x % 2 == 0) {
                    drawRect(
                        color = AppColors.Ink,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize, cellSize),
                    )
                }
            }
        }
    }
}
