package ru.routealert.app.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.routealert.app.data.LegalDocs
import ru.routealert.app.data.RoutePoint
import ru.routealert.app.data.ThemeMode
import ru.routealert.app.location.PlaceHit
import ru.routealert.app.ui.screens.AboutScreen
import ru.routealert.app.ui.screens.HelpScreen
import ru.routealert.app.ui.screens.LegalDocSheet
import ru.routealert.app.ui.screens.MapScreen
import ru.routealert.app.ui.screens.PointEditorSheet
import ru.routealert.app.ui.screens.PointsListScreen
import ru.routealert.app.ui.screens.SearchPlaceSheet
import ru.routealert.app.ui.screens.SharePointsSheet
import ru.routealert.app.ui.theme.Glass
import ru.routealert.app.ui.theme.LiquidGlassPanel
import ru.routealert.app.ui.theme.liquidGlass

private enum class AppTab { Map, Points, Help }

@Composable
fun RouteAlertAppRoot(
    uiState: RouteAlertUiState,
    onAddPointAt: (Double, Double) -> Unit,
    onSavePoint: () -> Unit,
    onDeletePoint: (RoutePoint) -> Unit,
    onToggleEnabled: (RoutePoint, Boolean) -> Unit,
    onSelectPoint: (RoutePoint) -> Unit,
    onClearSelection: () -> Unit,
    onStartTrip: () -> Unit,
    onStopTrip: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenFullScreenIntentSettings: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onMapNightMode: (Boolean) -> Unit,
    onDraftChange: ((PointDraft) -> PointDraft) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onPlacePicked: (PlaceHit) -> Unit,
    onShowPlaceOnMap: (PlaceHit) -> Unit,
    onFocusDraftOnMap: () -> Unit,
    onOpenShare: () -> Unit,
    onCloseShare: () -> Unit,
    onSetPro: (Boolean) -> Unit,
    onClearImportMessage: () -> Unit,
    onImportText: (String) -> Unit = {}
) {
    var tab by rememberSaveable { mutableStateOf(AppTab.Map) }
    var showingAbout by rememberSaveable { mutableStateOf(false) }
    var showingLegalDoc by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(uiState.importMessage) {
        val msg = uiState.importMessage ?: return@LaunchedEffect
        tab = AppTab.Map
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        onClearImportMessage()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapScreen(
            points = uiState.points,
            trip = uiState.trip,
            nightMode = uiState.mapNightMode,
            onNightModeChange = onMapNightMode,
            onLongTap = onAddPointAt,
            onMarkerTap = onSelectPoint,
            onStartTrip = onStartTrip,
            onStopTrip = onStopTrip,
            onOpenSearch = onOpenSearch,
            focusTick = uiState.mapFocus?.tick ?: 0,
            focusLat = uiState.mapFocus?.latitude,
            focusLon = uiState.mapFocus?.longitude
        )
        AnimatedVisibility(
            visible = tab == AppTab.Points,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                LiquidGlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    colors = Glass.SheetFill
                ) {
                    PointsListScreen(
                        points = uiState.points,
                        onOpen = {
                            onSelectPoint(it)
                        },
                        onToggle = onToggleEnabled,
                        onDelete = onDeletePoint,
                        onShare = onOpenShare
                    )
                }
            }
        }
        if (tab == AppTab.Help) {
            HelpScreen(
                permissions = uiState.permissions,
                themeMode = uiState.themeMode,
                isPro = uiState.isPro,
                onThemeMode = onThemeMode,
                onSetPro = onSetPro,
                onOpenAppSettings = onOpenAppSettings,
                onOpenBatterySettings = onOpenBatterySettings,
                onOpenLocationSettings = onOpenLocationSettings,
                onOpenFullScreenIntentSettings = onOpenFullScreenIntentSettings,
                onOpenAbout = { showingAbout = true }
            )
        }

        if (showingAbout) {
            AboutScreen(
                onBack = { showingAbout = false },
                onOpenPrivacy = { showingLegalDoc = "privacy" },
                onOpenTerms = { showingLegalDoc = "terms" }
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .liquidGlass(shape = CircleShape, colors = Glass.MapFillStrong)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TabBubble("Карта", tab == AppTab.Map && !showingAbout) {
                showingAbout = false
                tab = AppTab.Map
            }
            TabBubble("Точки", tab == AppTab.Points && !showingAbout) {
                showingAbout = false
                tab = AppTab.Points
            }
            TabBubble("Помощь", tab == AppTab.Help && !showingAbout) {
                showingAbout = false
                tab = AppTab.Help
            }
        }
    }

    uiState.draft?.let { draft ->
        PointEditorSheet(
            draft = draft,
            onDraftChange = onDraftChange,
            onSave = onSavePoint,
            onDelete = {
                val point = uiState.points.find { it.id == draft.id }
                if (point != null) onDeletePoint(point) else onClearSelection()
            },
            onShowOnMap = {
                showingAbout = false
                tab = AppTab.Map
                onFocusDraftOnMap()
            },
            onDismiss = onClearSelection
        )
    }

    if (uiState.showSearch) {
        SearchPlaceSheet(
            onPick = onPlacePicked,
            onShowOnMap = {
                showingAbout = false
                tab = AppTab.Map
                onShowPlaceOnMap(it)
            },
            onDismiss = onCloseSearch
        )
    }
    if (uiState.showShare) {
        SharePointsSheet(
            points = uiState.points,
            isPro = uiState.isPro,
            onImportFromText = onImportText,
            onDismiss = onCloseShare
        )
    }

    when (showingLegalDoc) {
        "privacy" -> LegalDocSheet(
            title = "Политика конфиденциальности",
            content = LegalDocs.PRIVACY_POLICY,
            onDismiss = { showingLegalDoc = null }
        )
        "terms" -> LegalDocSheet(
            title = "Условия использования",
            content = LegalDocs.TERMS_OF_USE,
            onDismiss = { showingLegalDoc = null }
        )
    }
}

@Composable
private fun TabBubble(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .liquidGlass(
                shape = shape,
                colors = if (selected) Glass.Selected else listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.04f)
                ),
                stroke = if (selected) Color.White.copy(alpha = 0.5f) else Glass.StrokeSoft
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
