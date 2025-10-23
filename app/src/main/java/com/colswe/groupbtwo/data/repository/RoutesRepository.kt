package com.colswe.groupbtwo.data.repository

import android.content.Context
import com.colswe.groupbtwo.data.local.AppDatabase
import com.colswe.groupbtwo.data.local.toEntity
import com.colswe.groupbtwo.data.local.toRoute
import com.colswe.groupbtwo.data.model.Route
import com.colswe.groupbtwo.util.Constants
import com.colswe.groupbtwo.util.NetworkUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RoutesRepository(private val context: Context) {
    private val baseUrl = Constants.BASE_URL_ROUTES
    private val database = AppDatabase.getDatabase(context)
    private val routeDao = database.routeDao()

    suspend fun getNearbyRoutes(lat: Double, lng: Double, forceRefresh: Boolean = false): Result<List<Route>> {
        return withContext(Dispatchers.IO) {
            try {
                val hasInternet = NetworkUtil.isNetworkAvailable(context)

                if (!hasInternet) {
                    val cachedRoutes = routeDao.getAllRoutes().map { it.toRoute() }
                    return@withContext if (cachedRoutes.isNotEmpty()) {
                        Result.success(cachedRoutes)
                    } else {
                        Result.failure(Exception("No hay conexión a internet y no hay datos en caché"))
                    }
                }

                try {
                    val urlString = "$baseUrl/routesApi/routes/near?lat=$lat&lng=$lng"
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection

                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    val responseCode = connection.responseCode

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val routes = parseRoutesJson(response)

                        if (routes.isNotEmpty()) {
                            val routeEntities = routes.map { it.toEntity() }
                            routeDao.deleteAllRoutes()
                            routeDao.insertRoutes(routeEntities)
                        }

                        Result.success(routes)
                    } else {
                        val cachedRoutes = routeDao.getAllRoutes().map { it.toRoute() }
                        if (cachedRoutes.isNotEmpty()) {
                            Result.success(cachedRoutes)
                        } else {
                            Result.failure(Exception("Error del servidor: $responseCode"))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    val cachedRoutes = routeDao.getAllRoutes().map { it.toRoute() }
                    if (cachedRoutes.isNotEmpty()) {
                        Result.success(cachedRoutes)
                    } else {
                        Result.failure(e)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    private fun parseRoutesJson(jsonString: String): List<Route> {
        val routes = mutableListOf<Route>()

        try {
            val jsonObject = JSONObject(jsonString)
            val dataObject = jsonObject.getJSONObject("data")
            val featuresArray = dataObject.getJSONArray("features")

            for (i in 0 until featuresArray.length()) {
                val feature = featuresArray.getJSONObject(i)
                val properties = feature.getJSONObject("properties")
                val geometry = feature.getJSONObject("geometry")
                val coordinatesArray = geometry.getJSONArray("coordinates")

                val firstCoord = coordinatesArray.getJSONArray(0)

                val coordinates = mutableListOf<Pair<Double, Double>>()
                for (j in 0 until coordinatesArray.length()) {
                    val coord = coordinatesArray.getJSONArray(j)
                    coordinates.add(Pair(coord.getDouble(0), coord.getDouble(1)))
                }

                val route = Route(
                    id = properties.getString("id"),
                    name = properties.getString("nombre"),
                    distance = parseDistance(properties.getString("distancia")),
                    latitude = firstCoord.getDouble(1),
                    longitude = firstCoord.getDouble(0),
                    description = properties.optString("duracion", null),
                    coordinates = coordinates
                )

                routes.add(route)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return routes
    }

    private fun parseDistance(distanceStr: String): Double {
        return try {
            distanceStr.replace(" km", "").trim().toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun getCachedRoutesCount(): Int {
        return routeDao.getRoutesCount()
    }

    suspend fun getLastUpdateTime(): Long? {
        return routeDao.getLastUpdateTime()
    }
}