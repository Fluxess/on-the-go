package ru.routealert.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.routealert.app.subscription.BillingProvider
import ru.routealert.app.subscription.PurchaseResult
import ru.routealert.app.subscription.SubscriptionPlan
import ru.routealert.app.subscription.SubscriptionState
import ru.routealert.app.subscription.SubscriptionStatus
import ru.routealert.app.ui.theme.Glass
import ru.routealert.app.ui.theme.GlassBlue
import ru.routealert.app.ui.theme.LiquidGlassPanel
import ru.routealert.app.ui.theme.liquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    subscriptionState: SubscriptionState,
    billingProvider: BillingProvider,
    onSuccessPurchase: (SubscriptionPlan) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.YEARLY) }
    var isLoading by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF2141418),
        scrimColor = Color.Black.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                brush = Brush.verticalGradient(Glass.Selected),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "on-the-go Pro",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reverse-Trial Timeline card
            TrialTimelineCard(subscriptionState)

            Spacer(modifier = Modifier.height(20.dp))

            // Feature list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureRow(
                    icon = Icons.Rounded.Alarm,
                    title = "Громкий будильник в дороге",
                    subtitle = "Пробивает бесшумный режим, чтобы не проспать станцию"
                )
                FeatureRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Оповещение + будильник одновременно",
                    subtitle = "Двойная страховка для важных пересадок"
                )
                FeatureRow(
                    icon = Icons.Rounded.QrCode,
                    title = "Обмен маршрутами (QR и ссылки)",
                    subtitle = "Отправляйте готовые точки близким и попутчикам в 1 клик"
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Subscription plans
            Text(
                text = "Выберите вариант доступа:",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SubscriptionPlan.entries.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        isSelected = selectedPlan == plan,
                        onSelect = { selectedPlan = plan }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .liquidGlass(
                        shape = RoundedCornerShape(20.dp),
                        colors = Glass.Selected,
                        stroke = Color.White.copy(alpha = 0.5f)
                    )
                    .clickable(enabled = !isLoading) {
                        val activity = context as? Activity ?: return@clickable
                        isLoading = true
                        scope.launch {
                            val result = billingProvider.launchPurchaseFlow(activity, selectedPlan)
                            isLoading = false
                            when (result) {
                                is PurchaseResult.Success -> {
                                    Toast.makeText(
                                        context,
                                        "Pro успешно активирован!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onSuccessPurchase(result.plan)
                                    onDismiss()
                                }
                                is PurchaseResult.Cancelled -> {}
                                is PurchaseResult.Error -> {
                                    Toast.makeText(
                                        context,
                                        "Ошибка: ${result.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (subscriptionState.status is SubscriptionStatus.TrialActive) {
                            "Продолжить с Pro"
                        } else {
                            "Активировать Pro"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Провайдер: ${billingProvider.providerName}\nОтмена подписки доступна в любой момент в магазине приложений.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TrialTimelineCard(subscriptionState: SubscriptionState) {
    val daysLeft = if (subscriptionState.status is SubscriptionStatus.TrialActive) {
        subscriptionState.status.daysLeft
    } else 0

    LiquidGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = listOf(
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.04f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "7 дней бесплатного тест-драйва",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (subscriptionState.status is SubscriptionStatus.TrialActive) {
                    Text(
                        text = "Осталось $daysLeft дн.",
                        color = Color(0xFF81D4FA),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                } else if (subscriptionState.status is SubscriptionStatus.ProPurchased) {
                    Text(
                        text = "Активна Pro",
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TimelineStep(
                title = "День 1: Старт полного доступа",
                subtitle = "Все функции Pro включены сразу без ввода карты",
                isCompleted = true,
                isCurrent = daysLeft > 2 && subscriptionState.status is SubscriptionStatus.TrialActive
            )
            Spacer(modifier = Modifier.height(10.dp))
            TimelineStep(
                title = "День 5: Напоминание",
                subtitle = "Уведомление об окончании пробного периода",
                isCompleted = daysLeft in 1..2 || subscriptionState.status is SubscriptionStatus.ExpiredFree || subscriptionState.status is SubscriptionStatus.ProPurchased,
                isCurrent = daysLeft in 2..3 && subscriptionState.status is SubscriptionStatus.TrialActive
            )
            Spacer(modifier = Modifier.height(10.dp))
            TimelineStep(
                title = "День 7: Выбор тарифа",
                subtitle = "Продлите Pro или останьтесь на базовых уведомлениях",
                isCompleted = subscriptionState.status is SubscriptionStatus.ExpiredFree || subscriptionState.status is SubscriptionStatus.ProPurchased,
                isCurrent = daysLeft <= 1 && subscriptionState.status is SubscriptionStatus.TrialActive
            )
        }
    }
}

@Composable
private fun TimelineStep(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isCurrent: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = when {
                        isCurrent -> GlassBlue
                        isCompleted -> Color(0xFF4CAF50)
                        else -> Color.White.copy(alpha = 0.2f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.85f),
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(10.dp),
                    colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF81D4FA),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = shape,
                colors = if (isSelected) Glass.Selected else listOf(
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.04f)
                ),
                stroke = if (isSelected) Color.White.copy(alpha = 0.5f) else Glass.StrokeSoft
            )
            .clickable(onClick = onSelect)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (plan.badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF4CAF50), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = plan.badge,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = plan.periodDescription,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
            }

            Text(
                text = plan.priceText,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}
