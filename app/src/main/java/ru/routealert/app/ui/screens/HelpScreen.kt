package ru.routealert.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.routealert.app.data.ThemeMode
import ru.routealert.app.ui.PermissionHints
import ru.routealert.app.ui.theme.Glass
import ru.routealert.app.ui.theme.LiquidGlassChip
import ru.routealert.app.ui.theme.LiquidGlassPanel
import ru.routealert.app.ui.theme.Signal
import ru.routealert.app.ui.theme.liquidGlass

@Composable
fun HelpScreen(
    permissions: PermissionHints,
    themeMode: ThemeMode,
    isPro: Boolean,
    onThemeMode: (ThemeMode) -> Unit,
    onSetPro: (Boolean) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenFullScreenIntentSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .padding(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Помощь",
            style = MaterialTheme.typography.headlineLarge,
            color = colors.onBackground
        )

        GlassSection(title = "Как пользоваться") {
            HelpLine("Удерживайте карту, чтобы создать точку.")
            HelpLine("Задайте название, радиус от 10 м до 10 км и тип оповещения.")
            HelpLine("Круглая кнопка «Старт» включает поездку. GPS работает, пока поездка активна.")
            HelpLine("У точки сработает уведомление или будильник. «Стоп» его выключает.")
        }

        GlassSection(title = "Тема") {
            HelpLine("Меняет оформление экрана помощи и стиль карты (светлая / тёмная).")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiquidGlassChip("Как в системе", themeMode == ThemeMode.SYSTEM) {
                    onThemeMode(ThemeMode.SYSTEM)
                }
                LiquidGlassChip("Светлая", themeMode == ThemeMode.LIGHT) {
                    onThemeMode(ThemeMode.LIGHT)
                }
                LiquidGlassChip("Тёмная", themeMode == ThemeMode.DARK) {
                    onThemeMode(ThemeMode.DARK)
                }
            }
        }

        GlassSection(title = "Pro") {
            HelpLine(
                "Pro: шаринг точек ссылкой и QR (родители/дети). " +
                    "Поиск адреса и live-уведомление в поездке доступны всем."
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiquidGlassChip("Pro вкл", isPro) { onSetPro(true) }
                LiquidGlassChip("Pro выкл", !isPro) { onSetPro(false) }
            }
        }

        GlassSection(title = "Радиус") {
            HelpLine(
                "В транспорте GPS часто ошибается на 15–50 м. Надёжнее 100–300 м. " +
                    "Точка срабатывает один раз за поездку."
            )
        }

        GlassSection(title = "Разрешения") {
            StatusLine("Геолокация", permissions.hasFineLocation)
            StatusLine("Уведомления", permissions.hasNotifications)
            StatusLine("Батарея без ограничений", permissions.ignoringBattery)
            StatusLine("Полноэкранный будильник", permissions.canFullScreenIntent)
            HelpLine(
                "На Samsung: Батарея → ограничение использования → Без ограничений " +
                    "(или «Не спящие приложения»). На Xiaomi ещё включите автозапуск."
            )
        }

        GlassSection(title = "О программе") {
            HelpLine("Версия 1.0.0 · Контакты, политика конфиденциальности и правовая информация.")
            GlassActionButton("Открыть «О программе»", onOpenAbout)
        }

        GlassActionButton("Геолокация", onOpenLocationSettings)
        GlassActionButton("Батарея", onOpenBatterySettings)
        GlassActionButton("Полноэкранный будильник", onOpenFullScreenIntentSettings)
        GlassActionButton("Настройки приложения", onOpenAppSettings)
    }
}

@Composable
private fun GlassSection(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    LiquidGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = Glass.PanelFill
    ) {
        Column(
            modifier = Modifier
                .background(colors.surface.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            content()
        }
    }
}

@Composable
private fun HelpLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    )
}

@Composable
private fun GlassActionButton(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(24.dp), colors = Glass.MapFillStrong)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = colors.primary),
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun StatusLine(label: String, ok: Boolean) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = if (ok) "✓ $label" else "○ $label",
        color = if (ok) colors.onSurface.copy(alpha = 0.9f) else Signal
    )
}
