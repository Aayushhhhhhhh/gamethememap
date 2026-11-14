package app.overworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.overworld.Place
import app.overworld.Skins

private fun previewUi(skinId: String = "nightgrid"): Ui {
    val skin = Skins.byId(skinId)
    return Ui(
        card = if (skin.dark) Color(0xF2101626) else Color(0xF7FFFFFF),
        ink = inkFor(skin),
        muted = inkFor(skin).copy(alpha = 0.6f),
        accent = hex(skin.accent),
        tile = if (skin.dark) Color(0xFF1B2335) else Color(0xFFECEAE4),
        onInk = if (skin.dark) Color.Black else Color.White,
    )
}

private val demoPlace = Place("Sula Vineyards", "Gangapur Savargaon Road, Nashik", 20.0, 73.7)

@Preview(name = "Home sheet", widthDp = 411, heightDp = 800, showBackground = true, backgroundColor = 0xFF333333)
@Composable
fun HomeSheetPreview() {
    Box(Modifier.fillMaxSize().background(Color(0xFF333333)), contentAlignment = Alignment.BottomCenter) {
        HomeSheet(
            ui = previewUi(), full = false, maxHeight = 450.dp, hint = "Search here", query = "",
            onQuery = {}, onExpand = {}, onClose = {}, onMic = {}, onProfile = {},
            home = null, work = null, onHome = {}, onWork = {}, onSaved = {},
            recents = listOf(demoPlace), results = emptyList(), listTitle = null, listItems = emptyList(),
            loadingList = false, onBackList = {}, onCategory = { _, _ -> }, onPlace = {}, distText = { "8.4 km" },
        )
    }
}

@Preview(name = "Home sheet full", widthDp = 411, heightDp = 800)
@Composable
fun HomeSheetFullPreview() {
    Box(Modifier.fillMaxSize().background(Color(0xFF333333)), contentAlignment = Alignment.BottomCenter) {
        HomeSheet(
            ui = previewUi("paper"), full = true, maxHeight = 720.dp, hint = "Search here", query = "",
            onQuery = {}, onExpand = {}, onClose = {}, onMic = {}, onProfile = {},
            home = demoPlace, work = null, onHome = {}, onWork = {}, onSaved = {},
            recents = listOf(demoPlace, demoPlace), results = emptyList(), listTitle = null, listItems = emptyList(),
            loadingList = false, onBackList = {}, onCategory = { _, _ -> }, onPlace = {}, distText = { "8.4 km" },
        )
    }
}

@Preview(name = "Place card", widthDp = 411, heightDp = 400)
@Composable
fun PlaceCardPreview() {
    Box(Modifier.fillMaxSize().background(Color(0xFF333333)), contentAlignment = Alignment.BottomCenter) {
        PlaceCard(
            ui = previewUi(), place = demoPlace, dist = "8.4 km", onClose = {}, onDirections = {},
            isSaved = false, onToggleSave = {}, onSetHome = {}, onSetWork = {},
        )
    }
}

@Preview(name = "Map details", widthDp = 411, heightDp = 400)
@Composable
fun DetailsPreview() {
    Box(Modifier.fillMaxSize().background(Color(0xFF333333)), contentAlignment = Alignment.BottomCenter) {
        DetailsSheet(previewUi(), true, true, true, {}, {}, {}, {})
    }
}
