package app.overworld

/** A "world": the colours that turn real OpenStreetMap data into a game-looking map. */
data class Skin(
    val id: String,
    val name: String,
    val tagline: String,
    val dark: Boolean,
    val bg: String,
    val park: String,
    val water: String,
    val minor: String,
    val major: String,
    val motorway: String,
    val casing: String,
    val building: String,
    val label: String,
    val route: String,
    val me: String,
    val accent: String,
)

object Skins {
    val all = listOf(
        Skin(
            "nightgrid", "Night Grid", "Neon streets, crimson blocks", true,
            bg = "#05080d", park = "#07140f", water = "#06101c",
            minor = "#0f6f7e", major = "#1597ad", motorway = "#22d3ee",
            casing = "#021015", building = "#8c1030", label = "#d7e3f0",
            route = "#ffe14a", me = "#ffffff", accent = "#22d3ee",
        ),
        Skin(
            "terminal", "Terminal", "Green phosphor wireframe", true,
            bg = "#031206", park = "#06210d", water = "#031a0a",
            minor = "#145c28", major = "#1f9a40", motorway = "#7dff9c",
            casing = "#020b04", building = "#0b3d17", label = "#39ff6a",
            route = "#d4ff4a", me = "#39ff6a", accent = "#39ff6a",
        ),
        Skin(
            "dusty", "Dusty Trail", "Sun-baked frontier", true,
            bg = "#3a2f22", park = "#4a4a2a", water = "#2d3b3a",
            minor = "#8a7554", major = "#b89a62", motorway = "#e6c27a",
            casing = "#231b12", building = "#5a4733", label = "#e8d9b8",
            route = "#ffd166", me = "#ffffff", accent = "#e6a23c",
        ),
        Skin(
            "heat", "Heat Wave", "Violet haze racing", true,
            bg = "#1a0b33", park = "#231048", water = "#120a3a",
            minor = "#4a2a8a", major = "#7a3dd6", motorway = "#ff5cf0",
            casing = "#0d0620", building = "#3a1a6e", label = "#d9c2ff",
            route = "#ffd34d", me = "#ffffff", accent = "#b05cff",
        ),
        Skin(
            "midnight", "Midnight", "Calm night drive", true,
            bg = "#0f1420", park = "#13241f", water = "#0b2a44",
            minor = "#2a3550", major = "#3b4a73", motorway = "#ffc93c",
            casing = "#0a0e17", building = "#1d2536", label = "#9aa7c7",
            route = "#ffc93c", me = "#ffffff", accent = "#ffc93c",
        ),
        Skin(
            "neon", "Neon", "Arcade after dark", true,
            bg = "#0b0616", park = "#0f1f2a", water = "#10224a",
            minor = "#2b1b4d", major = "#5b2a9c", motorway = "#ff4fa3",
            casing = "#07030f", building = "#1b1038", label = "#cbb6ff",
            route = "#2ef2ff", me = "#ffffff", accent = "#ff4fa3",
        ),
        Skin(
            "arcade", "Arcade", "Bright and bold", false,
            bg = "#f3efe2", park = "#bfe3a8", water = "#8fd3ff",
            minor = "#ffffff", major = "#ffe08a", motorway = "#ff9f5a",
            casing = "#2b2b2b", building = "#e4dcc8", label = "#2b2b2b",
            route = "#e5383b", me = "#2b2b2b", accent = "#e5383b",
        ),
        Skin(
            "paper", "Paper", "Quiet and clean", false,
            bg = "#f6f4ef", park = "#dfe9d5", water = "#cfe0ee",
            minor = "#ffffff", major = "#fbfbf9", motorway = "#f1d9a8",
            casing = "#d8d4c8", building = "#e9e5db", label = "#6b675d",
            route = "#b3392e", me = "#3a3a3a", accent = "#b3392e",
        ),
    )

    fun byId(id: String?): Skin = all.firstOrNull { it.id == id } ?: all[0]
}

/**
 * Builds a MapLibre style (JSON) for a skin. Data: OpenFreeMap vector tiles (OpenStreetMap,
 * OpenMapTiles schema). The skin only changes colours/widths - the roads and buildings are real.
 */
object StyleBuilder {
    private const val EMPTY = """{"type":"FeatureCollection","features":[]}"""

    private const val TEMPLATE = """
{
 "version": 8,
 "name": "overworld",
 "glyphs": "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf",
 "sources": {
  "omt": {"type": "vector", "url": "https://tiles.openfreemap.org/planet"},
  "route": {"type": "geojson", "data": @@EMPTY@@},
  "me": {"type": "geojson", "data": @@EMPTY@@},
  "dest": {"type": "geojson", "data": @@EMPTY@@}
 },
 "layers": [
  {"id": "bg", "type": "background", "paint": {"background-color": "@@BG@@"}},
  {"id": "landcover", "type": "fill", "source": "omt", "source-layer": "landcover",
   "filter": ["in", "class", "wood", "grass"], "paint": {"fill-color": "@@PARK@@"}},
  {"id": "park", "type": "fill", "source": "omt", "source-layer": "park",
   "paint": {"fill-color": "@@PARK@@"}},
  {"id": "water", "type": "fill", "source": "omt", "source-layer": "water",
   "paint": {"fill-color": "@@WATER@@"}},

  {"id": "minor-case", "type": "line", "source": "omt", "source-layer": "transportation", "minzoom": 13,
   "filter": ["in", "class", "minor", "service", "track"],
   "layout": {"line-cap": "round", "line-join": "round"},
   "paint": {"line-color": "@@CASING@@",
    "line-width": ["interpolate", ["exponential", 1.4], ["zoom"], 13, 1.5, 16, 6, 20, 34]}},
  {"id": "major-case", "type": "line", "source": "omt", "source-layer": "transportation", "minzoom": 9,
   "filter": ["in", "class", "primary", "secondary", "tertiary", "trunk"],
   "layout": {"line-cap": "round", "line-join": "round"},
   "paint": {"line-color": "@@CASING@@",
    "line-width": ["interpolate", ["exponential", 1.4], ["zoom"], 9, 1.5, 14, 5, 16, 9, 20, 50]}},
  {"id": "motorway-case", "type": "line", "source": "omt", "source-layer": "transportation", "minzoom": 6,
   "filter": ["==", "class", "motorway"],
   "layout": {"line-cap": "round", "line-join": "round"},
   "paint": {"line-color": "@@CASING@@",
    "line-width": ["interpolate", ["exponential", 1.4], ["zoom"], 6, 1.5, 14, 6, 16, 11, 20, 58]}},

  {"id": "minor", "type": "line", "source": "omt", "source-layer": "transportation", "minzoom": 13,
   "filter": ["in", "class", "minor", "service", "track"],
   "layout": {"line-cap": "round", "line-join": "round"},
   "paint": {"line-color": "@@MINOR@@",
    "line-width": ["interpolate", ["exponential", 1.4], ["zoom"], 13, 0.5, 16, 4, 20, 28]}},
  {"id": "major", "type": "line", "source": "omt", "source-layer": "transportation", "minzoom": 9,
   "filter": ["in", "class", "primary", "secondary", "tertiary", "trunk"],
   "layout": {"line-cap": "round", "line-join": "round"},
   "paint": {"line-color": "@@MAJOR@@",
    "line-width": ["interpolate", ["exponential", 1.4], ["zoom"], 9, 0.8, 14, 3, 16, 7, 20, 44]}},
  {"id": "motorway", "type": "line", "source": "omt", "source-layer": "transportation", "minzoom": 6,
   "filter": ["==", "class", "motorway"],
   "layout": {"line-cap": "round", "line-join": "round"},
   "paint": {"line-color": "@@MOTORWAY@@",
    "line-width": ["interpolate", ["exponential", 1.4], ["zoom"], 6, 0.8, 14, 4, 16, 9, 20, 52]}},

  {"id": "buildings", "type": "fill-extrusion", "source": "omt", "source-layer": "building", "minzoom": 14,
   "paint": {"fill-extrusion-color": "@@BUILDING@@",
    "fill-extrusion-height": ["coalesce", ["get", "render_height"], 8],
    "fill-extrusion-base": ["coalesce", ["get", "render_min_height"], 0],
    "fill-extrusion-opacity": 0.92}},

  {"id": "place", "type": "symbol", "source": "omt", "source-layer": "place",
   "filter": ["in", "class", "city", "town", "village", "suburb", "neighbourhood"],
   "layout": {"text-field": ["get", "name"], "text-font": ["Noto Sans Bold"],
    "text-size": ["interpolate", ["linear"], ["zoom"], 8, 11, 16, 15], "text-max-width": 8},
   "paint": {"text-color": "@@LABEL@@", "text-halo-color": "@@BG@@", "text-halo-width": 1.5}},
  {"id": "road-name", "type": "symbol", "source": "omt", "source-layer": "transportation_name", "minzoom": 15,
   "layout": {"symbol-placement": "line", "text-field": ["get", "name"],
    "text-font": ["Noto Sans Regular"], "text-size": 12},
   "paint": {"text-color": "@@LABEL@@", "text-halo-color": "@@BG@@", "text-halo-width": 1.5}},

  {"id": "route-case", "type": "line", "source": "route",
   "layout": {"line-cap": "round", "line-join": "round"},
   "paint": {"line-color": "@@CASING@@",
    "line-width": ["interpolate", ["linear"], ["zoom"], 10, 6, 18, 20]}},
  {"id": "route", "type": "line", "source": "route",
   "layout": {"line-cap": "round", "line-join": "round"},
   "paint": {"line-color": "@@ROUTE@@",
    "line-width": ["interpolate", ["linear"], ["zoom"], 10, 3.5, 18, 12]}},
  {"id": "dest", "type": "circle", "source": "dest",
   "paint": {"circle-radius": 9, "circle-color": "@@ROUTE@@", "circle-stroke-width": 3,
    "circle-stroke-color": "#ffffff"}},
  {"id": "me", "type": "circle", "source": "me",
   "paint": {"circle-radius": 9, "circle-color": "@@ME@@", "circle-stroke-width": 4,
    "circle-stroke-color": "@@ROUTE@@", "circle-pitch-alignment": "map"}}
 ]
}
"""

    fun build(s: Skin): String = TEMPLATE
        .replace("@@EMPTY@@", EMPTY)
        .replace("@@BG@@", s.bg)
        .replace("@@PARK@@", s.park)
        .replace("@@WATER@@", s.water)
        .replace("@@MINOR@@", s.minor)
        .replace("@@MAJOR@@", s.major)
        .replace("@@MOTORWAY@@", s.motorway)
        .replace("@@CASING@@", s.casing)
        .replace("@@BUILDING@@", s.building)
        .replace("@@LABEL@@", s.label)
        .replace("@@ROUTE@@", s.route)
        .replace("@@ME@@", s.me)
}
