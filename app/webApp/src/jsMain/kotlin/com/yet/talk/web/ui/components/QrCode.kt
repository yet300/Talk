package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.svg.Rect
import org.jetbrains.compose.web.svg.Svg

@JsModule("qrcode-generator")
@JsNonModule
private external val qrcodeModule: dynamic

/** Which modules of a QR code are dark, row by row (error correction M). */
private fun qrModules(payload: String): List<List<Boolean>> {
    val factory = qrcodeModule.default ?: qrcodeModule
    val qr = factory(0, "M")
    qr.addData(payload)
    qr.make()
    val count = qr.getModuleCount() as Int
    return List(count) { row -> List(count) { col -> qr.isDark(row, col) as Boolean } }
}

/** A QR code (`t-qr`): black modules on white with a quiet zone, whatever the theme, so cameras read it. */
@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun QrCode(payload: String, label: String) {
    val modules = remember(payload) { qrModules(payload) }
    val size = modules.size + 8
    Svg(viewBox = "0 0 $size $size", attrs = {
        classes("t-qr-svg")
        attr("role", "img")
        attr("aria-label", label)
        attr("shape-rendering", "crispEdges")
    }) {
        Rect(0, 0, size, size, attrs = { attr("fill", "#FFFFFF") })
        modules.forEachIndexed { row, cells ->
            cells.forEachIndexed { col, dark ->
                if (dark) Rect(col + 4, row + 4, 1, 1, attrs = { attr("fill", "#000000") })
            }
        }
    }
}
