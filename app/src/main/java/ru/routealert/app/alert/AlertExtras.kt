package ru.routealert.app.alert

object AlertExtras {
    const val EXTRA_POINT_ID = "extra_point_id"
    const val EXTRA_POINT_NAME = "extra_point_name"
    const val EXTRA_ALERT_TYPE = "extra_alert_type"
    const val EXTRA_DISTANCE_M = "extra_distance_m"

    const val ACTION_DISMISS = "ru.routealert.app.action.DISMISS"
    const val ACTION_SNOOZE = "ru.routealert.app.action.SNOOZE"
    const val ACTION_DISABLE_POINT = "ru.routealert.app.action.DISABLE_POINT"
    const val ACTION_STOP_TRIP = "ru.routealert.app.action.STOP_TRIP"

    const val CHANNEL_TRIP = "trip_tracking"
    const val CHANNEL_ALERT = "point_alerts"
    const val CHANNEL_ALARM = "point_alarms"

    const val NOTIFICATION_ID_TRIP = 1001
    const val NOTIFICATION_ID_ALERT_BASE = 2000

    const val SNOOZE_MINUTES = 2
}
