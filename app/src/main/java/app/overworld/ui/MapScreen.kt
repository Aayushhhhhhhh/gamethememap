package app.overworld.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import app.overworld.Prefs
import app.overworld.Route
import app.overworld.Skin
import app.overworld.Skins
import app.overworld.StyleBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.async
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
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val EMPTY_FC = """{"type":"FeatureCollection","features":[]}"""

@Composable
fun MapScreen(skinId: String, onSkin: (String) -> Unit) {
    val ctx = LocalContext.current
    val skin: Skin = Skins.byId(skinId)
    val ui = remember(skin.id) {
        Ui(
            card = if (skin.dark) Color(0xF2101626) else Color(0xF7FFFFFF),
            ink = inkFor(skin),
            muted = inkFor(skin).copy(alpha = 0.6f),
            accent = hex(skin.accent),
            tile = if (skin.dark) Color(0xFF1B2335) else Color(0xFFECEAE4),
            onInk = if (skin.dark) Color.Black else Color.White,
        )
    }
    val scope = rememberCoroutineScope()
    val provider = remember { GeoapifyProvider(BuildConfig.GEOAPIFY_KEY) }
    val screenH = LocalConfiguration.current.screenHeightDp.dp

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
    var mode by remember { mutableStateOf("home") } // home | place | plan | drive | arrived
    var sheetFull by remember { mutableStateOf(false) }
    var showWorlds by remember { mutableStateOf(false) }
    var showAvoid by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showBuildings by remember { mutableStateOf(true) }
    var showLabels by remember { mutableStateOf(true) }
    var is3d by remember { mutableStateOf(true) }
    var saved by remember { mutableStateOf(Prefs.saved(ctx)) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var listTitle by remember { mutableStateOf<String?>(null) }
    var listItems by remember { mutableStateOf<List<Place>>(emptyList()) }
    var loadingList by remember { mutableStateOf(false) }
    var savingFor by remember { mutableStateOf<String?>(null) } // "home" | "work"
    var home by remember { mutableStateOf(Prefs.place(ctx, "home")) }
    var work by remember { mutableStateOf(Prefs.place(ctx, "work")) }
    var recents by remember { mutableStateOf(Prefs.recents(ctx)) }
    var dest by remember { mutableStateOf<Place?>(null) }
    var travel by remember { mutableStateOf("drive") }
    var routes by remember { mutableStateOf<Map<String, Route?>>(emptyMap()) }
    var route by remember { mutableStateOf<Route?>(null) }
    var planning by remember { mutableStateOf(false) }
    var avoidTolls by remember { mutableStateOf(false) }
    var avoidHighways by remember { mutableStateOf(false) }
    var avoidFerries by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
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

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {
                sheetFull = true
                query = it
            }
        }
    }
    fun startMic() {
        try {
            micLauncher.launch(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
            )
        } catch (_: Exception) {
            message = "Voice search isn't available on this device."
        }
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

    fun origin(): DoubleArray? =
        me ?: map?.cameraPosition?.target?.let { doubleArrayOf(it.longitude, it.latitude) }

    fun distText(p: Place): String {
        val o = origin() ?: return ""
        return Geo.fmtDist(Geo.dist(o, doubleArrayOf(p.lon, p.lat)))
    }

    fun recenter() {
        val m = map ?: return
        val p = me
        if (p == null) {
            if (hasPerm) message = "Looking for your location..." else askLocation = true
            return
        }
        m.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(LatLng(p[1], p[0])).zoom(15.5).tilt(45.0).bearing(0.0).build()
            ),
            800,
        )
    }

    fun toggle3d() {
        val m = map ?: return
        is3d = !is3d
        val cp = m.cameraPosition
        m.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(cp.target).zoom(cp.zoom).bearing(0.0).tilt(if (is3d) 50.0 else 0.0).build()
            ),
            600,
        )
    }

    fun fitRoute() {
        val r = route ?: return
        val m = map ?: return
        val h = ctx.resources.displayMetrics.heightPixels
        val b = LatLngBounds.Builder().includes(r.line.map { LatLng(it[1], it[0]) }).build()
        m.easeCamera(CameraUpdateFactory.newLatLngBounds(b, 90, (h * 0.12).toInt(), 90, (h * 0.5).toInt()), 900)
    }

    fun openPlace(p: Place) {
        val key = savingFor
        query = ""
        results = emptyList()
        sheetFull = false
        listTitle = null
        if (key != null) {
            Prefs.savePlace(ctx, key, p)
            if (key == "home") home = p else work = p
            savingFor = null
            message = "Saved as ${if (key == "home") "Home" else "Work"}"
            return
        }
        Prefs.addRecent(ctx, p)
        recents = Prefs.recents(ctx)
        dest = p
        route = null
        routes = emptyMap()
        mode = "place"
        map?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(LatLng(p.lat, p.lon)).zoom(16.0).tilt(50.0).build()
            ),
            900,
        )
    }

    fun fetchRoutes() {
        val p = dest ?: return
        val from = me?.let { it } ?: run {
            message = "Turn on location so I can route from where you are."
            return
        }
        val avoid = buildList {
            if (avoidTolls) add("tolls")
            if (avoidHighways) add("highways")
            if (avoidFerries) add("ferries")
        }
        planning = true
        scope.launch {
            var routeErr: String? = null
            val modes = listOf("drive", "bicycle", "walk")
            val res = modes.map { m ->
                async {
                    try {
                        provider.route(from[1], from[0], p.lat, p.lon, m, avoid)
                    } catch (e: Exception) {
                        if (routeErr == null) routeErr = e.message
                        null
                    }
                }
            }.map { it.await() }
            routes = modes.zip(res).toMap()
            route = routes[travel]
            planning = false
            if (route == null) message = routeErr?.let { "Route failed: $it" } ?: "No route found."
            fitRoute()
        }
    }

    fun searchCategory(title: String, cats: String) {
        if (BuildConfig.GEOAPIFY_KEY.isEmpty()) {
            message = "Add a Geoapify key to search nearby - see the README."
            return
        }
        val c = origin()
        if (c == null) {
            message = "Waiting for a location first."
            return
        }
        sheetFull = true
        listTitle = title
        listItems = emptyList()
        loadingList = true
        scope.launch {
            listItems = try {
                provider.nearby(cats, c[1], c[0], 5000)
            } catch (e: Exception) {
                message = "Places failed: ${e.message}"
                emptyList()
            }
            loadingList = false
        }
    }

    fun openSavedList() {
        sheetFull = true
        listTitle = "Saved"
        loadingList = false
        listItems = listOfNotNull(home, work) + saved.filter { s -> s.name != home?.name && s.name != work?.name }
    }

    fun clearTrip() {
        mode = "home"
        route = null
        routes = emptyMap()
        dest = null
        demo = false
        demoMe = null
        travel = "drive"
        query = ""
        showAvoid = false
        sheetFull = false
        listTitle = null
        if (me != null) recenter()
    }

    BackHandler(enabled = showWorlds || showAvoid || showDetails || showProfile || mode != "home" || sheetFull) {
        when {
            showWorlds -> showWorlds = false
            showDetails -> showDetails = false
            showProfile -> showProfile = false
            showAvoid -> showAvoid = false
            mode == "plan" -> {
                mode = "place"
                route = null
                routes = emptyMap()
            }
            mode == "place" || mode == "drive" || mode == "arrived" -> clearTrip()
            sheetFull -> {
                sheetFull = false
                query = ""
                listTitle = null
            }
        }
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
    fun eta(sec: Double): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(System.currentTimeMillis() + (sec * 1000).toLong()))

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

    // ---- map detail toggles ----
    LaunchedEffect(style, showBuildings, showLabels) {
        val st = style ?: return@LaunchedEffect
        fun vis(on: Boolean) = PropertyFactory.visibility(if (on) Property.VISIBLE else Property.NONE)
        st.getLayer("buildings")?.setProperties(vis(showBuildings))
        st.getLayer("place")?.setProperties(vis(showLabels))
        st.getLayer("road-name")?.setProperties(vis(showLabels))
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

    // ---- live search ----
    LaunchedEffect(query) {
        if (query.length < 3) {
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
            message = "Search failed: ${e.message}"
            emptyList()
        }
    }
    LaunchedEffect(message) {
        if (message != null) {
            delay(6000)
            message = null
        }
    }
    LaunchedEffect(avoidTolls, avoidHighways, avoidFerries) {
        if (mode == "plan") fetchRoutes()
    }

    // ================= UI =================
    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // Next instruction (drive)
        if (mode == "drive") {
            Column(
                Modifier.systemBarsPadding().padding(12.dp).fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)).background(ui.card).padding(20.dp)
            ) {
                Text(Geo.fmtDist(nextStepDist), color = ui.accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    nextStep?.text?.takeIf { it.isNotEmpty() } ?: "Follow the route",
                    color = ui.ink, fontSize = 18.sp, maxLines = 2,
                )
            }
        }

        // Bottom stack
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            if ((mode == "home" || mode == "place" || mode == "plan") && !sheetFull && !askLocation) {
                Column(
                    Modifier.align(Alignment.End).padding(end = 14.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(
                        Modifier.shadow(6.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)).background(ui.card)
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SkinPreview(
                            skin,
                            Modifier.size(46.dp).clip(CircleShape).clickable { showWorlds = true },
                        )
                        Box(
                            Modifier.size(46.dp).clip(CircleShape).clickable { showDetails = true },
                            contentAlignment = Alignment.Center,
                        ) { Text("◈", color = ui.ink, fontSize = 24.sp) }
                    }
                    RoundButton("➤", ui) { recenter() }
                }
            }
            message?.let {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp).clip(RoundedCornerShape(18.dp))
                        .background(Color(0xE6000000)).padding(horizontal = 16.dp, vertical = 10.dp)
                ) { Text(it, color = Color.White, fontSize = 14.sp) }
            }

            when {
                askLocation && mode == "home" -> SheetShell(ui) {
                    Text("Place your car on the map", color = ui.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Your location is used to show where you are and plan routes.",
                        color = ui.muted, fontSize = 14.sp,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryBtn("Allow", ui) {
                            askLocation = false
                            permLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                        TextBtn("Not now", ui.muted) { askLocation = false }
                    }
                }

                mode == "home" -> HomeSheet(
                    ui = ui,
                    full = sheetFull,
                    maxHeight = if (sheetFull) screenH * 0.9f else screenH * 0.56f,
                    hint = if (savingFor != null) "Search a place to save as ${if (savingFor == "home") "Home" else "Work"}" else "Search here",
                    query = query,
                    onQuery = { query = it },
                    onExpand = { sheetFull = true },
                    onClose = {
                        sheetFull = false
                        query = ""
                        listTitle = null
                        savingFor = null
                    },
                    onMic = { startMic() },
                    onProfile = { showProfile = true },
                    onSaved = { openSavedList() },
                    home = home,
                    work = work,
                    onHome = {
                        val h = home
                        if (h != null) openPlace(h) else {
                            savingFor = "home"
                            sheetFull = true
                        }
                    },
                    onWork = {
                        val w = work
                        if (w != null) openPlace(w) else {
                            savingFor = "work"
                            sheetFull = true
                        }
                    },
                    recents = recents,
                    results = results,
                    listTitle = listTitle,
                    listItems = listItems,
                    loadingList = loadingList,
                    onBackList = { listTitle = null },
                    onCategory = { title, cats -> searchCategory(title, cats) },
                    onPlace = { openPlace(it) },
                    distText = { distText(it) },
                )

                mode == "place" -> {
                    val d = dest
                    if (d != null) {
                        PlaceCard(
                            ui = ui,
                            place = d,
                            dist = distText(d),
                            onClose = { clearTrip() },
                            onDirections = {
                                mode = "plan"
                                fetchRoutes()
                            },
                            isSaved = saved.any { it.name == d.name && it.lat == d.lat && it.lon == d.lon },
                            onToggleSave = {
                                val on = Prefs.toggleSaved(ctx, d)
                                saved = Prefs.saved(ctx)
                                message = if (on) "Saved" else "Removed from Saved"
                            },
                            onSetHome = {
                                Prefs.savePlace(ctx, "home", d)
                                home = d
                                message = "Saved as Home"
                            },
                            onSetWork = {
                                Prefs.savePlace(ctx, "work", d)
                                work = d
                                message = "Saved as Work"
                            },
                        )
                    }
                }

                mode == "plan" -> DirectionsSheet(
                    ui = ui,
                    destName = dest?.name ?: "",
                    travel = travel,
                    routes = routes,
                    loading = planning,
                    etaText = eta(routes[travel]?.seconds ?: 0.0),
                    onTravel = {
                        travel = it
                        route = routes[it]
                        fitRoute()
                    },
                    onClose = {
                        mode = "place"
                        route = null
                        routes = emptyMap()
                    },
                    onGo = {
                        demo = false
                        mode = "drive"
                    },
                    onDemo = {
                        val r = routes["drive"]
                        if (r != null) {
                            travel = "drive"
                            route = r
                            demo = true
                            demoMe = r.line.first()
                            mode = "drive"
                        }
                    },
                    onAvoid = { showAvoid = true },
                )

                mode == "drive" -> SheetShell(ui) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(Geo.fmtTime(remainingSec), color = ui.ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text(
                                Geo.fmtDist(remaining) + if (demo) " · demo" else "",
                                color = ui.muted, fontSize = 14.sp,
                            )
                        }
                        PrimaryBtn("End", ui) { clearTrip() }
                    }
                }

                mode == "arrived" -> SheetShell(ui) {
                    Text("You've arrived", color = ui.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(dest?.name ?: "", color = ui.muted, fontSize = 15.sp, maxLines = 1)
                    PrimaryBtn("Done", ui, Modifier.fillMaxWidth()) { clearTrip() }
                }
            }
        }

        if (showDetails) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                DetailsSheet(ui, showBuildings, showLabels, is3d, { showBuildings = it }, { showLabels = it }, { if (it != is3d) toggle3d() }, { showDetails = false })
            }
        }
        if (showProfile) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                ProfileSheet(
                    ui = ui,
                    worldName = skin.name,
                    savedCount = saved.size + listOfNotNull(home, work).size,
                    onChangeWorld = {
                        showProfile = false
                        showWorlds = true
                    },
                    onClose = { showProfile = false },
                )
            }
        }

        if (showAvoid) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                AvoidSheet(
                    ui = ui,
                    tolls = avoidTolls,
                    highways = avoidHighways,
                    ferries = avoidFerries,
                    onTolls = { avoidTolls = it },
                    onHighways = { avoidHighways = it },
                    onFerries = { avoidFerries = it },
                    onClose = { showAvoid = false },
                )
            }
        }

        if (showWorlds) {
            WorldPicker(
                title = "Map Select",
                initialId = skin.id,
                buttonLabel = "Use this world",
                onContinue = {
                    onSkin(it)
                    showWorlds = false
                },
                onBack = { showWorlds = false },
            )
        }
    }
}

@Composable
private fun RoundButton(glyph: String, ui: Ui, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(ui.card).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(glyph, color = ui.ink, fontSize = 22.sp) }
}
