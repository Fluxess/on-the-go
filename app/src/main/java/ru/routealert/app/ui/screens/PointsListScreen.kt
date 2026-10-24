package ru.routealert.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.routealert.app.alert.NotificationHelper
import ru.routealert.app.data.AlertType
import ru.routealert.app.data.RoutePoint
import ru.routealert.app.ui.theme.Glass
import ru.routealert.app.ui.theme.GlassBlue
import ru.routealert.app.ui.theme.LiquidGlassPanel

@Composable
fun PointsListScreen(
    points: List<RoutePoint>,
    onOpen: (RoutePoint) -> Unit,
    onToggle: (RoutePoint, Boolean) -> Unit,
    onDelete: (RoutePoint) -> Unit,
    onShare: () -> Unit = {}
) {
    if (points.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Пока нет точек", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                "На карте удерживайте место, чтобы добавить точку.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Точки", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text(
                    "Нажмите, чтобы редактировать",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
                TextButton(onClick = onShare) {
                    Text("Поделиться · Pro", color = GlassBlue)
                }
            }
            items(points, key = { it.id }) { point ->
                LiquidGlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(point) },
                    shape = RoundedCornerShape(24.dp),
                    colors = Glass.PanelFill
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(point.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                "${NotificationHelper.formatDistance(point.radiusMeters)} · ${alertLabel(point.alertType)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                            TextButton(onClick = { onDelete(point) }) {
                                Text("Удалить", color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                        Switch(
                            checked = point.enabled,
                            onCheckedChange = { onToggle(point, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GlassBlue,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.9f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.22f)
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun alertLabel(type: AlertType): String = when (type) {
    AlertType.NOTIFICATION -> "уведомление"
    AlertType.ALARM -> "будильник"
    AlertType.BOTH -> "оба"
}
