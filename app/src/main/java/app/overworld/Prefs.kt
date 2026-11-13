package app.overworld

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("overworld", Context.MODE_PRIVATE)

    fun onboarded(c: Context): Boolean = sp(c).getBoolean("onboarded", false)
    fun skin(c: Context): String = sp(c).getString("skin", "midnight") ?: "midnight"
    fun tipSeen(c: Context): Boolean = sp(c).getBoolean("tip_seen", false)

    fun finishOnboarding(c: Context, skinId: String) {
        sp(c).edit().putBoolean("onboarded", true).putString("skin", skinId).apply()
    }

    fun setSkin(c: Context, skinId: String) {
        sp(c).edit().putString("skin", skinId).apply()
    }

    fun setTipSeen(c: Context) {
        sp(c).edit().putBoolean("tip_seen", true).apply()
    }
}
