package app.overworld

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Place(val name: String, val sub: String, val lat: Double, val lon: Double)

data class Step(val text: String, val fromIndex: Int, val distance: Double)

data class Route(
    val line: List<DoubleArray>, // each = (lon, lat)
    val steps: List<Step>,
    val meters: Double,
    val seconds: Double,
)

/**
 * Search + routing live behind this interface so the provider can be swapped
 * (Geoapify now, self-hosted Photon + Valhalla later) without touching the UI.
 */
interface GeoProvider {
    suspend fun search(query: String, nearLat: Double?, nearLon: Double?): List<Place>
    suspend fun route(
        fromLat: Double, fromLon: Double, toLat: Double, toLon: Double,
        mode: String = "drive", avoid: List<String> = emptyList(),
    ): Route?

    suspend fun nearby(categories: String, lat: Double, lon: Double, radiusM: Int): List<Place>
}

class GeoapifyProvider(private val key: String) : GeoProvider {

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10000
            c.readTimeout = 15000
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    override suspend fun search(query: String, nearLat: Double?, nearLon: Double?): List<Place> {
        val q = URLEncoder.encode(query, "UTF-8")
        val bias = if (nearLat != null && nearLon != null) "&bias=proximity:$nearLon,$nearLat" else ""
        val body = get("https://api.geoapify.com/v1/geocode/autocomplete?text=$q&limit=5&format=json$bias&apiKey=$key")
        val arr = JSONObject(body).optJSONArray("results") ?: return emptyList()
        val out = ArrayList<Place>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (!o.has("lat") || !o.has("lon")) continue
            val line1 = o.optString("address_line1", o.optString("name", ""))
            val line2 = o.optString("address_line2", "")
            out.add(Place(line1.ifEmpty { o.optString("formatted") }, line2, o.getDouble("lat"), o.getDouble("lon")))
        }
        return out
    }

    override suspend fun nearby(categories: String, lat: Double, lon: Double, radiusM: Int): List<Place> {
        val body = get(
            "https://api.geoapify.com/v2/places?categories=$categories" +
                "&filter=circle:$lon,$lat,$radiusM&bias=proximity:$lon,$lat&limit=20&apiKey=$key"
        )
        val feats = JSONObject(body).optJSONArray("features") ?: return emptyList()
        val out = ArrayList<Place>()
        for (i in 0 until feats.length()) {
            val pr = feats.getJSONObject(i).optJSONObject("properties") ?: continue
            val name = pr.optString("name", "")
            if (name.isEmpty() || !pr.has("lat") || !pr.has("lon")) continue
            val sub = pr.optString("address_line2", pr.optString("formatted", ""))
            out.add(Place(name, sub, pr.getDouble("lat"), pr.getDouble("lon")))
        }
        return out
    }

    override suspend fun route(
        fromLat: Double, fromLon: Double, toLat: Double, toLon: Double,
        mode: String, avoid: List<String>,
    ): Route? {
        val avoidParam = if (avoid.isEmpty()) "" else "&avoid=" + avoid.joinToString("%7C")
        val body = get(
            "https://api.geoapify.com/v1/routing?waypoints=$fromLat,$fromLon%7C$toLat,$toLon" +
                "&mode=$mode$avoidParam&apiKey=$key"
        )
        val feats = JSONObject(body).optJSONArray("features") ?: return null
        if (feats.length() == 0) return null
        val f = feats.getJSONObject(0)
        val props = f.getJSONObject("properties")
        val geom = f.getJSONObject("geometry")
        val coords = geom.getJSONArray("coordinates")
        val pts = ArrayList<DoubleArray>()
        if (geom.getString("type") == "MultiLineString") {
            for (p in 0 until coords.length()) {
                val part = coords.getJSONArray(p)
                for (i in 0 until part.length()) {
                    if (p > 0 && i == 0) continue // shared vertex between legs
                    val c = part.getJSONArray(i)
                    pts.add(doubleArrayOf(c.getDouble(0), c.getDouble(1)))
                }
            }
        } else {
            for (i in 0 until coords.length()) {
                val c = coords.getJSONArray(i)
                pts.add(doubleArrayOf(c.getDouble(0), c.getDouble(1)))
            }
        }
        if (pts.size < 2) return null

        val steps = ArrayList<Step>()
        val legs = props.optJSONArray("legs")
        if (legs != null && legs.length() > 0) {
            val st = legs.getJSONObject(0).optJSONArray("steps")
            if (st != null) {
                for (i in 0 until st.length()) {
                    val s = st.getJSONObject(i)
                    val text = s.optJSONObject("instruction")?.optString("text").orEmpty()
                    steps.add(Step(text, s.optInt("from_index", 0), s.optDouble("distance", 0.0)))
                }
            }
        }
        return Route(pts, steps, props.optDouble("distance", 0.0), props.optDouble("time", 0.0))
    }
}
