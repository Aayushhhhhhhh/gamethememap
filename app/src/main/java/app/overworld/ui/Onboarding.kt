package app.overworld.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.overworld.Skin
import app.overworld.Skins

fun hex(s: String): Color = Color(android.graphics.Color.parseColor(s))

fun inkFor(skin: Skin): Color = if (skin.dark) Color.White else Color(0xFF1C1C1C)

fun onAccent(accent: Color): Color = if (accent.luminance() > 0.5f) Color(0xFF1C1C1C) else Color.White

private val VIBES = listOf(
    "crime" to "Open-world crime",
    "wasteland" to "Wasteland survival",
    "fantasy" to "Fantasy quest",
    "racing" to "Street racing",
    "cyber" to "Cyber city",
    "space" to "Space explorer",
    "western" to "Wild west",
    "sandbox" to "Sandbox builder",
    "royale" to "Battle royale",
    "stealth" to "Stealth missions",
    "retro" to "Retro arcade",
    "horror" to "Survival horror",
)

private val BG = Brush.verticalGradient(listOf(Color(0xFF0A1A3C), Color(0xFF03050A), Color.Black))

/**
 * Flow (no sign-up, one decision per screen, a progress bar so it feels short):
 * 3 intro slides -> vibe quiz -> pick a world (swipe) -> location ask.
 */
@Composable
fun Onboarding(initialSkin: String, onDone: (String, Set<String>) -> Unit) {
    var step by remember { mutableStateOf(0) }
    var skinId by remember { mutableStateOf(initialSkin) }
    var vibes by remember { mutableStateOf(setOf<String>()) }
    val total = 6
    val accent = hex("#22d3ee")

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        onDone(skinId, vibes)
    }

    if (step == 4) {
        WorldPicker(
            title = "Select world to start",
            initialId = skinId,
            buttonLabel = "Continue",
            progress = 5f / total,
            onContinue = {
                skinId = it
                step = 5
            },
            onBack = { step = 3 },
        )
        return
    }

    Column(
        Modifier.fillMaxSize().background(BG).systemBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ProgressBar((step + 1).toFloat() / total, accent)

        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            when (step) {
                0 -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { EmojiGrid(accent) }
                    Headline("Your real world,\nas a game map", "Roads and buildings are real. The look is all yours.")
                }
                1 -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CarArt() }
                    Headline("Built for your\ncar screen", "Works with Android Auto-enabled cars.")
                }
                2 -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { WorldTiles() }
                    Headline("Pick a world that\nfits your mood", "Night city, wasteland, wild west and more.")
                }
                3 -> {
                    Text(
                        "What's your vibe?", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    )
                    Text(
                        "Pick all that apply.", color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    )
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        VIBES.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { (id, label) ->
                                    val on = id in vibes
                                    Box(
                                        Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
                                            .background(if (on) accent.copy(alpha = 0.22f) else Color(0x22FFFFFF))
                                            .border(2.dp, if (on) accent else Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                                            .clickable { vibes = if (on) vibes - id else vibes + id }
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("📍", fontSize = 90.sp)
                    }
                    Headline(
                        "Allow location for\nOverworld",
                        "Your location is used to show where you are and to plan routes. You can still search without it.",
                    )
                }
            }
        }

        Column(Modifier.padding(top = 16.dp)) {
            val canGo = step != 3 || vibes.isNotEmpty()
            Button(
                onClick = {
                    when (step) {
                        5 -> permLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                        3 -> step = 4
                        else -> step += 1
                    }
                },
                enabled = canGo,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent, contentColor = Color(0xFF031015),
                    disabledContainerColor = Color(0x33FFFFFF), disabledContentColor = Color(0x66FFFFFF),
                ),
            ) {
                Text(if (step == 5) "Allow location" else "Continue  →", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            if (step == 5) {
                Text(
                    "Not now", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { onDone(skinId, vibes) }.padding(top = 14.dp, bottom = 4.dp),
                )
            } else if (step == 3) {
                Text(
                    "Pick at least one to continue", color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            } else {
                Text(
                    "No account needed", color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, accent: Color) {
    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x22FFFFFF))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(4.dp).background(accent))
    }
}

@Composable
private fun Headline(title: String, sub: String) {
    Text(
        title, color = Color.White, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
    Text(
        sub, color = Color.White.copy(alpha = 0.65f), fontSize = 15.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
    )
}

@Composable
private fun EmojiGrid(accent: Color) {
    val icons = listOf("🍺", "🏠", "🥖", "🎡", "🏀", "💎", "☕", "🧱", "🍕", "🍸", "💵", "🚗")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        icons.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach {
                    Box(
                        Modifier.size(76.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x22FFFFFF))
                            .border(1.5.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(it, fontSize = 36.sp) }
                }
            }
        }
    }
}

@Composable
private fun CarArt() {
    val s = Skins.byId("nightgrid")
    Box(contentAlignment = Alignment.BottomEnd) {
        SkinPreview(
            s,
            Modifier.width(300.dp).height(170.dp).clip(RoundedCornerShape(18.dp))
                .border(6.dp, Color(0xFF1B2233), RoundedCornerShape(18.dp)),
        )
        SkinPreview(
            Skins.byId("terminal"),
            Modifier.padding(end = 4.dp, bottom = 4.dp).width(70.dp).height(120.dp).clip(RoundedCornerShape(12.dp))
                .border(3.dp, Color.White, RoundedCornerShape(12.dp)),
        )
    }
}

@Composable
private fun WorldTiles() {
    val ids = listOf("nightgrid", "terminal", "dusty", "heat")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ids.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEach { SkinPreview(Skins.byId(it), Modifier.size(130.dp).clip(CircleShape)) }
            }
        }
    }
}

/** Swipeable world chooser (also used from the map's world button). */
@Composable
fun WorldPicker(
    title: String,
    initialId: String,
    buttonLabel: String,
    progress: Float? = null,
    onContinue: (String) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val worlds = Skins.all
    var idx by remember { mutableStateOf(worlds.indexOfFirst { it.id == initialId }.coerceAtLeast(0)) }
    var drag by remember { mutableStateOf(0f) }
    val skin = worlds[idx]
    val accent = hex(skin.accent)

    fun next() { idx = (idx + 1) % worlds.size }
    fun prev() { idx = (idx - 1 + worlds.size) % worlds.size }

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black, hex(skin.bg))))
            .systemBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (progress != null) ProgressBar(progress, accent)
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Text(
                    "←", color = Color.White, fontSize = 26.sp,
                    modifier = Modifier.clip(CircleShape).clickable { onBack() }.padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }
            Text(
                title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            if (onBack != null) Spacer(Modifier.width(44.dp))
        }

        Box(
            Modifier.weight(1f).fillMaxWidth().pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = {
                        if (drag < -80f) next() else if (drag > 80f) prev()
                    },
                    onHorizontalDrag = { _, dx -> drag += dx },
                )
            },
            contentAlignment = Alignment.Center,
        ) {
            SkinPreview(
                skin,
                Modifier.size(290.dp).clip(CircleShape).border(3.dp, accent.copy(alpha = 0.7f), CircleShape),
            )
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundArrow("←") { prev() }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(skin.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(skin.tagline, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
            }
            RoundArrow("→") { next() }
        }
        Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            worlds.forEachIndexed { i, _ ->
                Box(
                    Modifier.height(6.dp).width(if (i == idx) 20.dp else 6.dp).clip(CircleShape)
                        .background(if (i == idx) Color.White else Color(0x55FFFFFF))
                )
            }
        }
        Button(
            onClick = { onContinue(skin.id) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) { Text("$buttonLabel  →", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun RoundArrow(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(Color(0x33FFFFFF)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(glyph, color = Color.White, fontSize = 22.sp) }
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
        drawRect(hex(skin.building), topLeft = Offset(w * 0.12f, h * 0.55f), size = Size(w * 0.2f, h * 0.14f))

        fun road(a: Offset, b: Offset, color: Color, width: Float) {
            drawLine(hex(skin.casing), a, b, strokeWidth = width + 4f, cap = StrokeCap.Round)
            drawLine(color, a, b, strokeWidth = width, cap = StrokeCap.Round)
        }
        road(Offset(0f, h * 0.55f), Offset(w, h * 0.45f), hex(skin.major), 12f)
        road(Offset(w * 0.45f, 0f), Offset(w * 0.5f, h * 0.8f), hex(skin.minor), 8f)
        road(Offset(0f, h * 0.2f), Offset(w, h * 0.25f), hex(skin.minor), 7f)
        road(Offset(w * 0.78f, h * 0.4f), Offset(w * 0.8f, h), hex(skin.motorway), 9f)

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
