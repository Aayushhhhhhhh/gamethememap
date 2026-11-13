package app.overworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import app.overworld.ui.MapScreen
import app.overworld.ui.Onboarding

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val ctx = this
        setContent {
            var onboarded by remember { mutableStateOf(Prefs.onboarded(ctx)) }
            var skinId by remember { mutableStateOf(Prefs.skin(ctx)) }

            if (!onboarded) {
                Onboarding(initialSkin = skinId) { chosen, vibes ->
                    Prefs.finishOnboarding(ctx, chosen, vibes)
                    skinId = chosen
                    onboarded = true
                }
            } else {
                MapScreen(
                    skinId = skinId,
                    onSkin = {
                        skinId = it
                        Prefs.setSkin(ctx, it)
                    },
                )
            }
        }
    }
}
