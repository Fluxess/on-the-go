package ru.routealert.app.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.routealert.app.alert.AlertExtras
import ru.routealert.app.alert.NotificationHelper
import ru.routealert.app.data.RoutePoint
import ru.routealert.app.data.RoutePointRepository
import ru.routealert.app.location.GeoUtils

class TripTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: RoutePointRepository
    private lateinit var notifications: NotificationHelper
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private var points: List<RoutePoint> = emptyList()
    private var pointsJob: Job? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            onLocation(location.latitude, location.longitude, location.accuracy)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = RoutePointRepository.get(this)
        notifications = NotificationHelper(this)
        notifications.ensureChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val initial = notifications.buildTripNotification(0, null, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                AlertExtras.NOTIFICATION_ID_TRIP,
                initial,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(AlertExtras.NOTIFICATION_ID_TRIP, initial)
        }

        TripStateHolder.update { it.copy(isActive = true) }
        observePoints()
        startLocationUpdates()
        return START_STICKY
    }

    private fun observePoints() {
        pointsJob?.cancel()
        pointsJob = scope.launch {
            repository.observeEnabled().collectLatest { list ->
                points = list
                TripStateHolder.update { it.copy(enabledPointCount = list.size) }
                updateTripNotification(force = true)
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_500L)
            .setMinUpdateIntervalMillis(800L)
            .setMinUpdateDistanceMeters(3f)
            .setWaitForAccurateLocation(false)
            .build()

        fused.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun onLocation(lat: Double, lon: Double, accuracy: Float) {
        val nearest = GeoUtils.nearest(lat, lon, points)
        TripStateHolder.update {
            it.copy(
                isActive = true,
                latitude = lat,
                longitude = lon,
                accuracyMeters = accuracy,
                enabledPointCount = points.size,
                nearestName = nearest?.point?.name,
                nearestDistanceMeters = nearest?.distanceMeters
            )
        }
        updateTripNotification(force = false)

        val now = System.currentTimeMillis()
        for (point in points) {
            val inside = GeoUtils.isInsideRadius(lat, lon, point)
            if (!inside) {
                if (GeoUtils.isOutsideHysteresis(lat, lon, point)) {
                    // allow re-arm only after leaving hysteresis; still respect session trigger
                }
                continue
            }

            val snoozeUntil = snoozedUntil[point.id] ?: 0L
            if (now < snoozeUntil) continue
            if (triggeredThisSession.contains(point.id)) continue

            triggeredThisSession.add(point.id)
            val distance = GeoUtils.distanceMeters(lat, lon, point.latitude, point.longitude)
            notifications.notifyPointReached(point, distance)
        }
    }

    private var lastNotifiedDistance: Float? = null
    private var lastNotifiedName: String? = null

    private fun updateTripNotification(force: Boolean) {
        val state = TripStateHolder.state.value
        val lat = state.latitude
        val lon = state.longitude
        val lines = if (lat != null && lon != null) {
            points
                .map { p ->
                    val d = GeoUtils.distanceMeters(lat, lon, p.latitude, p.longitude)
                    p to d
                }
                .sortedBy { it.second }
                .take(4)
                .map { (p, d) -> "• ${p.name}: ${NotificationHelper.formatDistance(d)}" }
        } else {
            emptyList()
        }

        val dist = state.nearestDistanceMeters
        val name = state.nearestName
        val shouldPush = force || when {
            name != lastNotifiedName -> true
            dist == null || lastNotifiedDistance == null -> true
            kotlin.math.abs(dist - lastNotifiedDistance!!) >= 8f -> true
            else -> false
        }
        if (!shouldPush) return

        lastNotifiedDistance = dist
        lastNotifiedName = name
        val notification = notifications.buildTripNotification(
            enabledCount = state.enabledPointCount,
            nearestName = name,
            nearestDistanceMeters = dist,
            lines = lines
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(AlertExtras.NOTIFICATION_ID_TRIP, notification)
    }

    override fun onDestroy() {
        fused.removeLocationUpdates(locationCallback)
        pointsJob?.cancel()
        scope.cancel()
        TripStateHolder.setInactive()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "ru.routealert.app.action.SERVICE_STOP"

        private val triggeredThisSession = mutableSetOf<Long>()
        private val snoozedUntil = mutableMapOf<Long, Long>()

        fun markTriggered(pointId: Long) {
            triggeredThisSession.add(pointId)
            snoozedUntil.remove(pointId)
        }

        fun snooze(pointId: Long, minutes: Int) {
            triggeredThisSession.remove(pointId)
            snoozedUntil[pointId] = System.currentTimeMillis() + minutes * 60_000L
        }

        fun resetSession() {
            triggeredThisSession.clear()
            snoozedUntil.clear()
        }

        fun isRunning(stateActive: Boolean): Boolean = stateActive
    }
}
