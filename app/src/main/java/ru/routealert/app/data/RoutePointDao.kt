package ru.routealert.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutePointDao {
    @Query("SELECT * FROM route_points ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RoutePoint>>

    @Query("SELECT * FROM route_points WHERE enabled = 1 ORDER BY createdAt DESC")
    fun observeEnabled(): Flow<List<RoutePoint>>

    @Query("SELECT * FROM route_points WHERE enabled = 1")
    suspend fun getEnabled(): List<RoutePoint>

    @Query("SELECT * FROM route_points WHERE id = :id")
    suspend fun getById(id: Long): RoutePoint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: RoutePoint): Long

    @Update
    suspend fun update(point: RoutePoint)

    @Delete
    suspend fun delete(point: RoutePoint)

    @Query("DELETE FROM route_points WHERE id = :id")
    suspend fun deleteById(id: Long)
}
