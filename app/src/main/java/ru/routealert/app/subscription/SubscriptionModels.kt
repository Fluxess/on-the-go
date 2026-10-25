package ru.routealert.app.subscription

enum class SubscriptionPlan(
    val id: String,
    val title: String,
    val periodDescription: String,
    val priceText: String,
    val badge: String? = null
) {
    MONTHLY(
        id = "route_alert_pro_monthly",
        title = "1 месяц",
        periodDescription = "Ежемесячное продление",
        priceText = "149 ₽"
    ),
    YEARLY(
        id = "route_alert_pro_yearly",
        title = "1 год (Выгодно)",
        periodDescription = "99 ₽ / месяц при оплате за год",
        priceText = "1 190 ₽",
        badge = "-33%"
    ),
    LIFETIME(
        id = "route_alert_pro_lifetime",
        title = "Навсегда",
        periodDescription = "Один платёж, вечный доступ",
        priceText = "2 490 ₽",
        badge = "Хит"
    )
}

sealed interface SubscriptionStatus {
    data class TrialActive(val daysLeft: Int, val endsAtTimestampMs: Long) : SubscriptionStatus
    data class ProPurchased(val planId: String, val purchasedAtTimestampMs: Long) : SubscriptionStatus
    data object ExpiredFree : SubscriptionStatus
}

data class SubscriptionState(
    val status: SubscriptionStatus,
    val isProUnlocked: Boolean
)

sealed interface PurchaseResult {
    data class Success(val plan: SubscriptionPlan) : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Error(val message: String) : PurchaseResult
}
