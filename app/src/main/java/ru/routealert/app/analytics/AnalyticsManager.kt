package ru.routealert.app.analytics

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.analyticsDataStore by preferencesDataStore("route_alert_analytics")

data class AppAnalyticsData(
    val appLaunches: Int,
    val firstLaunchTimeMs: Long,
    val lastLaunchTimeMs: Long,
    val totalTripsStarted: Int,
    val totalAlertsTriggered: Int,
    val totalPointsCreated: Int,
    val totalSharesCount: Int,
    val totalPaywallViews: Int,
    val totalPurchasesCount: Int,
    val lastPurchasedPlan: String
)

class AnalyticsManager(private val context: Context) {

    val analyticsFlow: Flow<AppAnalyticsData> = context.analyticsDataStore.data.map { prefs ->
        AppAnalyticsData(
            appLaunches = prefs[KEY_APP_LAUNCHES] ?: 0,
            firstLaunchTimeMs = prefs[KEY_FIRST_LAUNCH] ?: 0L,
            lastLaunchTimeMs = prefs[KEY_LAST_LAUNCH] ?: 0L,
            totalTripsStarted = prefs[KEY_TRIPS_STARTED] ?: 0,
            totalAlertsTriggered = prefs[KEY_ALERTS_TRIGGERED] ?: 0,
            totalPointsCreated = prefs[KEY_POINTS_CREATED] ?: 0,
            totalSharesCount = prefs[KEY_SHARES_COUNT] ?: 0,
            totalPaywallViews = prefs[KEY_PAYWALL_VIEWS] ?: 0,
            totalPurchasesCount = prefs[KEY_PURCHASES_COUNT] ?: 0,
            lastPurchasedPlan = prefs[KEY_LAST_PURCHASED_PLAN] ?: "Нет"
        )
    }

    suspend fun trackAppLaunch() {
        val now = System.currentTimeMillis()
        context.analyticsDataStore.edit { prefs ->
            val count = prefs[KEY_APP_LAUNCHES] ?: 0
            prefs[KEY_APP_LAUNCHES] = count + 1
            if ((prefs[KEY_FIRST_LAUNCH] ?: 0L) == 0L) {
                prefs[KEY_FIRST_LAUNCH] = now
            }
            prefs[KEY_LAST_LAUNCH] = now
        }
    }

    suspend fun trackTripStarted() {
        context.analyticsDataStore.edit { prefs ->
            prefs[KEY_TRIPS_STARTED] = (prefs[KEY_TRIPS_STARTED] ?: 0) + 1
        }
    }

    suspend fun trackAlertTriggered() {
        context.analyticsDataStore.edit { prefs ->
            prefs[KEY_ALERTS_TRIGGERED] = (prefs[KEY_ALERTS_TRIGGERED] ?: 0) + 1
        }
    }

    suspend fun trackPointCreated() {
        context.analyticsDataStore.edit { prefs ->
            prefs[KEY_POINTS_CREATED] = (prefs[KEY_POINTS_CREATED] ?: 0) + 1
        }
    }

    suspend fun trackShare() {
        context.analyticsDataStore.edit { prefs ->
            prefs[KEY_SHARES_COUNT] = (prefs[KEY_SHARES_COUNT] ?: 0) + 1
        }
    }

    suspend fun trackPaywallView() {
        context.analyticsDataStore.edit { prefs ->
            prefs[KEY_PAYWALL_VIEWS] = (prefs[KEY_PAYWALL_VIEWS] ?: 0) + 1
        }
    }

    suspend fun trackPurchase(planId: String) {
        context.analyticsDataStore.edit { prefs ->
            prefs[KEY_PURCHASES_COUNT] = (prefs[KEY_PURCHASES_COUNT] ?: 0) + 1
            prefs[KEY_LAST_PURCHASED_PLAN] = planId
        }
    }

    suspend fun resetStats() {
        context.analyticsDataStore.edit { prefs ->
            prefs.clear()
            prefs[KEY_FIRST_LAUNCH] = System.currentTimeMillis()
            prefs[KEY_LAST_LAUNCH] = System.currentTimeMillis()
        }
    }

    companion object {
        private val KEY_APP_LAUNCHES = intPreferencesKey("app_launches")
        private val KEY_FIRST_LAUNCH = longPreferencesKey("first_launch_time")
        private val KEY_LAST_LAUNCH = longPreferencesKey("last_launch_time")
        private val KEY_TRIPS_STARTED = intPreferencesKey("trips_started")
        private val KEY_ALERTS_TRIGGERED = intPreferencesKey("alerts_triggered")
        private val KEY_POINTS_CREATED = intPreferencesKey("points_created")
        private val KEY_SHARES_COUNT = intPreferencesKey("shares_count")
        private val KEY_PAYWALL_VIEWS = intPreferencesKey("paywall_views")
        private val KEY_PURCHASES_COUNT = intPreferencesKey("purchases_count")
        private val KEY_LAST_PURCHASED_PLAN = stringPreferencesKey("last_purchased_plan")

        @Volatile
        private var instance: AnalyticsManager? = null

        fun get(context: Context): AnalyticsManager {
            return instance ?: synchronized(this) {
                instance ?: AnalyticsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
