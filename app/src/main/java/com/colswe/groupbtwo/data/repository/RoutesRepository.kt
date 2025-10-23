package com.colswe.groupbtwo.data.repository

import com.colswe.groupbtwo.data.model.Route
import com.colswe.groupbtwo.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RoutesRepository {
    private val baseUrl = Constants.BASE_URL_ROUTES

    suspend fun getNearbyRoutes(lat: Double, lng: Double): Result<List<Route>> {
        return withContext(Dispatchers.IO) {
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
                    Result.success(routes)
                } else {
                    Result.failure(Exception("Error del servidor: $responseCode"))
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
                val lastCoord = coordinatesArray.getJSONArray(coordinatesArray.length() - 1)

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
}