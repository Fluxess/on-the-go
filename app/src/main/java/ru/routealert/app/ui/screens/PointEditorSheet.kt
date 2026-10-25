package ru.routealert.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.routealert.app.alert.NotificationHelper
import ru.routealert.app.data.AlertType
import ru.routealert.app.data.RoutePointRepository
import ru.routealert.app.ui.PointDraft
import ru.routealert.app.ui.theme.Glass
import ru.routealert.app.ui.theme.GlassBlue
import ru.routealert.app.ui.theme.LiquidGlassChip
import ru.routealert.app.ui.theme.liquidGlass
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointEditorSheet(
    draft: PointDraft,
    isPro: Boolean = true,
    onOpenPaywall: () -> Unit = {},
    onDraftChange: ((PointDraft) -> PointDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onShowOnMap: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xE61A1A1C),
        scrimColor = Color.Black.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (draft.isNew) "Новая точка" else "Редактирование",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = draft.name,
                onValueChange = { value -> onDraftChange { it.copy(name = value) } },
                label = { Text("Название") },
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(shape = RoundedCornerShape(20.dp), colors = Glass.PanelFill),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = Color.White.copy(alpha = 0.7f),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.55f),
                    cursorColor = GlassBlue
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Радиус: ${NotificationHelper.formatDistance(draft.radiusMeters)}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = "Для транспорта надёжнее 100–300 м (GPS часто ошибается на ±15–50 м).",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                softWrap = true
            )
            Slider(
                value = draft.radiusMeters,
                onValueChange = { value ->
                    onDraftChange { it.copy(radiusMeters = value.roundToInt().toFloat()) }
                },
                valueRange = RoutePointRepository.MIN_RADIUS_M..RoutePointRepository.MAX_RADIUS_M,
                steps = 0,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = GlassBlue,
                    inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(50f, 200f, 500f, 1000f).forEach { preset ->
                    LiquidGlassChip(
                        label = NotificationHelper.formatDistance(preset),
                        selected = draft.radiusMeters == preset,
                        onClick = { onDraftChange { it.copy(radiusMeters = preset) } }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Тип оповещения", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidGlassChip("Уведомление", draft.alertType == AlertType.NOTIFICATION) {
                    onDraftChange { it.copy(alertType = AlertType.NOTIFICATION) }
                }
                LiquidGlassChip(
                    label = if (isPro) "Будильник" else "Будильник · Pro",
                    selected = draft.alertType == AlertType.ALARM
                ) {
                    if (isPro) {
                        onDraftChange { it.copy(alertType = AlertType.ALARM) }
                    } else {
                        onOpenPaywall()
                    }
                }
                LiquidGlassChip(
                    label = if (isPro) "Оба" else "Оба · Pro",
                    selected = draft.alertType == AlertType.BOTH
                ) {
                    if (isPro) {
                        onDraftChange { it.copy(alertType = AlertType.BOTH) }
                    } else {
                        onOpenPaywall()
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            GlassButton("Точка на карте", Glass.Selected, onShowOnMap)
            Spacer(modifier = Modifier.height(8.dp))
            GlassButton(
                "Сохранить",
                listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.10f)),
                onSave
            )
            if (!draft.isNew) {
                Spacer(modifier = Modifier.height(8.dp))
                GlassButton("Удалить точку", Glass.Danger, onDelete)
            }
            Spacer(modifier = Modifier.height(8.dp))
            GlassButton(
                "Отмена",
                listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.06f)),
                onDismiss
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GlassButton(label: String, colors: List<Color>, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(24.dp), colors = colors)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}
