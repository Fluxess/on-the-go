package ru.routealert.app.ui.screens

import android.os.Build
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.routealert.app.analytics.AppAnalyticsData
import ru.routealert.app.data.LegalDocs
import ru.routealert.app.data.RoutePoint
import ru.routealert.app.subscription.SubscriptionState
import ru.routealert.app.subscription.SubscriptionStatus
import ru.routealert.app.ui.theme.Glass
import ru.routealert.app.ui.theme.GlassBlue
import ru.routealert.app.ui.theme.LiquidGlassPanel
import ru.routealert.app.ui.theme.Signal
import ru.routealert.app.ui.theme.liquidGlass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretAdminStatsSheet(
    analytics: AppAnalyticsData,
    subscriptionState: SubscriptionState,
    points: List<RoutePoint>,
    onResetStats: () -> Unit,
    onToggleProTest: (Boolean) -> Unit,
    onResetTrialTest: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isAuthenticated by remember { mutableStateOf(false) }

    var loginInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF2121216),
        scrimColor = Color.Black.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .padding(bottom = 36.dp)
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
                                brush = Brush.verticalGradient(listOf(Color(0xFF673AB7), Color(0xFF512DA8))),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAuthenticated) Icons.Rounded.QueryStats else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isAuthenticated) "Панель статистики (Admin)" else "Вход в панель управления",
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

            Spacer(modifier = Modifier.height(18.dp))

            if (!isAuthenticated) {
                // Auth form
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Скрытая зона для разработчика и администратора",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = loginInput,
                        onValueChange = {
                            loginInput = it
                            authError = null
                        },
                        label = { Text("Логин") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(shape = RoundedCornerShape(18.dp), colors = Glass.PanelFill),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedLabelColor = Color.White.copy(alpha = 0.8f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = GlassBlue
                        )
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            authError = null
                        },
                        label = { Text("Пароль") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(shape = RoundedCornerShape(18.dp), colors = Glass.PanelFill),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedLabelColor = Color.White.copy(alpha = 0.8f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = GlassBlue
                        )
                    )

                    if (authError != null) {
                        Text(
                            text = authError!!,
                            color = Signal,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .liquidGlass(shape = RoundedCornerShape(20.dp), colors = Glass.Selected)
                            .clickable {
                                if (loginInput.trim() == "Dragov" && passwordInput == "161107Dragov") {
                                    isAuthenticated = true
                                    authError = null
                                } else {
                                    authError = "Неверный логин или пароль"
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Войти", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            } else {
                // Authenticated: Full Statistics & Management Dashboard
                val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Subscription & Trial status card
                    StatsSectionCard(title = "Статус подписки и пользователя") {
                        val subText = when (val s = subscriptionState.status) {
                            is SubscriptionStatus.TrialActive -> "Бесплатный Trial: ${s.daysLeft} дн. осталось"
                            is SubscriptionStatus.ProPurchased -> "Куплен Pro (${s.planId})"
                            is SubscriptionStatus.ExpiredFree -> "Истёк (Бесплатный режим)"
                        }
                        StatLine("Текущий статус", subText, highlight = subscriptionState.isProUnlocked)
                        StatLine("Доступ Pro разблокирован", if (subscriptionState.isProUnlocked) "ДА" else "НЕТ")
                        if (subscriptionState.status is SubscriptionStatus.TrialActive) {
                            val ends = dateFormat.format(Date(subscriptionState.status.endsAtTimestampMs))
                            StatLine("Окончание триала", ends)
                        }
                    }

                    // App usage analytics card
                    StatsSectionCard(title = "Активность приложения") {
                        StatLine("Запусков приложения", analytics.appLaunches.toString())
                        StatLine("Создано точек всего", analytics.totalPointsCreated.toString())
                        StatLine("Точек в базе сейчас", points.size.toString())
                        StatLine("Точек активно сейчас", points.count { it.enabled }.toString())
                        StatLine("Поездок начато", analytics.totalTripsStarted.toString())
                        StatLine("Срабатываний алертов", analytics.totalAlertsTriggered.toString())
                        StatLine("Поделились точками (QR/link)", analytics.totalSharesCount.toString())
                    }

                    // Conversion & Monetization stats card
                    StatsSectionCard(title = "Монетизация и конверсия") {
                        StatLine("Открытий экрана Paywall", analytics.totalPaywallViews.toString())
                        StatLine("Успешных покупок Pro", analytics.totalPurchasesCount.toString())
                        StatLine("Последний купленный тариф", analytics.lastPurchasedPlan)
                        if (analytics.firstLaunchTimeMs > 0L) {
                            StatLine("Первый запуск", dateFormat.format(Date(analytics.firstLaunchTimeMs)))
                        }
                        if (analytics.lastLaunchTimeMs > 0L) {
                            StatLine("Последний запуск", dateFormat.format(Date(analytics.lastLaunchTimeMs)))
                        }
                    }

                    // Device & System info card
                    StatsSectionCard(title = "Системные данные устройства") {
                        StatLine("Версия приложения", LegalDocs.APP_VERSION)
                        StatLine("Модель устройства", "${Build.MANUFACTURER} ${Build.MODEL}")
                        StatLine("Версия Android", "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    }

                    // Testing controls for admin
                    StatsSectionCard(title = "Управление для тестов") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = if (subscriptionState.isProUnlocked) listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)) else Glass.PanelFill
                                    )
                                    .clickable { onToggleProTest(!subscriptionState.isProUnlocked) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (subscriptionState.isProUnlocked) "Pro: ВКЛ" else "Pro: ВЫКЛ",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .liquidGlass(shape = RoundedCornerShape(14.dp), colors = Glass.PanelFill)
                                    .clickable { onResetTrialTest() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Сброс триала", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .liquidGlass(shape = RoundedCornerShape(14.dp), colors = listOf(Color(0x33E10600), Color(0x11E10600)))
                                .clickable { onResetStats() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = Signal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Сбросить счётчики статистики", color = Signal, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    LiquidGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = listOf(
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.04f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF90CAF9),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            content()
        }
    }
}

@Composable
private fun StatLine(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = if (highlight) Color(0xFF81C784) else Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp
        )
    }
}
