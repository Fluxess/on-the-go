package ru.routealert.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class RoutePointRepository(context: Context) {
    private val dao = AppDatabase.get(context).routePointDao()

    fun observeAll(): Flow<List<RoutePoint>> = dao.observeAll()

    fun observeEnabled(): Flow<List<RoutePoint>> = dao.observeEnabled()

    suspend fun getEnabled(): List<RoutePoint> = dao.getEnabled()

    suspend fun getById(id: Long): RoutePoint? = dao.getById(id)

    suspend fun upsert(point: RoutePoint): Long {
        val normalized = point.copy(
            name = point.name.trim().ifEmpty { "Точка" },
            radiusMeters = point.radiusMeters.coerceIn(MIN_RADIUS_M, MAX_RADIUS_M)
        )
        return if (normalized.id == 0L) {
            dao.insert(normalized)
        } else {
            dao.update(normalized)
            normalized.id
        }
    }

    suspend fun delete(point: RoutePoint) = dao.delete(point)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val point = dao.getById(id) ?: return
        dao.update(point.copy(enabled = enabled))
    }

    companion object {
        const val MIN_RADIUS_M = 10f
        const val MAX_RADIUS_M = 10_000f
        const val RECOMMENDED_RADIUS_M = 200f

        @Volatile
        private var instance: RoutePointRepository? = null

        fun get(context: Context): RoutePointRepository {
            return instance ?: synchronized(this) {
                instance ?: RoutePointRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
