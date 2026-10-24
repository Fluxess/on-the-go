package ru.routealert.app

import android.app.Application
import android.util.Log
import com.yandex.mapkit.MapKitFactory
import ru.routealert.app.alert.NotificationHelper

class RouteAlertApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            MapKitFactory.setApiKey(BuildConfig.YANDEX_MAPKIT_API_KEY)
            MapKitFactory.initialize(this)
        } catch (t: Throwable) {
            Log.e("OnTheGo", "MapKit init failed. Check YANDEX_MAPKIT_API_KEY in local.properties", t)
        }
        NotificationHelper(this).ensureChannels()
    }
}
