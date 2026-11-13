package app.overworld.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.overworld.Skin
import app.overworld.Skins

fun hex(s: String): Color = Color(android.graphics.Color.parseColor(s))

fun inkFor(skin: Skin): Color = if (skin.dark) Color.White else Color(0xFF1C1C1C)

fun onAccent(accent: Color): Color = if (accent.luminance() > 0.5f) Color(0xFF1C1C1C) else Color.White

/**
 * Two screens, no sign-up:
 *  1. one promise + three short benefits
 *  2. pick your world (the background already switches, so the choice feels like yours)
 */
@Composable
fun Onboarding(initialSkin: String, onDone: (String) -> Unit) {
    var step by remember { mutableStateOf(0) }
    var skinId by remember { mutableStateOf(initialSkin) }
    val skin = Skins.byId(skinId)
    val ink = inkFor(skin)
    val accent = hex(skin.accent)

    Box(
        Modifier
            .fillMaxSize()
            .background(hex(skin.bg))
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("OVERWORLD", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.height(6.dp))
                Text("${step + 1} of 2", color = ink.copy(alpha = 0.55f), fontSize = 12.sp)
            }

            if (step == 0) {
                Column {
                    Text(
                        "Make every drive feel like a game.",
                        color = ink, fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(24.dp))
                    Benefit("Real roads, drawn like a game map", ink, accent)
                    Benefit("Pick a world that fits your mood", ink, accent)
                    Benefit("Made to work on your car screen", ink, accent)
                }
            } else {
                Column {
                    Text("Choose your world", color = ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("You can change it any time.", color = ink.copy(alpha = 0.65f), fontSize = 15.sp)
                    Spacer(Modifier.height(16.dp))
                    Skins.all.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { s ->
                                SkinCard(s, selected = s.id == skinId, modifier = Modifier.weight(1f)) { skinId = s.id }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            Column {
                Button(
                    onClick = { if (step == 0) step = 1 else onDone(skinId) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent(accent)),
                ) {
                    Text(
                        if (step == 0) "Let's go" else "Start in ${skin.name}",
                        fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (step == 0) "No account needed." else "Your location is only asked when you tap the locate button.",
                    color = ink.copy(alpha = 0.55f), fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Benefit(text: String, ink: Color, accent: Color) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.size(12.dp))
        Text(text, color = ink, fontSize = 17.sp)
    }
}

@Composable
private fun SkinCard(skin: Skin, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val accent = hex(skin.accent)
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .border(BorderStroke(if (selected) 3.dp else 1.dp, if (selected) accent else Color(0x33808080)), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        SkinPreview(skin, Modifier.fillMaxWidth().height(96.dp))
        Column(Modifier.background(hex(skin.bg)).fillMaxWidth().padding(10.dp)) {
            Text(skin.name, color = inkFor(skin), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(skin.tagline, color = inkFor(skin).copy(alpha = 0.65f), fontSize = 12.sp)
        }
    }
}

/** A tiny hand-drawn map so each world can be previewed without loading tiles. */
@Composable
fun SkinPreview(skin: Skin, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(hex(skin.bg))
        drawRect(hex(skin.water), topLeft = Offset(0f, h * 0.74f), size = Size(w, h * 0.26f))
        drawRect(hex(skin.park), topLeft = Offset(w * 0.07f, h * 0.10f), size = Size(w * 0.26f, h * 0.30f))
        drawRect(hex(skin.building), topLeft = Offset(w * 0.64f, h * 0.12f), size = Size(w * 0.24f, h * 0.24f))

        fun road(a: Offset, b: Offset, color: Color, width: Float) {
            drawLine(hex(skin.casing), a, b, strokeWidth = width + 4f, cap = StrokeCap.Round)
            drawLine(color, a, b, strokeWidth = width, cap = StrokeCap.Round)
        }
        road(Offset(0f, h * 0.55f), Offset(w, h * 0.45f), hex(skin.major), 12f)
        road(Offset(w * 0.45f, 0f), Offset(w * 0.5f, h * 0.8f), hex(skin.minor), 8f)
        road(Offset(0f, h * 0.2f), Offset(w, h * 0.25f), hex(skin.minor), 7f)

        drawLine(
            hex(skin.route), Offset(w * 0.12f, h * 0.9f), Offset(w * 0.5f, h * 0.52f),
            strokeWidth = 7f, cap = StrokeCap.Round,
        )
        drawLine(
            hex(skin.route), Offset(w * 0.5f, h * 0.52f), Offset(w * 0.86f, h * 0.44f),
            strokeWidth = 7f, cap = StrokeCap.Round,
        )
        drawCircle(hex(skin.me), radius = 8.dp.toPx(), center = Offset(w * 0.12f, h * 0.9f))
        drawCircle(hex(skin.route), radius = 8.dp.toPx(), center = Offset(w * 0.86f, h * 0.44f))
    }
}
