package ru.routealert.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.yandex.mapkit.map.MapType
import ru.routealert.app.R
import ru.routealert.app.alert.NotificationHelper
import ru.routealert.app.data.RoutePoint
import ru.routealert.app.service.TripUiState
import ru.routealert.app.ui.components.YandexMapView
import ru.routealert.app.ui.theme.Glass
import ru.routealert.app.ui.theme.LiquidGlassChip
import ru.routealert.app.ui.theme.LiquidGlassCircle
import ru.routealert.app.ui.theme.LiquidGlassPanel
import ru.routealert.app.ui.theme.liquidGlass

@Composable
fun MapScreen(
    points: List<RoutePoint>,
    trip: TripUiState,
    nightMode: Boolean,
    onNightModeChange: (Boolean) -> Unit,
    onLongTap: (Double, Double) -> Unit,
    onMarkerTap: (RoutePoint) -> Unit,
    onStartTrip: () -> Unit,
    onStopTrip: () -> Unit,
    onOpenSearch: () -> Unit = {},
    focusTick: Int = 0,
    focusLat: Double? = null,
    focusLon: Double? = null
) {
    val context = LocalContext.current
    val hasEnabled = points.any { it.enabled }
    var located by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var recenterTick by remember { mutableIntStateOf(0) }
    var mapMenuOpen by remember { mutableStateOf(false) }
    val userLat = trip.latitude ?: located?.first
    val userLon = trip.longitude ?: located?.second

    Box(modifier = Modifier.fillMaxSize()) {
        YandexMapView(
            modifier = Modifier.fillMaxSize(),
            points = points,
            userLat = userLat,
            userLon = userLon,
            mapType = MapType.MAP,
            nightMode = nightMode,
            recenterTick = recenterTick,
            focusTick = focusTick,
            focusLat = focusLat,
            focusLon = focusLon,
            onMapLongTap = onLongTap,
            onMarkerTap = onMarkerTap
        )

        if (points.isEmpty()) {
            LiquidGlassPanel(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = Glass.MapFillStrong
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        "Удерживайте карту, чтобы поставить точку",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 116.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (mapMenuOpen) {
                LiquidGlassChip("Светлая", !nightMode) {
                    onNightModeChange(false)
                    mapMenuOpen = false
                }
                LiquidGlassChip("Тёмная", nightMode) {
                    onNightModeChange(true)
                    mapMenuOpen = false
                }
            }
            LiquidGlassCircle(onClick = onOpenSearch) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Поиск",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            LiquidGlassCircle(onClick = { mapMenuOpen = !mapMenuOpen }) {
                Icon(
                    imageVector = Icons.Filled.Layers,
                    contentDescription = "Стиль карты",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            LiquidGlassCircle(
                onClick = {
                    moveToMyLocation(context, trip.latitude, trip.longitude) { lat, lon ->
                        located = lat to lon
                        recenterTick++
                    }
                }
            ) {
                CrosshairIcon()
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 76.dp, bottom = 108.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (trip.isActive) {
                LiquidGlassPanel(
                    shape = RoundedCornerShape(28.dp),
                    colors = Glass.Danger
                ) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Text(
                            "Поездка · ${trip.enabledPointCount}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val nearest = trip.nearestName
                        val dist = trip.nearestDistanceMeters
                        if (nearest != null && dist != null) {
                            Text(
                                "$nearest · ${NotificationHelper.formatDistance(dist)}",
                                color = Color.White.copy(alpha = 0.92f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            SwipePill(
                hint = when {
                    trip.isActive -> "Смахните, чтобы остановить"
                    points.isEmpty() -> "Сначала поставьте точку"
                    !hasEnabled -> "Включите точку"
                    else -> "Начать поездку"
                },
                thumbText = if (trip.isActive) "Стоп" else "Старт",
                enabled = trip.isActive || hasEnabled,
                active = trip.isActive,
                onConfirm = { if (trip.isActive) onStopTrip() else onStartTrip() }
            )
        }
    }
}

@Composable
private fun CrosshairIcon() {
    Canvas(Modifier.size(22.dp)) {
        val color = Color.White.copy(alpha = 0.95f)
        val stroke = 1.7.dp.toPx()
        val c = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - stroke
        drawCircle(color = color, radius = radius, center = c, style = Stroke(stroke))
        drawCircle(color = color, radius = 2.4.dp.toPx(), center = c)
        val gap = 5.dp.toPx()
        drawLine(color, Offset(c.x, c.y - radius), Offset(c.x, c.y - gap), stroke)
        drawLine(color, Offset(c.x, c.y + gap), Offset(c.x, c.y + radius), stroke)
        drawLine(color, Offset(c.x - radius, c.y), Offset(c.x - gap, c.y), stroke)
        drawLine(color, Offset(c.x + gap, c.y), Offset(c.x + radius, c.y), stroke)
    }
}

@Composable
private fun SwipePill(
    hint: String,
    thumbText: String,
    enabled: Boolean,
    active: Boolean,
    onConfirm: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var trackWidth by remember { mutableStateOf(0) }
    val thumbWidth = 92.dp
    val max = (trackWidth - with(density) { thumbWidth.toPx() }).coerceAtLeast(0f)
    val trackShape = RoundedCornerShape(36.dp)
    val thumbShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .liquidGlass(
                shape = trackShape,
                colors = if (enabled) Glass.MapFillStrong else Glass.MapFill,
                stroke = Glass.StrokeSoft
            )
            .onSizeChanged { trackWidth = it.width }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(start = thumbWidth + 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                hint,
                modifier = Modifier.weight(1f, fill = false),
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .size(width = thumbWidth, height = 56.dp)
                .liquidGlass(
                    shape = thumbShape,
                    colors = if (active) {
                        Glass.Danger
                    } else {
                        listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.78f))
                    },
                    stroke = Color.White.copy(alpha = 0.65f)
                )
                .pointerInput(enabled, max, active) {
                    if (!enabled || max <= 0f) return@pointerInput
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                offset.snapTo((offset.value + dragAmount).coerceIn(0f, max))
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                if (offset.value > max * 0.72f) {
                                    offset.snapTo(0f)
                                    onConfirm()
                                } else {
                                    offset.animateTo(0f)
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f) }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                thumbText,
                color = if (active) Color.White else Color.Black,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun moveToMyLocation(
    context: android.content.Context,
    knownLat: Double?,
    knownLon: Double?,
    onFound: (Double, Double) -> Unit
) {
    if (knownLat != null && knownLon != null) {
        onFound(knownLat, knownLon)
        return
    }
    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) return
    LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location ->
            if (location != null) onFound(location.latitude, location.longitude)
        }
}
