package ru.routealert.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "route_points")
data class RoutePoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** Radius in meters, clamped 10..10000 in UI/repository. */
    val radiusMeters: Float = 200f,
    val alertType: AlertType = AlertType.BOTH,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
