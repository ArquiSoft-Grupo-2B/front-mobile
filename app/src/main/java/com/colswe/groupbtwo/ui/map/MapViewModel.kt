package com.colswe.groupbtwo.ui.map

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colswe.groupbtwo.data.repository.AuthRepository
import com.colswe.groupbtwo.data.repository.RoutesRepository
import com.colswe.groupbtwo.data.model.Route
import com.colswe.groupbtwo.util.NetworkUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MapViewModel(
    private val context: Context,
    private val authRepository: AuthRepository = AuthRepository(),
    private val routesRepository: RoutesRepository = RoutesRepository(context)
) : ViewModel() {

    private val _nearbyRoutes = MutableStateFlow<List<Route>>(emptyList())
    val nearbyRoutes: StateFlow<List<Route>> = _nearbyRoutes.asStateFlow()

    private val _filteredRoutes = MutableStateFlow<List<Route>>(emptyList())
    val filteredRoutes: StateFlow<List<Route>> = _filteredRoutes.asStateFlow()

    private val _isLoadingRoutes = MutableStateFlow(false)
    val isLoadingRoutes: StateFlow<Boolean> = _isLoadingRoutes.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _selectedRoute = MutableStateFlow<Route?>(null)
    val selectedRoute: StateFlow<Route?> = _selectedRoute.asStateFlow()

    private val _navigationRoute = MutableStateFlow<List<Pair<Double, Double>>?>(null)
    val navigationRoute: StateFlow<List<Pair<Double, Double>>?> = _navigationRoute.asStateFlow()

    private val _startZoneCircle = MutableStateFlow<Pair<Double, Double>?>(null)
    val startZoneCircle: StateFlow<Pair<Double, Double>?> = _startZoneCircle.asStateFlow()

    private val _maxDistance = MutableStateFlow<Double?>(null)
    val maxDistance: StateFlow<Double?> = _maxDistance.asStateFlow()

    private val _validationMessage = MutableStateFlow<String?>(null)
    val validationMessage: StateFlow<String?> = _validationMessage.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    fun logout() {
        authRepository.logout()
    }

    fun getCurrentUserEmail(): String? {
        return authRepository.auth.currentUser?.email
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = location
    }

    fun refreshRoutes() {
        val lat = _currentLocation.value?.latitude
        val lng = _currentLocation.value?.longitude
        loadNearbyRoutes(lat, lng, forceRefresh = true)
    }

    fun loadNearbyRoutes(lat: Double? = null, lng: Double? = null, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoadingRoutes.value = true
            _errorMessage.value = null

            val hasInternet = NetworkUtil.isNetworkAvailable(context)
            _isOfflineMode.value = !hasInternet

            val latitude = lat ?: _currentLocation.value?.latitude ?: 4.63
            val longitude = lng ?: _currentLocation.value?.longitude ?: -74.08034

            routesRepository.getNearbyRoutes(latitude, longitude, forceRefresh).fold(
                onSuccess = { routes ->
                    _nearbyRoutes.value = routes
                    applyFilters()
                    _isLoadingRoutes.value = false

                    if (!hasInternet && routes.isNotEmpty()) {
                        _errorMessage.value = "Modo offline: Mostrando rutas guardadas"
                    }
                },
                onFailure = { exception ->
                    val errorMsg = if (!hasInternet) {
                        "Sin conexión y sin datos guardados"
                    } else {
                        "Error al cargar rutas: ${exception.message}"
                    }
                    _errorMessage.value = errorMsg
                    _isLoadingRoutes.value = false
                }
            )
        }
    }

    fun applyFilters() {
        var routes = _nearbyRoutes.value

        _maxDistance.value?.let { maxDist ->
            routes = routes.filter { it.distance <= maxDist }
        }

        _filteredRoutes.value = routes
    }

    fun setMaxDistanceFilter(distance: Double?) {
        _maxDistance.value = distance
        applyFilters()
    }

    fun clearFilters() {
        _maxDistance.value = null
        applyFilters()
    }

    fun selectRoute(route: Route) {
        _selectedRoute.value = route
        if (route.coordinates.isNotEmpty()) {
            val startPoint = route.coordinates.first()
            _startZoneCircle.value = startPoint
        }
    }

    fun clearSelectedRoute() {
        _selectedRoute.value = null
        _navigationRoute.value = null
        _startZoneCircle.value = null
    }

    fun clearValidationMessage() {
        _validationMessage.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun startNavigation(route: Route): Boolean {
        val location = _currentLocation.value

        if (location == null) {
            _validationMessage.value = "No se pudo obtener tu ubicación actual"
            return false
        }

        if (route.coordinates.isEmpty()) {
            _validationMessage.value = "Esta ruta no tiene coordenadas válidas"
            return false
        }

        val startPoint = route.coordinates.first()
        val distanceToStart = calculateDistanceInMeters(
            location.latitude,
            location.longitude,
            startPoint.second,
            startPoint.first
        )

        if (distanceToStart > 200) {
            val distanceKm = String.format("%.2f", distanceToStart / 1000)
            _validationMessage.value = "Debes estar cerca del inicio de la ruta para comenzar (máximo 200m). Te encuentras a $distanceKm km del punto de inicio."
            return false
        }

        val navigationPoints = mutableListOf<Pair<Double, Double>>()
        navigationPoints.add(Pair(location.longitude, location.latitude))
        navigationPoints.add(route.coordinates.first())

        _navigationRoute.value = navigationPoints
        _validationMessage.value = null
        return true
    }

    fun clearNavigation() {
        _navigationRoute.value = null
    }

    private fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}