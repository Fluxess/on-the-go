package ru.routealert.app.subscription

import android.app.Activity
import kotlinx.coroutines.delay

interface BillingProvider {
    val providerName: String

    suspend fun launchPurchaseFlow(
        activity: Activity,
        plan: SubscriptionPlan
    ): PurchaseResult
}

class MockBillingProvider : BillingProvider {
    override val providerName: String = "Тестовый провайдер (Mock Billing)"

    override suspend fun launchPurchaseFlow(
        activity: Activity,
        plan: SubscriptionPlan
    ): PurchaseResult {
        delay(600) // Имитация обработки запроса платежной системы
        return PurchaseResult.Success(plan)
    }
}
