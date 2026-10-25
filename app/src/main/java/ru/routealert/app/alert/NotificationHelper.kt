package ru.routealert.app.alert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.routealert.app.MainActivity
import ru.routealert.app.R
import ru.routealert.app.data.AlertType
import ru.routealert.app.data.RoutePoint

class NotificationHelper(private val context: Context) {

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val trip = NotificationChannel(
            AlertExtras.CHANNEL_TRIP,
            context.getString(R.string.trip_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Постоянное уведомление активной поездки"
            setShowBadge(false)
        }

        val alert = NotificationChannel(
            AlertExtras.CHANNEL_ALERT,
            context.getString(R.string.alert_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Оповещения при приближении к точке"
            enableVibration(true)
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val alarm = NotificationChannel(
            AlertExtras.CHANNEL_ALARM,
            context.getString(R.string.alarm_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Будильник при приближении к точке"
            // Sound is played by AlarmActivity's MediaPlayer — keep channel silent
            // to avoid double audio when full-screen intent fires.
            enableVibration(false)
            setBypassDnd(true)
            setSound(null, null)
        }

        val trial = NotificationChannel(
            AlertExtras.CHANNEL_TRIAL,
            "Напоминания о подписке",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Напоминания об окончании бесплатного периода Pro"
            setShowBadge(true)
        }

        manager.createNotificationChannels(listOf(trip, alert, alarm, trial))
    }

    fun buildTripNotification(
        enabledCount: Int,
        nearestName: String?,
        nearestDistanceMeters: Float?,
        lines: List<String> = emptyList()
    ): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, AlertActionReceiver::class.java).setAction(AlertExtras.ACTION_STOP_TRIP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (nearestName != null && nearestDistanceMeters != null) {
            "$nearestName · ${formatDistance(nearestDistanceMeters)}"
        } else {
            context.getString(R.string.trip_active_title)
        }
        val text = context.getString(R.string.trip_active_text, enabledCount)
        val big = buildString {
            append(text)
            if (nearestName != null && nearestDistanceMeters != null) {
                append("\nБлижайшая: ")
                append(nearestName)
                append(" — ")
                append(formatDistance(nearestDistanceMeters))
            }
            lines.forEach { line ->
                append('\n')
                append(line)
            }
        }

        return NotificationCompat.Builder(context, AlertExtras.CHANNEL_TRIP)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setSubText("on-the-go")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp)
            .addAction(0, context.getString(R.string.stop_trip), stopIntent)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setShowWhen(false)
            .build()
    }

    fun notifyPointReached(point: RoutePoint, distanceMeters: Float) {
        val useAlarm = point.alertType == AlertType.ALARM || point.alertType == AlertType.BOTH
        val useNotification =
            point.alertType == AlertType.NOTIFICATION || point.alertType == AlertType.BOTH

        if (useAlarm) {
            showAlarm(point, distanceMeters)
        }
        if (useNotification && !useAlarm) {
            showAlertNotification(point, distanceMeters, fullScreen = false)
        } else if (useNotification && useAlarm) {
            // Alarm path already posts a high-priority full-screen notification.
        }
    }

    private fun showAlarm(point: RoutePoint, distanceMeters: Float) {
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlertExtras.EXTRA_POINT_ID, point.id)
            putExtra(AlertExtras.EXTRA_POINT_NAME, point.name)
            putExtra(AlertExtras.EXTRA_ALERT_TYPE, point.alertType.name)
            putExtra(AlertExtras.EXTRA_DISTANCE_M, distanceMeters)
        }

        val fullScreenPi = PendingIntent.getActivity(
            context,
            point.id.toInt(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Also launch activity directly when app can start activities.
        runCatching { context.startActivity(alarmIntent) }

        showAlertNotification(point, distanceMeters, fullScreen = true, fullScreenPi = fullScreenPi)
    }

    private fun showAlertNotification(
        point: RoutePoint,
        distanceMeters: Float,
        fullScreen: Boolean,
        fullScreenPi: PendingIntent? = null
    ) {
        val channel = if (fullScreen) AlertExtras.CHANNEL_ALARM else AlertExtras.CHANNEL_ALERT
        val dismiss = PendingIntent.getBroadcast(
            context,
            (point.id * 10).toInt(),
            Intent(context, AlertActionReceiver::class.java).apply {
                action = AlertExtras.ACTION_DISMISS
                putExtra(AlertExtras.EXTRA_POINT_ID, point.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snooze = PendingIntent.getBroadcast(
            context,
            (point.id * 10 + 1).toInt(),
            Intent(context, AlertActionReceiver::class.java).apply {
                action = AlertExtras.ACTION_SNOOZE
                putExtra(AlertExtras.EXTRA_POINT_ID, point.id)
                putExtra(AlertExtras.EXTRA_POINT_NAME, point.name)
                putExtra(AlertExtras.EXTRA_ALERT_TYPE, point.alertType.name)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val disablePoint = PendingIntent.getBroadcast(
            context,
            (point.id * 10 + 3).toInt(),
            Intent(context, AlertActionReceiver::class.java).apply {
                action = AlertExtras.ACTION_DISABLE_POINT
                putExtra(AlertExtras.EXTRA_POINT_ID, point.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentPi = PendingIntent.getActivity(
            context,
            (point.id * 10 + 2).toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distanceText = formatDistance(distanceMeters)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.point_reached_title))
            .setContentText("${point.name} · $distanceText")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (fullScreen) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_NAVIGATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .addAction(0, context.getString(R.string.dismiss), dismiss)
            .addAction(0, context.getString(R.string.snooze), snooze)
            .addAction(0, context.getString(R.string.disable_point), disablePoint)

        if (fullScreen && fullScreenPi != null) {
            builder.setFullScreenIntent(fullScreenPi, true)
            // AlarmActivity plays its own sound via MediaPlayer — silence the notification
            // channel sound to prevent two alarms playing simultaneously.
            builder.setSound(null)
            builder.setVibrate(null)
        }

        NotificationManagerCompat.from(context)
            .notify(AlertExtras.NOTIFICATION_ID_ALERT_BASE + point.id.toInt(), builder.build())
    }

    fun showTrialReminderNotification(daysLeft: Int) {
        val openApp = PendingIntent.getActivity(
            context,
            3001,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "Осталось $daysLeft дн. полного доступа. Не забудьте выбрать удобный тариф, чтобы будильник в дороге остался активным."
        val notification = NotificationCompat.Builder(context, AlertExtras.CHANNEL_TRIAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Бесплатный период on-the-go Pro")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        NotificationManagerCompat.from(context)
            .notify(AlertExtras.NOTIFICATION_ID_TRIAL_REMINDER, notification)
    }

    fun cancelPointNotification(pointId: Long) {
        NotificationManagerCompat.from(context)
            .cancel(AlertExtras.NOTIFICATION_ID_ALERT_BASE + pointId.toInt())
    }

    companion object {
        fun formatDistance(meters: Float): String {
            return if (meters >= 1000f) {
                String.format("%.1f км", meters / 1000f)
            } else {
                "${meters.toInt()} м"
            }
        }
    }
}
