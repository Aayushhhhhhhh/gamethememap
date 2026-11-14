package app.overworld

import android.content.Context

object Prefs {
    private const val F = "\u001f" // field separator
    private const val R = "\u001e" // record separator

    private fun sp(c: Context) = c.getSharedPreferences("overworld", Context.MODE_PRIVATE)

    // v2 = the redesigned onboarding, so people on older builds see it once.
    fun onboarded(c: Context): Boolean = sp(c).getBoolean("onboarded_v2", false)
    fun skin(c: Context): String = sp(c).getString("skin", "nightgrid") ?: "nightgrid"

    fun finishOnboarding(c: Context, skinId: String, vibes: Set<String>) {
        sp(c).edit()
            .putBoolean("onboarded_v2", true)
            .putString("skin", skinId)
            .putStringSet("vibes", vibes)
            .apply()
    }

    fun setSkin(c: Context, skinId: String) {
        sp(c).edit().putString("skin", skinId).apply()
    }

    private fun enc(p: Place) = listOf(p.name, p.sub, p.lat.toString(), p.lon.toString()).joinToString(F)

    private fun dec(s: String): Place? {
        val a = s.split(F)
        if (a.size != 4) return null
        val lat = a[2].toDoubleOrNull() ?: return null
        val lon = a[3].toDoubleOrNull() ?: return null
        return Place(a[0], a[1], lat, lon)
    }

    fun place(c: Context, key: String): Place? = sp(c).getString("place_$key", null)?.let { dec(it) }

    fun savePlace(c: Context, key: String, p: Place) {
        sp(c).edit().putString("place_$key", enc(p)).apply()
    }

    fun recents(c: Context): List<Place> =
        (sp(c).getString("recents", "") ?: "").split(R).filter { it.isNotEmpty() }.mapNotNull { dec(it) }

    fun addRecent(c: Context, p: Place) {
        val list = (listOf(p) + recents(c).filter { it.name != p.name }).take(5)
        sp(c).edit().putString("recents", list.joinToString(R) { enc(it) }).apply()
    }

    fun saved(c: Context): List<Place> =
        (sp(c).getString("saved", "") ?: "").split(R).filter { it.isNotEmpty() }.mapNotNull { dec(it) }

    /** Adds the place if missing, removes it if present. Returns true if it is saved afterwards. */
    fun toggleSaved(c: Context, p: Place): Boolean {
        val cur = saved(c)
        val has = cur.any { it.name == p.name && it.lat == p.lat && it.lon == p.lon }
        val next = if (has) cur.filterNot { it.name == p.name && it.lat == p.lat && it.lon == p.lon } else listOf(p) + cur
        sp(c).edit().putString("saved", next.joinToString(R) { enc(it) }).apply()
        return !has
    }
}
