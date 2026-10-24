package ru.routealert.app.ui.components

import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import android.graphics.PointF
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapLoadedListener
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.MapType
import com.yandex.mapkit.map.RotationType
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.delay
import ru.routealert.app.R
import ru.routealert.app.data.RoutePoint
import ru.routealert.app.ui.theme.Glass
import ru.routealert.app.ui.theme.liquidGlass

@Composable
fun YandexMapView(
    modifier: Modifier = Modifier,
    points: List<RoutePoint>,
    userLat: Double?,
    userLon: Double?,
    mapType: MapType = MapType.MAP,
    nightMode: Boolean = false,
    recenterTick: Int = 0,
    focusTick: Int = 0,
    focusLat: Double? = null,
    focusLon: Double? = null,
    onMapLongTap: (lat: Double, lon: Double) -> Unit,
    onMarkerTap: (RoutePoint) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapError by remember { mutableStateOf<String?>(null) }
    var mapLoading by remember { mutableStateOf(true) }

    val mapView = remember {
        runCatching {
            MapView(context).also {
                it.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        }.onFailure {
            Log.e("OnTheGo", "MapView create failed", it)
            mapError = it.message ?: "Ошибка карты Яндекс"
        }.getOrNull()
    }

    if (mapView == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Карта недоступна.\nПроверьте ключ Yandex MapKit.\n\n${mapError.orEmpty()}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    val pointByObject = remember { mutableMapOf<Any, RoutePoint>() }
    var didInitialCamera by remember { mutableStateOf(false) }
    var pointsLayer by remember { mutableStateOf<MapObjectCollection?>(null) }

    DisposableEffect(lifecycleOwner, mapView) {
        runCatching {
            MapKitFactory.getInstance().onStart()
            mapView.onStart()
        }.onFailure {
            Log.e("OnTheGo", "MapKit onStart failed", it)
            mapError = it.message
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> runCatching {
                    MapKitFactory.getInstance().onStart()
                    mapView.onStart()
                }
                Lifecycle.Event.ON_STOP -> runCatching {
                    mapView.onStop()
                    MapKitFactory.getInstance().onStop()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching {
                mapView.onStop()
                MapKitFactory.getInstance().onStop()
            }
        }
    }

    val inputListener = remember {
        object : InputListener {
            override fun onMapTap(map: Map, point: Point) = Unit

            override fun onMapLongTap(map: Map, point: Point) {
                onMapLongTap(point.latitude, point.longitude)
            }
        }
    }

    val tapListener = remember {
        MapObjectTapListener { mapObject, _ ->
            val point = pointByObject[mapObject]
            if (point != null) {
                onMarkerTap(point)
                true
            } else false
        }
    }

    LaunchedEffect(mapView) {
        runCatching {
            val map = mapView.mapWindow.map
            map.addInputListener(inputListener)
            map.setMapLoadedListener(
                MapLoadedListener {
                    mapLoading = false
                }
            )
            pointsLayer = map.mapObjects.addCollection()
            val userLocation = MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow)
            val pinIcon = ImageProvider.fromResource(context, R.drawable.ic_user_pin)
            val arrowIcon = ImageProvider.fromResource(context, R.drawable.ic_user_arrow)
            userLocation.setObjectListener(object : UserLocationObjectListener {
                override fun onObjectAdded(view: UserLocationView) = styleUserLocation(view, pinIcon, arrowIcon)
                override fun onObjectRemoved(view: UserLocationView) = Unit
                override fun onObjectUpdated(view: UserLocationView, event: com.yandex.mapkit.layers.ObjectEvent) {
                    if (event is com.yandex.mapkit.user_location.UserLocationIconChanged) {
                        styleUserLocation(view, pinIcon, arrowIcon)
                    }
                }
            })
            userLocation.isVisible = true
            userLocation.isHeadingEnabled = true
            userLocation.setDefaultSource()
            map.move(CameraPosition(Point(55.751244, 37.618423), 11f, 0f, 0f))
        }.onFailure {
            Log.e("OnTheGo", "Map init failed", it)
            mapError = it.message
            mapLoading = false
        }
    }

    LaunchedEffect(mapType, nightMode) {
        mapLoading = true
        runCatching {
            val map = mapView.mapWindow.map
            // Public MapKit keys only get road map tiles; SATELLITE/HYBRID stay blank.
            val safeType = when (mapType) {
                MapType.SATELLITE, MapType.HYBRID -> MapType.MAP
                else -> mapType
            }
            map.mapType = safeType
            map.isNightModeEnabled = nightMode && safeType == MapType.MAP
        }
        delay(1200)
        mapLoading = false
    }

    LaunchedEffect(points) {
        runCatching {
            val layer = pointsLayer ?: return@LaunchedEffect
            layer.clear()
            pointByObject.clear()
            val dot = ImageProvider.fromResource(context, R.drawable.ic_point_dot)
            val dotStyle = IconStyle()
                .setAnchor(PointF(0.5f, 0.5f))
                .setScale(0.9f)

            points.forEach { point ->
                layer.addCircle(
                    Circle(Point(point.latitude, point.longitude), point.radiusMeters)
                ).apply {
                    strokeColor = 0xFF2F6BFF.toInt()
                    strokeWidth = 2f
                    fillColor = 0x332F6BFF
                }

                val placemark = layer.addPlacemark(Point(point.latitude, point.longitude))
                placemark.setIcon(dot)
                placemark.setIconStyle(dotStyle)
                placemark.opacity = if (point.enabled) 1f else 0.45f
                placemark.addTapListener(tapListener)
                pointByObject[placemark] = point
            }

            if (!didInitialCamera && points.isNotEmpty()) {
                val focus = Point(points.first().latitude, points.first().longitude)
                mapView.mapWindow.map.move(
                    CameraPosition(focus, 13f, 0f, 0f),
                    Animation(Animation.Type.SMOOTH, 0.4f),
                    null
                )
                didInitialCamera = true
            }
        }.onFailure { Log.e("OnTheGo", "Update points failed", it) }
    }

    LaunchedEffect(recenterTick, userLat, userLon) {
        if (recenterTick == 0 || userLat == null || userLon == null) return@LaunchedEffect
        runCatching {
            mapView.mapWindow.map.move(
                CameraPosition(Point(userLat, userLon), 16f, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 0.35f),
                null
            )
        }
    }

    LaunchedEffect(focusTick, focusLat, focusLon) {
        if (focusTick == 0 || focusLat == null || focusLon == null) return@LaunchedEffect
        runCatching {
            mapView.mapWindow.map.move(
                CameraPosition(Point(focusLat, focusLon), 15f, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 0.45f),
                null
            )
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                (mapView.parent as? android.view.ViewGroup)?.removeView(mapView)
                mapView
            }
        )
        if (mapLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .liquidGlass(shape = CircleShape, colors = Glass.MapFillStrong),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
            }
        }
        mapError?.let { err ->
            Text(
                text = err,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun styleUserLocation(
    view: UserLocationView,
    pinIcon: ImageProvider,
    arrowIcon: ImageProvider
) {
    if (!view.isValid) return
    val heading = view.arrow.direction
    view.pin.setIcon(pinIcon)
    view.pin.setIconStyle(
        IconStyle()
            .setAnchor(PointF(0.5f, 1f))
            .setRotationType(RotationType.NO_ROTATION)
            .setScale(1.15f)
    )
    view.arrow.setIcon(arrowIcon)
    view.arrow.setIconStyle(
        IconStyle()
            .setAnchor(PointF(0.5f, 0.5f))
            .setRotationType(RotationType.ROTATE)
            .setScale(1.15f)
    )
    view.arrow.direction = heading
    view.accuracyCircle.fillColor = 0x332F6BFF
    view.accuracyCircle.strokeColor = 0xFF2F6BFF.toInt()
    view.accuracyCircle.strokeWidth = 1.5f
}
