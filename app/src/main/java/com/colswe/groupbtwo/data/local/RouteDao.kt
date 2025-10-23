package com.colswe.groupbtwo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes")
    suspend fun getAllRoutes(): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE id = :routeId")
    suspend fun getRouteById(routeId: String): RouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<RouteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity)

    @Query("DELETE FROM routes")
    suspend fun deleteAllRoutes()

    @Query("SELECT COUNT(*) FROM routes")
    suspend fun getRoutesCount(): Int

    @Query("SELECT lastUpdated FROM routes ORDER BY lastUpdated DESC LIMIT 1")
    suspend fun getLastUpdateTime(): Long?
}