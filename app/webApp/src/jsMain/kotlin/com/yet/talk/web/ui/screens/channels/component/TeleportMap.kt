package com.yet.talk.web.ui.screens.channels.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.yet.talk.domain.model.channel.ChannelLevel
import com.yet.talk.domain.model.channel.Geohash
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement

@JsModule("leaflet")
@JsNonModule
private external val leaflet: dynamic

@JsModule("leaflet/dist/leaflet.css")
@JsNonModule
private external val leafletCss: dynamic

/**
 * The teleport map (`Teleport`): OpenStreetMap tiles; a tap picks the area at [level] there and outlines its cell.
 * The tile server sees which part of the map you look at, never your own location.
 */
@Composable
fun TeleportMap(code: String, level: ChannelLevel, label: String, onPick: (String) -> Unit) {
    leafletCss // loads Leaflet's stylesheet with the map
    val pick by rememberUpdatedState(onPick)
    val currentLevel by rememberUpdatedState(level)
    var map by remember { mutableStateOf<dynamic>(null) }
    var cell by remember { mutableStateOf<dynamic>(null) }
    Div({
        classes("t-map-view")
        attr("role", "application")
        attr("aria-label", label)
        ref { element: HTMLElement ->
            val options: dynamic = js("({ worldCopyJump: true, zoomControl: true, attributionControl: true })")
            val created = leaflet.map(element, options).setView(arrayOf(20, 0), 2)
            val tiles: dynamic = js("({ maxZoom: 18, attribution: '&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors' })")
            leaflet.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", tiles).addTo(created)
            created.on("click") { event: dynamic ->
                pick(Geohash.encode(event.latlng.lat as Double, event.latlng.lng as Double, currentLevel.precision))
            }
            map = created
            onDispose {
                created.remove()
                map = null
            }
        }
    })
    // The picked or typed area, outlined and in view.
    DisposableEffect(map, code) {
        val current = map
        val area = Geohash.normalize(code)
        if (current != null && Geohash.isValid(area)) {
            val bounds = Geohash.bounds(area)
            val corners = arrayOf(arrayOf(bounds.south, bounds.west), arrayOf(bounds.north, bounds.east))
            cell?.remove()
            val style: dynamic = js("({ color: '#1D9BF0', weight: 2, fillOpacity: 0.15 })")
            cell = leaflet.rectangle(corners, style).addTo(current)
            val fit: dynamic = js("({ maxZoom: 15, padding: [24, 24] })")
            current.fitBounds(corners, fit)
        }
        onDispose { }
    }
}
