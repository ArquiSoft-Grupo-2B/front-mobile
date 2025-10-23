package com.colswe.groupbtwo.data.model

data class Route(
    val id: String,
    val name: String,
    val distance: Double,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val coordinates: List<Pair<Double, Double>> = emptyList()
)