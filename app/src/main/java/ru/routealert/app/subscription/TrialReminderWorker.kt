package ru.routealert.app.subscription

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ru.routealert.app.alert.NotificationHelper
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Worker, отправляющий push-уведомление пользователю за 2 дня до окончания триала (на 5-й день).
 */
class TrialReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val manager = SubscriptionManager.get(context)
        val state = manager.subscriptionState.value

        if (state.status is SubscriptionStatus.TrialActive) {
            val daysLeft = state.status.daysLeft
            NotificationHelper(context).showTrialReminderNotification(daysLeft)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "trial_day_5_reminder"

        fun scheduleIfRequired(context: Context, trialStartTimeMs: Long) {
            val now = System.currentTimeMillis()
            // 5 дней с момента старта
            val targetTimeMs = trialStartTimeMs + (5L * 24L * 60L * 60L * 1000L)
            val delayMs = targetTimeMs - now

            // Если время уже прошло или триал завершился, не планируем
            if (delayMs < 0) return

            val request = OneTimeWorkRequestBuilder<TrialReminderWorker>()
                .setInitialDelay(max(1000L, delayMs), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
