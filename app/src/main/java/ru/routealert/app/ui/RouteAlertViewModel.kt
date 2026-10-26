package ru.routealert.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.routealert.app.analytics.AnalyticsManager
import ru.routealert.app.analytics.AppAnalyticsData
import ru.routealert.app.data.AlertType
import ru.routealert.app.data.PreferencesRepository
import ru.routealert.app.data.RoutePoint
import ru.routealert.app.data.RoutePointRepository
import ru.routealert.app.data.ThemeMode
import ru.routealert.app.location.PlaceHit
import ru.routealert.app.service.TripStateHolder
import ru.routealert.app.service.TripUiState
import ru.routealert.app.share.PointShare
import ru.routealert.app.share.SharedPoint
import ru.routealert.app.subscription.SubscriptionManager
import ru.routealert.app.subscription.SubscriptionPlan
import ru.routealert.app.subscription.SubscriptionState
import ru.routealert.app.subscription.SubscriptionStatus

data class PointDraft(
    val id: Long = 0,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Float = RoutePointRepository.RECOMMENDED_RADIUS_M,
    val alertType: AlertType = AlertType.BOTH,
    val enabled: Boolean = true,
    val isNew: Boolean = true
)

data class PermissionHints(
    val hasFineLocation: Boolean = false,
    val hasNotifications: Boolean = true,
    val ignoringBattery: Boolean = false,
    val canFullScreenIntent: Boolean = true
)

data class MapFocusRequest(
    val latitude: Double,
    val longitude: Double,
    val tick: Int
)

data class RouteAlertUiState(
    val points: List<RoutePoint> = emptyList(),
    val trip: TripUiState = TripUiState(),
    val draft: PointDraft? = null,
    val helpSeen: Boolean = false,
    val defaultAlertType: AlertType = AlertType.BOTH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val mapNightMode: Boolean = false,
    val isPro: Boolean = true,
    val subscriptionState: SubscriptionState = SubscriptionState(
        status = SubscriptionStatus.TrialActive(7, 0L),
        isProUnlocked = true
    ),
    val showSearch: Boolean = false,
    val showShare: Boolean = false,
    val showPaywall: Boolean = false,
    val showSecretAdmin: Boolean = false,
    val analytics: AppAnalyticsData = AppAnalyticsData(0, 0L, 0L, 0, 0, 0, 0, 0, 0, "Нет"),
    val importMessage: String? = null,
    val mapFocus: MapFocusRequest? = null,
    val permissions: PermissionHints = PermissionHints()
)

class RouteAlertViewModel(
    private val pointsRepo: RoutePointRepository,
    private val prefsRepo: PreferencesRepository,
    val subscriptionManager: SubscriptionManager,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val draft = MutableStateFlow<PointDraft?>(null)
    private val permissions = MutableStateFlow(PermissionHints())
    private val showSearch = MutableStateFlow(false)
    private val showShare = MutableStateFlow(false)
    private val showPaywall = MutableStateFlow(false)
    private val showSecretAdmin = MutableStateFlow(false)
    private val importMessage = MutableStateFlow<String?>(null)
    private val mapFocus = MutableStateFlow<MapFocusRequest?>(null)

    init {
        viewModelScope.launch {
            analyticsManager.trackAppLaunch()
        }
    }

    private data class PrefBundle(
        val helpSeen: Boolean,
        val defaultAlert: AlertType,
        val themeMode: ThemeMode,
        val mapNightMode: Boolean,
        val isPro: Boolean
    )

    private val prefsFlow = combine(
        prefsRepo.helpSeen,
        prefsRepo.defaultAlertType,
        prefsRepo.themeMode,
        prefsRepo.mapNightMode,
        subscriptionManager.subscriptionState
    ) { seen, type, theme, mapNight, subState ->
        Triple(PrefBundle(seen, type, theme, mapNight, subState.isProUnlocked), subState, Unit)
    }

    val uiState: StateFlow<RouteAlertUiState> = combine(
        combine(
            pointsRepo.observeAll(),
            TripStateHolder.state,
            draft,
            prefsFlow,
            permissions
        ) { points, trip, draftValue, (prefBundle, subState), perm ->
            Triple(Triple(points, trip, draftValue), prefBundle to subState, perm)
        },
        combine(
            combine(showSearch, showShare, showPaywall) { s, sh, p -> Triple(s, sh, p) },
            combine(showSecretAdmin, importMessage, mapFocus) { sa, msg, f -> Triple(sa, msg, f) },
            analyticsManager.analyticsFlow
        ) { (search, share, paywall), (secretAdmin, msg, focus), analytics ->
            UiFlags(search, share, paywall, secretAdmin, msg, focus, analytics)
        }
    ) { core, flags ->
        val (bundle, prefAndSub, perm) = core
        val (points, trip, draftValue) = bundle
        val (prefs, subState) = prefAndSub
        RouteAlertUiState(
            points = points,
            trip = trip,
            draft = draftValue,
            helpSeen = prefs.helpSeen,
            defaultAlertType = prefs.defaultAlert,
            themeMode = prefs.themeMode,
            mapNightMode = prefs.mapNightMode,
            isPro = subState.isProUnlocked,
            subscriptionState = subState,
            showSearch = flags.showSearch,
            showShare = flags.showShare,
            showPaywall = flags.showPaywall,
            showSecretAdmin = flags.showSecretAdmin,
            analytics = flags.analytics,
            importMessage = flags.importMessage,
            mapFocus = flags.mapFocus,
            permissions = perm
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RouteAlertUiState())

    private data class UiFlags(
        val showSearch: Boolean,
        val showShare: Boolean,
        val showPaywall: Boolean,
        val showSecretAdmin: Boolean,
        val importMessage: String?,
        val mapFocus: MapFocusRequest?,
        val analytics: AppAnalyticsData
    )

    fun prepareNewPoint(lat: Double, lon: Double) {
        draft.value = PointDraft(
            name = "Точка ${(uiState.value.points.size + 1)}",
            latitude = lat,
            longitude = lon,
            radiusMeters = RoutePointRepository.RECOMMENDED_RADIUS_M,
            alertType = uiState.value.defaultAlertType,
            isNew = true
        )
    }

    fun preparePointFromSearch(hit: PlaceHit) {
        draft.value = PointDraft(
            name = hit.title,
            latitude = hit.latitude,
            longitude = hit.longitude,
            radiusMeters = RoutePointRepository.RECOMMENDED_RADIUS_M,
            alertType = uiState.value.defaultAlertType,
            isNew = true
        )
        showSearch.value = false
        focusMapOn(hit.latitude, hit.longitude)
    }

    fun showPlaceOnMap(hit: PlaceHit) {
        showSearch.value = false
        focusMapOn(hit.latitude, hit.longitude)
    }

    fun focusMapOn(latitude: Double, longitude: Double) {
        mapFocus.update { prev ->
            MapFocusRequest(
                latitude = latitude,
                longitude = longitude,
                tick = (prev?.tick ?: 0) + 1
            )
        }
    }

    fun focusDraftOnMap() {
        val d = draft.value ?: return
        focusMapOn(d.latitude, d.longitude)
        draft.value = null
    }

    fun selectPoint(point: RoutePoint) {
        draft.value = PointDraft(
            id = point.id,
            name = point.name,
            latitude = point.latitude,
            longitude = point.longitude,
            radiusMeters = point.radiusMeters,
            alertType = point.alertType,
            enabled = point.enabled,
            isNew = false
        )
    }

    fun clearSelection() {
        draft.value = null
    }

    fun updateDraft(transform: (PointDraft) -> PointDraft) {
        draft.update { current -> current?.let(transform) }
    }

    fun savePoint() {
        val current = draft.value ?: return
        viewModelScope.launch {
            pointsRepo.upsert(
                RoutePoint(
                    id = current.id,
                    name = current.name,
                    latitude = current.latitude,
                    longitude = current.longitude,
                    radiusMeters = current.radiusMeters,
                    alertType = current.alertType,
                    enabled = current.enabled
                )
            )
            if (current.isNew) {
                analyticsManager.trackPointCreated()
            }
            prefsRepo.setDefaultAlertType(current.alertType)
            draft.value = null
        }
    }

    fun deletePoint(point: RoutePoint) {
        viewModelScope.launch {
            pointsRepo.delete(point)
            if (draft.value?.id == point.id) draft.value = null
        }
    }

    fun setEnabled(point: RoutePoint, enabled: Boolean) {
        viewModelScope.launch {
            pointsRepo.setEnabled(point.id, enabled)
        }
    }

    fun markHelpSeen() {
        viewModelScope.launch { prefsRepo.setHelpSeen(true) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsRepo.setThemeMode(mode)
            when (mode) {
                ThemeMode.LIGHT -> prefsRepo.setMapNightMode(false)
                ThemeMode.DARK -> prefsRepo.setMapNightMode(true)
                ThemeMode.SYSTEM -> Unit
            }
        }
    }

    fun setMapNightMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setMapNightMode(enabled) }
    }

    fun setPro(enabled: Boolean) {
        viewModelScope.launch {
            subscriptionManager.setProDirectly(enabled)
            prefsRepo.setPro(enabled)
        }
    }

    fun openPaywall() {
        showPaywall.value = true
        viewModelScope.launch {
            analyticsManager.trackPaywallView()
        }
    }

    fun closePaywall() {
        showPaywall.value = false
    }

    fun openSecretAdmin() {
        showSecretAdmin.value = true
    }

    fun closeSecretAdmin() {
        showSecretAdmin.value = false
    }

    fun resetAnalytics() {
        viewModelScope.launch {
            analyticsManager.resetStats()
        }
    }

    fun onPurchaseSuccess(plan: SubscriptionPlan) {
        viewModelScope.launch {
            subscriptionManager.activatePlan(plan)
            prefsRepo.setPro(true)
            analyticsManager.trackPurchase(plan.id)
        }
    }

    fun resetTrialForTesting() {
        viewModelScope.launch {
            subscriptionManager.resetTrialForTesting()
            prefsRepo.setPro(true)
        }
    }

    fun openSearch() {
        showSearch.value = true
    }

    fun closeSearch() {
        showSearch.value = false
    }

    fun openShare() {
        showShare.value = true
        viewModelScope.launch {
            analyticsManager.trackShare()
        }
    }

    fun closeShare() {
        showShare.value = false
    }

    fun clearImportMessage() {
        importMessage.value = null
    }

    fun importFromUri(uri: Uri?) {
        if (uri == null) return
        val shared = PointShare.parse(uri)
        importShared(shared)
    }

    fun importFromText(text: String) {
        importShared(PointShare.parse(text))
    }

    private fun importShared(shared: List<SharedPoint>) {
        if (shared.isEmpty()) {
            importMessage.value = "Не удалось прочитать точки из ссылки"
            return
        }
        viewModelScope.launch {
            shared.forEach { s ->
                pointsRepo.upsert(
                    RoutePoint(
                        name = s.name,
                        latitude = s.latitude,
                        longitude = s.longitude,
                        radiusMeters = s.radiusMeters,
                        alertType = s.alertType,
                        enabled = true
                    )
                )
            }
            importMessage.value = "Импортировано точек: ${shared.size}"
            val target = shared.first()
            focusMapOn(target.latitude, target.longitude)
        }
    }

    fun refreshPermissionHints(
        hasFineLocation: Boolean,
        hasNotifications: Boolean,
        ignoringBattery: Boolean,
        canFullScreenIntent: Boolean
    ) {
        permissions.value = PermissionHints(
            hasFineLocation = hasFineLocation,
            hasNotifications = hasNotifications,
            ignoringBattery = ignoringBattery,
            canFullScreenIntent = canFullScreenIntent
        )
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RouteAlertViewModel(
                        RoutePointRepository.get(app),
                        PreferencesRepository.get(app),
                        SubscriptionManager.get(app),
                        AnalyticsManager.get(app)
                    ) as T
                }
            }
    }
}
