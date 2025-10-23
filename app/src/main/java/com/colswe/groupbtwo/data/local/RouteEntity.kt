package com.colswe.groupbtwo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.colswe.groupbtwo.data.model.Route
import org.json.JSONArray

@Entity(tableName = "routes")
@TypeConverters(CoordinatesConverter::class)
data class RouteEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val distance: Double,
    val latitude: Double,
    val longitude: Double,
    val description: String?,
    val coordinates: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

class CoordinatesConverter {
    @TypeConverter
    fun fromCoordinatesList(coordinates: List<Pair<Double, Double>>): String {
        val jsonArray = JSONArray()
        coordinates.forEach { (lng, lat) ->
            val coordArray = JSONArray()
            coordArray.put(lng)
            coordArray.put(lat)
            jsonArray.put(coordArray)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toCoordinatesList(coordinatesString: String): List<Pair<Double, Double>> {
        val coordinates = mutableListOf<Pair<Double, Double>>()
        try {
            val jsonArray = JSONArray(coordinatesString)
            for (i in 0 until jsonArray.length()) {
                val coord = jsonArray.getJSONArray(i)
                coordinates.add(Pair(coord.getDouble(0), coord.getDouble(1)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return coordinates
    }
}

fun RouteEntity.toRoute(): Route {
    val converter = CoordinatesConverter()
    return Route(
        id = id,
        name = name,
        distance = distance,
        latitude = latitude,
        longitude = longitude,
        description = description,
        coordinates = converter.toCoordinatesList(coordinates)
    )
}

fun Route.toEntity(): RouteEntity {
    val converter = CoordinatesConverter()
    return RouteEntity(
        id = id,
        name = name,
        distance = distance,
        latitude = latitude,
        longitude = longitude,
        description = description,
        coordinates = converter.fromCoordinatesList(coordinates)
    )
}