package ru.routealert.app.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.routealert.app.data.RoutePointRepository
import ru.routealert.app.service.TripTrackingService

class AlertActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            AlertExtras.ACTION_STOP_TRIP -> {
                context.stopService(Intent(context, TripTrackingService::class.java))
            }
            AlertExtras.ACTION_DISMISS -> {
                val pointId = intent.getLongExtra(AlertExtras.EXTRA_POINT_ID, -1L)
                if (pointId >= 0) {
                    NotificationHelper(context).cancelPointNotification(pointId)
                    TripTrackingService.markTriggered(pointId)
                    context.sendBroadcast(
                        Intent(AlarmActivity.ACTION_CLOSE_ALARM).setPackage(context.packageName)
                    )
                }
            }
            AlertExtras.ACTION_SNOOZE -> {
                val pointId = intent.getLongExtra(AlertExtras.EXTRA_POINT_ID, -1L)
                if (pointId >= 0) {
                    NotificationHelper(context).cancelPointNotification(pointId)
                    TripTrackingService.snooze(pointId, AlertExtras.SNOOZE_MINUTES)
                    context.sendBroadcast(
                        Intent(AlarmActivity.ACTION_CLOSE_ALARM).setPackage(context.packageName)
                    )
                }
            }
            AlertExtras.ACTION_DISABLE_POINT -> {
                val pointId = intent.getLongExtra(AlertExtras.EXTRA_POINT_ID, -1L)
                if (pointId >= 0) {
                    NotificationHelper(context).cancelPointNotification(pointId)
                    TripTrackingService.markTriggered(pointId)
                    context.sendBroadcast(
                        Intent(AlarmActivity.ACTION_CLOSE_ALARM).setPackage(context.packageName)
                    )
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            RoutePointRepository.get(context).setEnabled(pointId, false)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
