package ru.routealert.app.subscription

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.MessageDigest
import kotlin.math.max

private val Context.subscriptionDataStore by preferencesDataStore("route_alert_subscription")

/**
 * Управляет логикой подписки: 7 дней бесплатного Reverse Trial со старта,
 * защитой от сброса и хранением покупки Pro.
 */
class SubscriptionManager(
    private val context: Context,
    val billingProvider: BillingProvider = MockBillingProvider()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _subscriptionState = MutableStateFlow(
        SubscriptionState(
            status = SubscriptionStatus.TrialActive(daysLeft = TRIAL_DAYS, endsAtTimestampMs = 0L),
            isProUnlocked = true
        )
    )
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    init {
        scope.launch {
            initTrialAndState()
        }
    }

    private suspend fun initTrialAndState() {
        val prefs = context.subscriptionDataStore.data.first()
        val now = System.currentTimeMillis()

        val isProPurchased = prefs[KEY_IS_PRO_PURCHASED] ?: false
        val purchasedPlanId = prefs[KEY_PURCHASED_PLAN_ID]

        if (isProPurchased && purchasedPlanId != null) {
            _subscriptionState.value = SubscriptionState(
                status = SubscriptionStatus.ProPurchased(purchasedPlanId, now),
                isProUnlocked = true
            )
            return
        }

        // Двойная защита даты старта: DataStore + резервные SharedPreferences
        val fallbackPrefs = context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        val fallbackStart = fallbackPrefs.getLong(FALLBACK_KEY_TRIAL_START, 0L)
        val storedStart = prefs[KEY_TRIAL_START_TIME] ?: 0L

        val trialStart = when {
            storedStart > 0L -> storedStart
            fallbackStart > 0L -> fallbackStart
            else -> {
                // Первый запуск приложения
                fallbackPrefs.edit().putLong(FALLBACK_KEY_TRIAL_START, now).apply()
                context.subscriptionDataStore.edit {
                    it[KEY_TRIAL_START_TIME] = now
                    it[KEY_DEVICE_FINGERPRINT] = getDeviceFingerprint()
                }
                now
            }
        }

        if (storedStart == 0L && trialStart > 0L) {
            context.subscriptionDataStore.edit { it[KEY_TRIAL_START_TIME] = trialStart }
        }

        val elapsedMs = now - trialStart
        if (elapsedMs < TRIAL_DURATION_MS) {
            val remainingMs = TRIAL_DURATION_MS - elapsedMs
            val daysLeft = max(1, ((remainingMs + DAY_MS - 1) / DAY_MS).toInt())
            val endsAt = trialStart + TRIAL_DURATION_MS
            _subscriptionState.value = SubscriptionState(
                status = SubscriptionStatus.TrialActive(daysLeft, endsAt),
                isProUnlocked = true
            )
        } else {
            _subscriptionState.value = SubscriptionState(
                status = SubscriptionStatus.ExpiredFree,
                isProUnlocked = false
            )
        }
    }

    suspend fun activatePlan(plan: SubscriptionPlan) {
        val now = System.currentTimeMillis()
        context.subscriptionDataStore.edit {
            it[KEY_IS_PRO_PURCHASED] = true
            it[KEY_PURCHASED_PLAN_ID] = plan.id
        }
        _subscriptionState.value = SubscriptionState(
            status = SubscriptionStatus.ProPurchased(plan.id, now),
            isProUnlocked = true
        )
    }

    suspend fun setProDirectly(enabled: Boolean) {
        if (enabled) {
            activatePlan(SubscriptionPlan.YEARLY)
        } else {
            context.subscriptionDataStore.edit {
                it[KEY_IS_PRO_PURCHASED] = false
                it.remove(KEY_PURCHASED_PLAN_ID)
                it[KEY_TRIAL_START_TIME] = System.currentTimeMillis() - TRIAL_DURATION_MS - 1000L
            }
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(FALLBACK_KEY_TRIAL_START, System.currentTimeMillis() - TRIAL_DURATION_MS - 1000L)
                .apply()

            _subscriptionState.value = SubscriptionState(
                status = SubscriptionStatus.ExpiredFree,
                isProUnlocked = false
            )
        }
    }

    suspend fun resetTrialForTesting() {
        val now = System.currentTimeMillis()
        context.subscriptionDataStore.edit {
            it[KEY_IS_PRO_PURCHASED] = false
            it.remove(KEY_PURCHASED_PLAN_ID)
            it[KEY_TRIAL_START_TIME] = now
        }
        context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(FALLBACK_KEY_TRIAL_START, now)
            .apply()

        _subscriptionState.value = SubscriptionState(
            status = SubscriptionStatus.TrialActive(daysLeft = TRIAL_DAYS, endsAtTimestampMs = now + TRIAL_DURATION_MS),
            isProUnlocked = true
        )
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceFingerprint(): String {
        return runCatching {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "default"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(androidId.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        }.getOrDefault("fingerprint_unavailable")
    }

    companion object {
        const val TRIAL_DAYS = 7
        const val DAY_MS = 24L * 60L * 60L * 1000L
        const val TRIAL_DURATION_MS = TRIAL_DAYS * DAY_MS

        private const val FALLBACK_PREFS_NAME = "route_alert_sec_store"
        private const val FALLBACK_KEY_TRIAL_START = "sec_trial_start"

        private val KEY_TRIAL_START_TIME = longPreferencesKey("trial_start_time")
        private val KEY_IS_PRO_PURCHASED = booleanPreferencesKey("is_pro_purchased")
        private val KEY_PURCHASED_PLAN_ID = stringPreferencesKey("purchased_plan_id")
        private val KEY_DEVICE_FINGERPRINT = stringPreferencesKey("device_fingerprint")

        @Volatile
        private var instance: SubscriptionManager? = null

        fun get(context: Context): SubscriptionManager {
            return instance ?: synchronized(this) {
                instance ?: SubscriptionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
