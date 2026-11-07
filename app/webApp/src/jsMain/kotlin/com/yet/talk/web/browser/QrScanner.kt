package com.yet.talk.web.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Video
import org.w3c.dom.HTMLVideoElement

/** The browser can read QR codes from the camera (the `BarcodeDetector` API: Chrome on Android, Edge, Chrome OS). */
fun isQrScanSupported(): Boolean = js("typeof BarcodeDetector !== 'undefined' && !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia)") as Boolean

/**
 * The camera, reading QR codes until one is found ([onResult]) or it leaves the composition; the stream is
 * stopped then. [onError] when the camera can't be opened (denied, none).
 */
@Composable
fun QrScanner(onResult: (String) -> Unit, onError: () -> Unit) {
    Video({
        classes("t-scan-video")
        attr("playsinline", "")
        attr("muted", "")
        attr("autoplay", "")
        ref { video ->
            val stop = startScanning(video, onResult, onError)
            onDispose { stop() }
        }
    })
}

private fun startScanning(video: HTMLVideoElement, onResult: (String) -> Unit, onError: () -> Unit): () -> Unit {
    var stopped = false
    var stream: dynamic = null
    var timer = 0
    val detector: dynamic = js("new BarcodeDetector({ formats: ['qr_code'] })")
    val constraints: dynamic = js("({ video: { facingMode: 'environment' }, audio: false })")
    window.navigator.asDynamic().mediaDevices.getUserMedia(constraints).then({ media: dynamic ->
        if (stopped) {
            media.getTracks().forEach { track: dynamic -> track.stop() }
        } else {
            stream = media
            video.asDynamic().srcObject = media
            video.play()
            lateinit var tick: () -> Unit
            tick = {
                if (!stopped) {
                    detector.detect(video).then({ codes: dynamic ->
                        val value = if (codes.length > 0) codes[0].rawValue as String else null
                        if (value != null && !stopped) onResult(value) else timer = window.setTimeout(tick, 300)
                    }, { _: dynamic -> timer = window.setTimeout(tick, 300) })
                }
            }
            tick()
        }
    }, { _: dynamic -> if (!stopped) onError() })
    return {
        stopped = true
        window.clearTimeout(timer)
        stream?.getTracks()?.forEach { track: dynamic -> track.stop() }
    }
}
