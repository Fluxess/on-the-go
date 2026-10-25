package ru.routealert.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yandex.mapkit.MapKitFactory
import ru.routealert.app.service.TripTrackingService
import ru.routealert.app.ui.RouteAlertAppRoot
import ru.routealert.app.ui.RouteAlertViewModel
import ru.routealert.app.ui.theme.RouteAlertTheme
import ru.routealert.app.util.SettingsIntents

class MainActivity : ComponentActivity() {

    private var pendingStartTrip by mutableStateOf(false)
    private var permissionRefreshTick by mutableIntStateOf(0)
    private var setupStep by mutableStateOf(SetupStep.Idle)
    private var pendingImportUri by mutableStateOf<Uri?>(null)
    private var pendingImportText by mutableStateOf<String?>(null)

    private enum class SetupStep { Idle, Runtime, FullScreen, Battery, Done }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionRefreshTick++
        val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val notifOk = if (Build.VERSION.SDK_INT >= 33) {
            result[Manifest.permission.POST_NOTIFICATIONS] != false
        } else true
        if (fine && notifOk && pendingStartTrip) {
            pendingStartTrip = false
            ensureFullScreenIntentThenStart()
        }
        if (setupStep == SetupStep.Runtime) {
            continueAfterRuntimePermissions()
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { MapKitFactory.getInstance().onStart() }
    }

    override fun onStop() {
        runCatching { MapKitFactory.getInstance().onStop() }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        permissionRefreshTick++
        when (setupStep) {
            SetupStep.FullScreen -> continueAfterFullScreen()
            SetupStep.Battery -> setupStep = SetupStep.Done
            else -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureImportFromIntent(intent)
        enableEdgeToEdge()
        setContent {
            val vm: RouteAlertViewModel = viewModel(
                factory = RouteAlertViewModel.factory(application)
            )
            val ui by vm.uiState.collectAsStateWithLifecycle()

            RouteAlertTheme(mode = ui.themeMode) {
                LaunchedEffect(permissionRefreshTick) {
                    vm.refreshPermissionHints(
                        hasFineLocation = hasFineLocation(),
                        hasNotifications = hasNotifications(),
                        ignoringBattery = isIgnoringBatteryOptimizations(),
                        canFullScreenIntent = canUseFullScreenIntent()
                    )
                }

                LaunchedEffect(pendingImportUri) {
                    val uri = pendingImportUri ?: return@LaunchedEffect
                    vm.importFromUri(uri)
                    pendingImportUri = null
                }
                LaunchedEffect(pendingImportText) {
                    val text = pendingImportText ?: return@LaunchedEffect
                    vm.importFromText(text)
                    pendingImportText = null
                }

                RouteAlertAppRoot(
                    uiState = ui,
                    onAddPointAt = vm::prepareNewPoint,
                    onSavePoint = vm::savePoint,
                    onDeletePoint = vm::deletePoint,
                    onToggleEnabled = vm::setEnabled,
                    onSelectPoint = vm::selectPoint,
                    onClearSelection = vm::clearSelection,
                    onStartTrip = { requestStartTrip() },
                    onStopTrip = { stopTripService() },
                    onOpenAppSettings = { openAppSettings() },
                    onOpenBatterySettings = { openBatterySettings() },
                    onOpenLocationSettings = { openLocationSettings() },
                    onOpenFullScreenIntentSettings = { openFullScreenIntentSettings() },
                    onThemeMode = vm::setThemeMode,
                    onMapNightMode = vm::setMapNightMode,
                    onDraftChange = vm::updateDraft,
                    onOpenSearch = vm::openSearch,
                    onCloseSearch = vm::closeSearch,
                    onPlacePicked = vm::preparePointFromSearch,
                    onShowPlaceOnMap = vm::showPlaceOnMap,
                    onFocusDraftOnMap = vm::focusDraftOnMap,
                    onOpenShare = vm::openShare,
                    onCloseShare = vm::closeShare,
                    onOpenPaywall = vm::openPaywall,
                    onClosePaywall = vm::closePaywall,
                    billingProvider = vm.subscriptionManager.billingProvider,
                    onPurchaseSuccess = vm::onPurchaseSuccess,
                    onResetTrial = vm::resetTrialForTesting,
                    onSetPro = vm::setPro,
                    onClearImportMessage = vm::clearImportMessage,
                    onImportText = vm::importFromText
                )
            }
        }
        window.decorView.post { beginPermissionSetup() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureImportFromIntent(intent)
    }

    private fun captureImportFromIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!text.isNullOrBlank()) pendingImportText = text
            }
            else -> {
                intent.data?.let { pendingImportUri = it }
            }
        }
    }

    private fun beginPermissionSetup() {
        if (setupStep != SetupStep.Idle) return
        val needed = missingRuntimePermissions()
        if (needed.isNotEmpty()) {
            setupStep = SetupStep.Runtime
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            continueAfterRuntimePermissions()
        }
    }

    private fun continueAfterRuntimePermissions() {
        if (!canUseFullScreenIntent()) {
            setupStep = SetupStep.FullScreen
            openFullScreenIntentSettings()
        } else {
            continueAfterFullScreen()
        }
    }

    private fun continueAfterFullScreen() {
        if (!isIgnoringBatteryOptimizations()) {
            setupStep = SetupStep.Battery
            openBatterySettings()
        } else {
            setupStep = SetupStep.Done
        }
    }

    private fun missingRuntimePermissions(): List<String> {
        val needed = mutableListOf<String>()
        if (!hasFineLocation()) {
            needed += Manifest.permission.ACCESS_FINE_LOCATION
            needed += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= 33 && !hasNotifications()) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        return needed
    }

    private fun requestStartTrip() {
        val needed = missingRuntimePermissions()
        if (needed.isNotEmpty()) {
            pendingStartTrip = true
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            ensureFullScreenIntentThenStart()
        }
    }

    private fun ensureFullScreenIntentThenStart() {
        // Trip uses a location foreground service (persistent notification), not "always" background GPS.
        if (!canUseFullScreenIntent()) {
            openFullScreenIntentSettings()
        }
        startTripService()
    }

    private fun startTripService() {
        TripTrackingService.resetSession()
        val intent = Intent(this, TripTrackingService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopTripService() {
        stopService(Intent(this, TripTrackingService::class.java))
    }

    private fun hasFineLocation(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun canUseFullScreenIntent(): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        val nm = getSystemService(NotificationManager::class.java) ?: return true
        return nm.canUseFullScreenIntent()
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(PowerManager::class.java) ?: return true
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun openAppSettings() = SettingsIntents.openAppDetails(this)

    private fun openBatterySettings() = SettingsIntents.openBatteryOptimizations(this)

    private fun openLocationSettings() = SettingsIntents.openLocation(this)

    private fun openFullScreenIntentSettings() = SettingsIntents.openFullScreenIntent(this)
}
