package app.overworld.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.overworld.BuildConfig
import app.overworld.Geo
import app.overworld.GeoapifyProvider
import app.overworld.Place
import app.overworld.Route
import app.overworld.Skin
import app.overworld.Skins
import app.overworld.StyleBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource

private const val EMPTY_FC = """{"type":"FeatureCollection","features":[]}"""

@Composable
fun MapScreen(skinId: String, onSkin: (String) -> Unit, showTip: Boolean, onTipSeen: () -> Unit) {
    val ctx = LocalContext.current
    val skin: Skin = Skins.byId(skinId)
    val cardBg = if (skin.dark) Color(0xF0101626) else Color(0xF5FFFFFF)
    val ink = inkFor(skin)
    val muted = ink.copy(alpha = 0.6f)
    val accent = hex(skin.accent)
    val scope = rememberCoroutineScope()
    val provider = remember { GeoapifyProvider(BuildConfig.GEOAPIFY_KEY) }

    // ---- map ----
    val mapView = remember {
        MapLibre.getInstance(ctx)
        MapView(ctx)
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(Unit) {
        mapView.getMapAsync { m ->
            map = m
            // Neutral starting view until we have a location fix.
            m.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(LatLng(18.5204, 73.8567)).zoom(11.0).build()
                )
            )
        }
    }
    LaunchedEffect(map, skin.id) {
        val m = map ?: return@LaunchedEffect
        style = null
        m.setStyle(Style.Builder().fromJson(StyleBuilder.build(skin))) { st -> style = st }
    }

    // ---- app state ----
    var mode by remember { mutableStateOf("home") } // home | plan | drive | arrived
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var dest by remember { mutableStateOf<Place?>(null) }
    var route by remember { mutableStateOf<Route?>(null) }
    var planning by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showSkins by remember { mutableStateOf(false) }
    var askLocation by remember { mutableStateOf(false) }
    var demo by remember { mutableStateOf(false) }
    var realMe by remember { mutableStateOf<DoubleArray?>(null) }
    var demoMe by remember { mutableStateOf<DoubleArray?>(null) }
    var firstFix by remember { mutableStateOf(true) }
    val me: DoubleArray? = if (demo) demoMe else realMe

    // ---- location (just-in-time permission) ----
    val lm = remember { ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var hasPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val listener = remember {
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                realMe = doubleArrayOf(location.longitude, location.latitude)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        hasPerm = r[Manifest.permission.ACCESS_FINE_LOCATION] == true || r[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!hasPerm) message = "No problem - you can still search and preview routes."
    }

    @SuppressLint("MissingPermission")
    fun startUpdates() {
        try {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { p ->
                if (lm.isProviderEnabled(p)) {
                    lm.requestLocationUpdates(p, 1000L, 0f, listener)
                    lm.getLastKnownLocation(p)?.let { if (realMe == null) realMe = doubleArrayOf(it.longitude, it.latitude) }
                }
            }
        } catch (_: SecurityException) {
        }
    }
    LaunchedEffect(hasPerm) { if (hasPerm) startUpdates() }
    DisposableEffect(Unit) { onDispose { lm.removeUpdates(listener) } }

    fun recenter() {
        val m = map ?: return
        val p = me
        if (p == null) {
            message = "Looking for your location..."
            return
        }
        m.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(LatLng(p[1], p[0])).zoom(15.5).tilt(45.0).bearing(0.0).build()
            ),
            800,
        )
    }

    // ---- route progress ----
    val line = route?.line
    val cum = remember(route) { line?.let { Geo.cumulative(it) } }
    val idx = if (mode == "drive" && me != null && line != null) Geo.nearestIndex(line, me) else 0
    val remaining = if (mode == "drive" && me != null && line != null && cum != null) {
        cum.last() - cum[idx] + Geo.dist(me, line[idx])
    } else {
        route?.meters ?: 0.0
    }
    val totalM = route?.meters?.takeIf { it > 0 } ?: cum?.last() ?: 1.0
    val remainingSec = (route?.seconds ?: 0.0) * (remaining / totalM).coerceIn(0.0, 1.0)
    val nextStep = route?.steps?.firstOrNull { it.fromIndex > idx }
    val nextStepDist =
        if (nextStep != null && me != null && line != null && cum != null && nextStep.fromIndex < line.size) {
            cum[nextStep.fromIndex] - cum[idx] + Geo.dist(me, line[idx])
        } else {
            remaining
        }

    // ---- push data into the map ----
    LaunchedEffect(style, route, me, dest) {
        val st = style ?: return@LaunchedEffect
        st.getSourceAs<GeoJsonSource>("route")?.setGeoJson(line?.let { Geo.lineJson(it) } ?: EMPTY_FC)
        st.getSourceAs<GeoJsonSource>("me")?.setGeoJson(me?.let { Geo.pointJson(it[0], it[1]) } ?: EMPTY_FC)
        st.getSourceAs<GeoJsonSource>("dest")?.setGeoJson(dest?.let { Geo.pointJson(it.lon, it.lat) } ?: EMPTY_FC)
    }

    // ---- camera follow ----
    LaunchedEffect(me, mode, map) {
        val m = map ?: return@LaunchedEffect
        val p = me ?: return@LaunchedEffect
        if (mode == "drive") {
            val l = route?.line
            val bearing = if (l != null && l.size > 1) {
                val i = Geo.nearestIndex(l, p)
                Geo.bearing(l[i], l[minOf(i + 3, l.size - 1)])
            } else {
                0.0
            }
            m.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(LatLng(p[1], p[0])).zoom(17.0).tilt(58.0).bearing(bearing).build()
                ),
                500,
            )
        } else if (firstFix) {
            firstFix = false
            m.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(LatLng(p[1], p[0])).zoom(15.0).tilt(45.0).build()
                ),
                1200,
            )
        }
    }

    // ---- demo drive (test without driving) ----
    LaunchedEffect(mode, demo) {
        val r = route
        if (mode != "drive" || !demo || r == null) return@LaunchedEffect
        val c = Geo.cumulative(r.line)
        var s = 0.0
        while (s < c.last()) {
            demoMe = Geo.pointAt(r.line, c, s)
            s += 12.5 // 25 m/s
            delay(500)
        }
        demoMe = r.line.last()
    }
    LaunchedEffect(remaining, mode) {
        if (mode == "drive" && remaining < 30.0 && route != null) mode = "arrived"
    }

    // ---- search ----
    LaunchedEffect(query) {
        if (query.length < 3 || query == picked) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(350)
        if (BuildConfig.GEOAPIFY_KEY.isEmpty()) {
            message = "Search needs a Geoapify key - see the README."
            return@LaunchedEffect
        }
        results = try {
            provider.search(query, me?.get(1), me?.get(0))
        } catch (e: Exception) {
            message = "Search failed. Check your connection."
            emptyList()
        }
    }
    LaunchedEffect(message) {
        if (message != null) {
            delay(3500)
            message = null
        }
    }

    fun pick(p: Place) {
        picked = p.name
        query = p.name
        results = emptyList()
        dest = p
        route = null
        mode = "plan"
        planning = true
        showSkins = false
        scope.launch {
            val from: DoubleArray? = me ?: map?.cameraPosition?.target?.let { doubleArrayOf(it.longitude, it.latitude) }
            if (from == null) {
                planning = false
                return@launch
            }
            if (me == null) message = "No location yet - routing from the map centre."
            try {
                val r = provider.route(from[1], from[0], p.lat, p.lon)
                route = r
                if (r == null) message = "No route found."
                val m = map
                if (r != null && m != null) {
                    val pts = r.line.map { LatLng(it[1], it[0]) }
                    val b = LatLngBounds.Builder().includes(pts).build()
                    m.easeCamera(CameraUpdateFactory.newLatLngBounds(b, 90, 260, 90, 520), 900)
                }
            } catch (e: Exception) {
                message = "Couldn't plan the route."
            }
            planning = false
        }
    }

    fun clearTrip() {
        mode = "home"
        route = null
        dest = null
        demo = false
        demoMe = null
        query = ""
        picked = null
        recenter()
    }

    // ================= UI =================
    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // Top: search (home/plan) or next instruction (drive)
        Column(Modifier.fillMaxWidth().systemBarsPadding().padding(12.dp)) {
            if (mode == "home" || mode == "plan") {
                Box(
                    Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp))
                        .background(cardBg).padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = ink, fontSize = 17.sp),
                        cursorBrush = SolidColor(accent),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) Text("Where to?", color = muted, fontSize = 17.sp)
                                inner()
                            }
                        },
                    )
                }
                if (results.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                            .background(cardBg)
                    ) {
                        results.forEach { p ->
                            Column(Modifier.fillMaxWidth().clickable { pick(p) }.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                Text(p.name, color = ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                if (p.sub.isNotEmpty()) Text(p.sub, color = muted, fontSize = 13.sp, maxLines = 1)
                            }
                        }
                    }
                }
            } else if (mode == "drive") {
                Column(
                    Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                        .background(cardBg).padding(20.dp)
                ) {
                    Text(Geo.fmtDist(nextStepDist), color = accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(
                        nextStep?.text?.takeIf { it.isNotEmpty() } ?: "Follow the route",
                        color = ink, fontSize = 18.sp, maxLines = 2,
                    )
                }
            }
        }

        // Round buttons (home/plan)
        if (mode == "home" || mode == "plan") {
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RoundButton("◎", cardBg, ink) {
                    if (hasPerm) recenter() else askLocation = true
                }
                RoundButton("◐", cardBg, ink) { showSkins = !showSkins }
            }
        }

        // Bottom area
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().systemBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            message?.let {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(18.dp))
                        .background(Color(0xE6000000)).padding(horizontal = 16.dp, vertical = 10.dp)
                ) { Text(it, color = Color.White, fontSize = 14.sp) }
            }

            if (showSkins && (mode == "home" || mode == "plan")) {
                Row(
                    Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                        .background(cardBg).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Skins.all.forEach { s ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                .background(if (s.id == skin.id) hex(s.accent).copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { onSkin(s.id) }.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            SkinPreview(s, Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)))
                            Text(s.name, color = ink, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }

            when {
                askLocation && mode == "home" -> Card(cardBg) {
                    Text("Place your car on the map", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Your location is used to show where you are and plan routes.",
                        color = muted, fontSize = 14.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton("Allow", accent) {
                            askLocation = false
                            permLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                        TextAction("Not now", muted) { askLocation = false }
                    }
                }

                showTip && mode == "home" && !askLocation -> Card(cardBg) {
                    Text("Where are you headed?", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Search above to plan a drive, or tap ◎ to find yourself. Tap ◐ to change your world.",
                        color = muted, fontSize = 14.sp,
                    )
                    PrimaryButton("Got it", accent) { onTipSeen() }
                }

                mode == "plan" -> Card(cardBg) {
                    Text(dest?.name ?: "", color = ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        when {
                            planning -> "Finding the best route..."
                            route != null -> "${Geo.fmtTime(route!!.seconds)} · ${Geo.fmtDist(route!!.meters)}"
                            else -> "No route"
                        },
                        color = muted, fontSize = 15.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        PrimaryButton("Start", accent, enabled = route != null) {
                            demo = false
                            mode = "drive"
                        }
                        TextAction("Demo drive", muted) {
                            if (route != null) {
                                demo = true
                                demoMe = route!!.line.first()
                                mode = "drive"
                            }
                        }
                        TextAction("Cancel", muted) { clearTrip() }
                    }
                }

                mode == "drive" -> Card(cardBg) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(Geo.fmtTime(remainingSec), color = ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text(Geo.fmtDist(remaining) + if (demo) " · demo" else "", color = muted, fontSize = 14.sp)
                        }
                        PrimaryButton("End", accent) { clearTrip() }
                    }
                }

                mode == "arrived" -> Card(cardBg) {
                    Text("You've arrived", color = ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(dest?.name ?: "", color = muted, fontSize = 15.sp, maxLines = 1)
                    PrimaryButton("Done", accent) { clearTrip() }
                }
            }
        }
    }
}

@Composable
private fun Card(bg: Color, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp))
            .background(bg).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun RoundButton(glyph: String, bg: Color, ink: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(glyph, color = ink, fontSize = 22.sp) }
}

@Composable
private fun PrimaryButton(label: String, accent: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent(accent)),
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun TextAction(label: String, color: Color, onClick: () -> Unit) {
    Text(
        label, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp),
    )
}
