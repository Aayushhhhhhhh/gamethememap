package app.overworld

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Small geometry helpers. Points are DoubleArray(lon, lat). */
object Geo {
    private const val R = 6371000.0

    fun dist(a: DoubleArray, b: DoubleArray): Double {
        val p1 = Math.toRadians(a[1])
        val p2 = Math.toRadians(b[1])
        val dp = p2 - p1
        val dl = Math.toRadians(b[0] - a[0])
        val h = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * R * atan2(sqrt(h), sqrt(1 - h))
    }

    fun bearing(a: DoubleArray, b: DoubleArray): Double {
        val p1 = Math.toRadians(a[1])
        val p2 = Math.toRadians(b[1])
        val dl = Math.toRadians(b[0] - a[0])
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Cumulative distance (metres) at each vertex of the line. */
    fun cumulative(line: List<DoubleArray>): DoubleArray {
        val out = DoubleArray(line.size)
        for (i in 1 until line.size) out[i] = out[i - 1] + dist(line[i - 1], line[i])
        return out
    }

    fun nearestIndex(line: List<DoubleArray>, p: DoubleArray): Int {
        var best = 0
        var bestD = Double.MAX_VALUE
        for (i in line.indices) {
            val d = dist(line[i], p)
            if (d < bestD) {
                bestD = d
                best = i
            }
        }
        return best
    }

    /** Point at `s` metres along the line. */
    fun pointAt(line: List<DoubleArray>, cum: DoubleArray, s: Double): DoubleArray {
        if (s <= 0.0) return line.first()
        if (s >= cum.last()) return line.last()
        var i = 1
        while (i < cum.size && cum[i] < s) i++
        val seg = cum[i] - cum[i - 1]
        val t = if (seg <= 0.0) 0.0 else (s - cum[i - 1]) / seg
        val a = line[i - 1]
        val b = line[i]
        return doubleArrayOf(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)
    }

    fun lineJson(line: List<DoubleArray>): String {
        val sb = StringBuilder("""{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[""")
        line.forEachIndexed { i, p ->
            if (i > 0) sb.append(',')
            sb.append('[').append(p[0]).append(',').append(p[1]).append(']')
        }
        return sb.append("]}}").toString()
    }

    fun pointJson(lon: Double, lat: Double): String =
        """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[$lon,$lat]}}"""

    fun fmtDist(m: Double): String =
        if (m < 1000) "${(m / 10).toInt() * 10} m" else String.format(java.util.Locale.US, "%.1f km", m / 1000.0)

    fun fmtTime(sec: Double): String {
        val min = (sec / 60.0).toInt().coerceAtLeast(1)
        return if (min < 60) "$min min" else "${min / 60} h ${min % 60} min"
    }
}
