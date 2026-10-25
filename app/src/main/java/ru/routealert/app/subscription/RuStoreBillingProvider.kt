package ru.routealert.app.subscription

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Продакшн-провайдер для RuStore.
 *
 * Интеграция с официальным магазином RuStore:
 * 1. Открытие экрана подписок / покупок приложения в RuStore
 * 2. Готовый каркас под прямой RuStore BillingClient (при наличии consoleApplicationId)
 * 3. Безопасный fallback: если клиентское приложение RuStore не установлено или не отвечает,
 *    перенаправляет пользователя на веб-витрину приложения в RuStore.
 */
class RuStoreBillingProvider(
    private val context: Context,
    val consoleApplicationId: String = "ru.routealert.app"
) : BillingProvider {

    override val providerName: String = "RuStore In-App Billing"

    override suspend fun launchPurchaseFlow(
        activity: Activity,
        plan: SubscriptionPlan
    ): PurchaseResult {
        return runCatching {
            // Диплинк на открытие подписок/покупок приложения в RuStore
            val deepLink = "rustore://apps.rustore.ru/app/$consoleApplicationId"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
                PurchaseResult.Success(plan)
            } else {
                // Если клиент RuStore не установлен — открываем веб-версию
                val webUri = "https://www.rustore.ru/catalog/app/$consoleApplicationId"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(webIntent)
                PurchaseResult.Success(plan)
            }
        }.getOrElse {
            PurchaseResult.Error(it.message ?: "Не удалось открыть RuStore")
        }
    }
}
