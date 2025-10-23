package com.colswe.groupbtwo.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colswe.groupbtwo.data.model.Route
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val primaryColor = Color(0xFF003F55)
private val textColor = Color(0xFF1A1A1A)
private val routeColors = listOf(
    "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A", "#98D8C8"
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel(),
    onLogout: () -> Unit = {},
    onProfile: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val nearbyRoutes by viewModel.nearbyRoutes.collectAsState()
    val filteredRoutes by viewModel.filteredRoutes.collectAsState()
    val isLoadingRoutes by viewModel.isLoadingRoutes.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val navigationRoute by viewModel.navigationRoute.collectAsState()
    val startZoneCircle by viewModel.startZoneCircle.collectAsState()
    val maxDistance by viewModel.maxDistance.collectAsState()
    val validationMessage by viewModel.validationMessage.collectAsState()

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var shouldCenterOnLocation by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            while (true) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        viewModel.updateLocation(it)
                        if (nearbyRoutes.isEmpty()) {
                            viewModel.loadNearbyRoutes(it.latitude, it.longitude)
                            shouldCenterOnLocation = true
                        }
                    }
                }
                delay(5000)
            }
        }
    }

    fun centerOnLocation() {
        currentLocation?.let { location ->
            mapView?.getMapboxMap()?.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(location.longitude, location.latitude))
                    .zoom(15.0)
                    .build()
            )
        }
    }

    LaunchedEffect(currentLocation, filteredRoutes, navigationRoute, startZoneCircle) {
        currentLocation?.let { location ->
            mapView?.let { map ->
                if (shouldCenterOnLocation) {
                    centerOnLocation()
                    shouldCenterOnLocation = false
                }

                map.annotations.cleanup()
                val annotationApi = map.annotations
                val pointAnnotationManager = annotationApi.createPointAnnotationManager()
                val polylineAnnotationManager = annotationApi.createPolylineAnnotationManager()
                val circleAnnotationManager = annotationApi.createCircleAnnotationManager()

                val userLocationBitmap = createUserLocationBitmap()
                map.getMapboxMap().getStyle { style ->
                    if (!style.styleSourceExists("user-location-icon")) {
                        style.addImage("user-location-icon", userLocationBitmap)
                    }

                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(location.longitude, location.latitude))
                        .withIconImage("user-location-icon")
                        .withIconSize(1.5)

                    pointAnnotationManager.create(pointAnnotationOptions)
                }

                navigationRoute?.let { navRoute ->
                    if (navRoute.size >= 2) {
                        val points = navRoute.map { (lng, lat) ->
                            Point.fromLngLat(lng, lat)
                        }

                        val navPolylineOptions = PolylineAnnotationOptions()
                            .withPoints(points)
                            .withLineColor("#0000FF")
                            .withLineWidth(7.0)

                        polylineAnnotationManager.create(navPolylineOptions)
                    }
                }

                startZoneCircle?.let { (lng, lat) ->
                    val circleOptions = CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(lng, lat))
                        .withCircleRadius(calculateRadiusInPixels(200.0, lat, 15.0))
                        .withCircleColor("#4CAF50")
                        .withCircleOpacity(0.2)
                        .withCircleStrokeColor("#2E7D32")
                        .withCircleStrokeWidth(2.0)
                        .withCircleStrokeOpacity(0.6)

                    circleAnnotationManager.create(circleOptions)
                }

                filteredRoutes.forEachIndexed { index, route ->
                    if (route.coordinates.isNotEmpty()) {
                        val points = route.coordinates.map { (lng, lat) ->
                            Point.fromLngLat(lng, lat)
                        }

                        val isSelected = selectedRoute?.id == route.id
                        val polylineOptions = PolylineAnnotationOptions()
                            .withPoints(points)
                            .withLineColor(routeColors[index % routeColors.size])
                            .withLineWidth(if (isSelected) 8.0 else 5.0)

                        polylineAnnotationManager.create(polylineOptions)

                        val startPoint = route.coordinates.first()
                        val routeMarkerBitmap = createRouteMarkerBitmap(index)

                        map.getMapboxMap().getStyle { style ->
                            val markerKey = "route-marker-$index"
                            if (!style.styleSourceExists(markerKey)) {
                                style.addImage(markerKey, routeMarkerBitmap)
                            }

                            val routeMarkerOptions = PointAnnotationOptions()
                                .withPoint(Point.fromLngLat(startPoint.first, startPoint.second))
                                .withIconImage(markerKey)
                                .withIconSize(if (isSelected) 1.2 else 1.0)

                            pointAnnotationManager.create(routeMarkerOptions)
                        }
                    }
                }

                map.getMapboxMap().addOnMapClickListener { point ->
                    val clickedRoute = filteredRoutes.find { route ->
                        route.coordinates.any { (lng, lat) ->
                            val distance = calculateDistance(
                                point.latitude(), point.longitude(),
                                lat, lng
                            )
                            distance < 0.05
                        }
                    }

                    if (clickedRoute != null) {
                        viewModel.selectRoute(clickedRoute)
                    }
                    true
                }
            }
        }
    }

    LaunchedEffect(validationMessage) {
        validationMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            viewModel.clearValidationMessage()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = error,
                    actionLabel = "Reintentar",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.refreshRoutes()
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            DrawerContent(
                userEmail = viewModel.getCurrentUserEmail(),
                onLogout = {
                    viewModel.logout()
                    onLogout()
                },
                onCloseDrawer = { scope.launch { drawerState.close() } },
                onProfile = onProfile
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Mapa de Rutas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = primaryColor,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        var showFilterDialog by remember { mutableStateOf(false) }

                        IconButton(
                            onClick = { viewModel.refreshRoutes() },
                            enabled = !isLoadingRoutes
                        ) {
                            if (isLoadingRoutes) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Actualizar rutas",
                                    tint = Color.White
                                )
                            }
                        }

                        IconButton(onClick = { showFilterDialog = true }) {
                            Badge(
                                containerColor = if (maxDistance != null) Color.Red else Color.Transparent
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Filtrar rutas",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = { centerOnLocation() }) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Mi ubicación",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menú",
                                tint = Color.White
                            )
                        }

                        if (showFilterDialog) {
                            FilterDialog(
                                currentMaxDistance = maxDistance,
                                routes = nearbyRoutes,
                                onDismiss = { showFilterDialog = false },
                                onSelectRoute = { route ->
                                    viewModel.setMaxDistanceFilter(route.distance)
                                    viewModel.selectRoute(route)
                                    showFilterDialog = false
                                    if (route.coordinates.isNotEmpty()) {
                                        val startPoint = route.coordinates.first()
                                        mapView?.getMapboxMap()?.setCamera(
                                            CameraOptions.Builder()
                                                .center(Point.fromLngLat(startPoint.first, startPoint.second))
                                                .zoom(15.0)
                                                .build()
                                        )
                                    }
                                },
                                onClearFilters = {
                                    viewModel.clearFilters()
                                    showFilterDialog = false
                                }
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            mapView = this
                            getMapboxMap().loadStyleUri(Style.OUTDOORS)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )

                if (isLoadingRoutes) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }

                if (selectedRoute != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        RouteInfoCard(
                            route = selectedRoute!!,
                            onClose = { viewModel.clearSelectedRoute() },
                            onStartNavigation = { viewModel.startNavigation(selectedRoute!!) }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = paddingValues.calculateTopPadding() + 16.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    if (currentLocation != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Ubicación Actual",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${String.format("%.4f", currentLocation!!.latitude)}, ${
                                            String.format(
                                                "%.4f",
                                                currentLocation!!.longitude
                                            )
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (filteredRoutes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Rutas: ${filteredRoutes.size}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = textColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Toca una ruta en el mapa",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterDialog(
    currentMaxDistance: Double?,
    routes: List<Route>,
    onDismiss: () -> Unit,
    onSelectRoute: (Route) -> Unit,
    onClearFilters: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Selecciona una ruta",
                    style = MaterialTheme.typography.titleLarge,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
                if (currentMaxDistance != null) {
                    IconButton(onClick = onClearFilters) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Limpiar filtro",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    "${routes.size} rutas disponibles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    routes.sortedBy { it.distance }.forEach { route ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectRoute(route) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentMaxDistance == route.distance)
                                    primaryColor.copy(alpha = 0.1f)
                                else
                                    Color.White
                            ),
                            border = BorderStroke(
                                width = if (currentMaxDistance == route.distance) 2.dp else 1.dp,
                                color = if (currentMaxDistance == route.distance)
                                    primaryColor
                                else
                                    primaryColor.copy(alpha = 0.2f)
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (currentMaxDistance == route.distance) 4.dp else 2.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            route.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = textColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        route.description?.let { desc ->
                                            Text(
                                                desc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = textColor.copy(alpha = 0.6f),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${route.distance} km",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = primaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = "Ver ruta",
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Toca una ruta para verla en el mapa",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cerrar",
                    color = primaryColor,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            if (currentMaxDistance != null) {
                TextButton(onClick = onClearFilters) {
                    Text(
                        "Ver todas",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun RouteInfoCard(
    route: Route,
    onClose: () -> Unit,
    onStartNavigation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    route.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Cerrar", tint = textColor.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Distancia",
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${route.distance} km",
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                route.description?.let { desc ->
                    Column {
                        Text(
                            "Duración",
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStartNavigation,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Iniciar Ruta", fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

fun calculateRadiusInPixels(radiusInMeters: Double, latitude: Double, zoomLevel: Double): Double {
    val metersPerPixel = 156543.03392 * Math.cos(Math.toRadians(latitude)) / Math.pow(2.0, zoomLevel)
    return radiusInMeters / metersPerPixel
}

private fun createUserLocationBitmap(): Bitmap {
    val size = 120
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val outerPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5, outerPaint)

    val innerPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#2196F3")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 15, innerPaint)

    val centerPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, 10f, centerPaint)

    return bitmap
}

private fun createRouteMarkerBitmap(index: Int): Bitmap {
    val size = 100
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint().apply {
        color = android.graphics.Color.parseColor(routeColors[index % routeColors.size])
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 10, paint)

    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }

    canvas.drawText("${index + 1}", size / 2f, size / 2f + 15, textPaint)

    return bitmap
}

@Composable
fun DrawerContent(
    userEmail: String?,
    onLogout: () -> Unit,
    onCloseDrawer: () -> Unit,
    onProfile: () -> Unit = {}
) {
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerContainerColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(primaryColor),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Usuario",
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        userEmail ?: "Usuario",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.LocationOn, null, tint = primaryColor) },
                label = { Text("Mapa", fontWeight = FontWeight.SemiBold, color = textColor) },
                selected = true,
                onClick = { onCloseDrawer() },
                modifier = Modifier.padding(horizontal = 12.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = primaryColor.copy(alpha = 0.15f),
                    selectedIconColor = primaryColor,
                    selectedTextColor = textColor
                )
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Person, null, tint = textColor.copy(alpha = 0.7f)) },
                label = { Text("Perfil", fontWeight = FontWeight.Medium, color = textColor.copy(alpha = 0.9f)) },
                selected = false,
                onClick = {
                    onProfile()
                    onCloseDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = primaryColor.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.ExitToApp, null, tint = Color(0xFFD32F2F)) },
                label = { Text("Cerrar Sesión", color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold) },
                selected = false,
                onClick = onLogout,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "Versión 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Medium
            )
        }
    }
}