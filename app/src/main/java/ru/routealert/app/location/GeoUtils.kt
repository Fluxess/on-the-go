package ru.routealert.app.location

import android.location.Location
import ru.routealert.app.data.RoutePoint

object GeoUtils {
    fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun isInsideRadius(userLat: Double, userLon: Double, point: RoutePoint): Boolean {
        return distanceMeters(userLat, userLon, point.latitude, point.longitude) <= point.radiusMeters
    }

    /** Exit threshold to clear hysteresis (1.25x radius). */
    fun isOutsideHysteresis(userLat: Double, userLon: Double, point: RoutePoint): Boolean {
        return distanceMeters(userLat, userLon, point.latitude, point.longitude) > point.radiusMeters * 1.25f
    }

    data class Nearest(
        val point: RoutePoint,
        val distanceMeters: Float
    )

    fun nearest(userLat: Double, userLon: Double, points: List<RoutePoint>): Nearest? {
        return points.minByOrNull {
            distanceMeters(userLat, userLon, it.latitude, it.longitude)
        }?.let {
            Nearest(it, distanceMeters(userLat, userLon, it.latitude, it.longitude))
        }
    }
}
