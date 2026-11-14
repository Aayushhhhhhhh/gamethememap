package app.overworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.overworld.Geo
import app.overworld.Place
import app.overworld.Route

/** Colours for the current world, shared by every sheet. */
class Ui(
    val card: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val tile: Color,
    val onInk: Color,
)

class Cat(val label: String, val glyph: String, val apiCats: String)

val POPULAR = listOf(
    Cat("Gas Station", "⛽", "service.vehicle.fuel"),
    Cat("Restaurant", "🍴", "catering.restaurant"),
    Cat("Coffee", "☕", "catering.cafe"),
    Cat("Hotel", "🛏", "accommodation.hotel"),
    Cat("Parking", "🅿", "parking"),
    Cat("Pharmacy", "💊", "healthcare.pharmacy"),
    Cat("Supermarket", "🛒", "commercial.supermarket"),
    Cat("Hospital", "🏥", "healthcare.hospital"),
    Cat("Bank", "🏦", "service.financial.bank"),
    Cat("Charging", "🔌", "service.vehicle.charging_station"),
)

val RECS = listOf(
    Cat("Popular restaurants", "🍽", "catering.restaurant"),
    Cat("Coffee spots", "☕", "catering.cafe"),
    Cat("Shopping nearby", "🛍", "commercial.shopping_mall"),
)

private val TOP = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
fun SheetShell(ui: Ui, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(TOP).background(ui.card).navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
fun PrimaryBtn(label: String, ui: Ui, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ui.accent, contentColor = onAccent(ui.accent)),
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun TextBtn(label: String, color: Color, onClick: () -> Unit) {
    Text(
        label, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp),
    )
}

@Composable
fun CloseBtn(ui: Ui, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(ui.tile).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text("✕", color = ui.ink, fontSize = 15.sp) }
}

@Composable
private fun Chip(label: String, ui: Ui, onClick: () -> Unit) {
    Text(
        label, color = ui.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(ui.tile).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun PlaceRow(p: Place, dist: String, ui: Ui, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(ui.tile), contentAlignment = Alignment.Center) {
            Text("📍", fontSize = 18.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(p.name, color = ui.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (p.sub.isNotEmpty()) Text(p.sub, color = ui.muted, fontSize = 13.sp, maxLines = 1)
        }
        if (dist.isNotEmpty()) Text(dist, color = ui.muted, fontSize = 13.sp)
    }
}

/**
 * Home bottom sheet (from the paper sketch): handle, search + mic + profile button,
 * Home / Work / Saved / See all chips, Recent, Recommendations. Typing swaps the content
 * for live search results; category taps show nearby places.
 */
@Composable
fun HomeSheet(
    ui: Ui,
    full: Boolean,
    maxHeight: Dp,
    hint: String,
    query: String,
    onQuery: (String) -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    onMic: () -> Unit,
    onProfile: () -> Unit,
    home: Place?,
    work: Place?,
    onHome: () -> Unit,
    onWork: () -> Unit,
    onSaved: () -> Unit,
    recents: List<Place>,
    results: List<Place>,
    listTitle: String?,
    listItems: List<Place>,
    loadingList: Boolean,
    onBackList: () -> Unit,
    onCategory: (String, String) -> Unit,
    onPlace: (Place) -> Unit,
    distText: (Place) -> String,
) {
    val focus = LocalFocusManager.current
    Column(
        Modifier.fillMaxWidth().heightIn(max = maxHeight).clip(TOP).background(ui.card)
            .navigationBarsPadding().then(if (full) Modifier.imePadding() else Modifier)
    ) {
        Box(
            Modifier.fillMaxWidth().clickable { if (full) onClose() else onExpand() }.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ui.muted.copy(alpha = 0.5f))) }

        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(26.dp)).background(ui.tile)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = TextStyle(color = ui.ink, fontSize = 17.sp),
                    cursorBrush = SolidColor(ui.accent),
                    modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onExpand() },
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) Text(hint, color = ui.muted, fontSize = 17.sp)
                            inner()
                        }
                    },
                )
                if (query.isNotEmpty() || full) {
                    Text(
                        "✕", color = ui.muted, fontSize = 16.sp,
                        modifier = Modifier.clip(CircleShape).clickable {
                            focus.clearFocus()
                            onClose()
                        }.padding(4.dp),
                    )
                } else {
                    Text("🎤", fontSize = 18.sp, modifier = Modifier.clip(CircleShape).clickable(onClick = onMic).padding(4.dp))
                }
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(ui.tile).clickable(onClick = onProfile),
                contentAlignment = Alignment.Center,
            ) { Text("👤", fontSize = 20.sp) }
        }

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(if (home != null) "🏠 Home" else "🏠 Add Home", ui, onHome)
                Chip(if (work != null) "💼 Work" else "💼 Add Work", ui, onWork)
                Chip("🔖 Saved", ui, onSaved)
                Chip("See all ›", ui, onSaved)
            }

            if (query.length >= 3) {
                if (results.isEmpty()) Text("Searching...", color = ui.muted, fontSize = 14.sp)
                results.forEach { PlaceRow(it, distText(it), ui) { onPlace(it) } }
            } else if (listTitle != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "‹  $listTitle", color = ui.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onBackList).padding(4.dp),
                    )
                }
                if (loadingList) Text("Looking nearby...", color = ui.muted, fontSize = 14.sp)
                else if (listItems.isEmpty()) Text("Nothing here yet.", color = ui.muted, fontSize = 14.sp)
                listItems.forEach { PlaceRow(it, distText(it), ui) { onPlace(it) } }
            } else {
                if (recents.isNotEmpty()) {
                    Text(
                        "Recent ›", color = ui.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onExpand).padding(vertical = 2.dp),
                    )
                    (if (full) recents else recents.take(1)).forEach { r ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .border(1.5.dp, ui.muted.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .clickable { onPlace(r) }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(ui.tile), contentAlignment = Alignment.Center) {
                                Text("🕘", fontSize = 18.sp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(r.name, color = ui.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                if (r.sub.isNotEmpty()) Text(r.sub, color = ui.muted, fontSize = 13.sp, maxLines = 1)
                            }
                        }
                    }
                }
                Text("Recommendations", color = ui.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RECS.take(2).forEach { c ->
                        Box(
                            Modifier.weight(1f).height(150.dp).clip(RoundedCornerShape(20.dp))
                                .background(Brush.linearGradient(listOf(ui.accent.copy(alpha = 0.55f), ui.tile)))
                                .clickable { onCategory(c.label, c.apiCats) }.padding(14.dp),
                        ) {
                            Text(c.glyph, fontSize = 38.sp, modifier = Modifier.align(Alignment.TopStart))
                            Text(
                                c.label, color = ui.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.BottomStart),
                            )
                        }
                    }
                }
                Text("Popular Locations", color = ui.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                POPULAR.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { c ->
                            Column(
                                Modifier.weight(1f).aspectRatio(1.1f).clip(RoundedCornerShape(18.dp))
                                    .background(ui.tile).clickable { onCategory(c.label, c.apiCats) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(c.glyph, fontSize = 26.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(c.label, color = ui.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceCard(
    ui: Ui,
    place: Place,
    dist: String,
    onClose: () -> Unit,
    onDirections: () -> Unit,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onSetHome: () -> Unit,
    onSetWork: () -> Unit,
) {
    SheetShell(ui) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                place.name, color = ui.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            CloseBtn(ui, onClose)
        }
        if (place.sub.isNotEmpty()) Text(place.sub, color = ui.muted, fontSize = 14.sp, maxLines = 2)
        if (dist.isNotEmpty()) {
            Row(
                Modifier.clip(RoundedCornerShape(14.dp)).background(ui.tile).padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("📍 $dist away", color = ui.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        PrimaryBtn("Directions", ui, Modifier.fillMaxWidth(), onClick = onDirections)
        Row {
            TextBtn(if (isSaved) "★ Saved" else "☆ Save", ui.muted, onToggleSave)
            TextBtn("Set as Home", ui.muted, onSetHome)
            TextBtn("Set as Work", ui.muted, onSetWork)
        }
    }
}

@Composable
fun DirectionsSheet(
    ui: Ui,
    destName: String,
    travel: String,
    routes: Map<String, Route?>,
    loading: Boolean,
    etaText: String,
    onTravel: (String) -> Unit,
    onClose: () -> Unit,
    onGo: () -> Unit,
    onDemo: () -> Unit,
    onAvoid: () -> Unit,
) {
    val route = routes[travel]
    SheetShell(ui) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Directions", color = ui.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            CloseBtn(ui, onClose)
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(ui.tile).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("drive" to "🚗", "bicycle" to "🚲", "walk" to "🚶").forEach { (id, glyph) ->
                val r = routes[id]
                val on = id == travel
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                        .background(if (on) ui.ink else Color.Transparent)
                        .clickable { onTravel(id) }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$glyph ${if (r != null) Geo.fmtTime(r.seconds) else if (loading) "…" else "–"}",
                        color = if (on) ui.onInk else ui.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ui.tile).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                Text("My Location", color = ui.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("📍", fontSize = 14.sp)
                Column {
                    Text(destName, color = ui.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    if (route != null) Text("Estimated arrival $etaText", color = ui.muted, fontSize = 13.sp)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(2.dp, ui.accent, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (route != null) Geo.fmtTime(route.seconds) else if (loading) "Finding route..." else "No route",
                color = ui.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
            )
            if (route != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("$etaText arrival", color = ui.ink, fontSize = 14.sp)
                    Text(Geo.fmtDist(route.meters), color = ui.muted, fontSize = 13.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(ui.tile).clickable(onClick = onAvoid),
                contentAlignment = Alignment.Center,
            ) { Text("⚙", color = ui.ink, fontSize = 20.sp) }
            PrimaryBtn(
                if (route != null) "Go · ${Geo.fmtTime(route.seconds)}" else "Go", ui,
                Modifier.weight(1f), enabled = route != null && travel == "drive", onClick = onGo,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (travel == "drive") "" else "Turn-by-turn is for driving only.",
                color = ui.muted, fontSize = 12.sp, modifier = Modifier.weight(1f),
            )
            if (route != null) TextBtn("Demo drive", ui.muted, onDemo)
        }
    }
}

@Composable
fun AvoidSheet(
    ui: Ui,
    tolls: Boolean,
    highways: Boolean,
    ferries: Boolean,
    onTolls: (Boolean) -> Unit,
    onHighways: (Boolean) -> Unit,
    onFerries: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    SheetShell(ui) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Avoid for all drives", color = ui.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            CloseBtn(ui, onClose)
        }
        listOf(
            Triple("Avoid Toll Roads", tolls, onTolls),
            Triple("Avoid Highways", highways, onHighways),
            Triple("Avoid Ferries", ferries, onFerries),
        ).forEach { (label, on, set) ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ui.tile).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = ui.ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Switch(checked = on, onCheckedChange = set)
            }
        }
    }
}

@Composable
fun DetailsSheet(
    ui: Ui,
    buildings: Boolean,
    labels: Boolean,
    onBuildings: (Boolean) -> Unit,
    onLabels: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    SheetShell(ui) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Map details", color = ui.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            CloseBtn(ui, onClose)
        }
        listOf(
            Triple("3D buildings", buildings, onBuildings),
            Triple("Place and road names", labels, onLabels),
        ).forEach { (label, on, set) ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ui.tile).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = ui.ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Switch(checked = on, onCheckedChange = set)
            }
        }
    }
}

@Composable
fun ProfileSheet(
    ui: Ui,
    worldName: String,
    savedCount: Int,
    onChangeWorld: () -> Unit,
    onClose: () -> Unit,
) {
    SheetShell(ui) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Profile", color = ui.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            CloseBtn(ui, onClose)
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ui.tile).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Your world: $worldName", color = ui.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Saved places: $savedCount", color = ui.muted, fontSize = 14.sp)
        }
        PrimaryBtn("Change world", ui, Modifier.fillMaxWidth(), onClick = onChangeWorld)
        Text("Accounts and sync are coming later.", color = ui.muted, fontSize = 12.sp)
    }
}
