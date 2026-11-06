package com.yet.talk.web.ui.components.post

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.domain.model.post.Image
import com.yet.talk.resources.*
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/** Up to four images (`MediaGrid`): one, two side by side, three with a tall first, four in a square. A tile opens the viewer. */
@Composable
fun MediaGrid(images: List<Image>) {
    var open by remember { mutableStateOf<Int?>(null) }
    val shown = images.take(4)
    Div({ classes("t-grid", "t-grid-${shown.size}") }) {
        shown.forEachIndexed { index, image ->
            val label = stringResource(MR.strings.media_open, index + 1, shown.size)
            Button({
                classes("t-tile")
                type(ButtonType.Button)
                attr("aria-label", label)
                onClick { it.stopPropagation(); open = index }
            }) {
                Img(src = image.url, alt = image.alt.orEmpty()) { classes("t-tile-img"); attr("loading", "lazy") }
                if (image.isGif) Span({ classes("t-badge") }) { Text(stringResource(MR.strings.media_gif)) }
            }
        }
    }
    open?.let { start -> ImageViewer(shown, start, onClose = { open = null }) }
}

/** The image viewer (`ImageViewer`): the image on the media scrim, Close, Previous and Next (arrow keys too), `2 of 3`. */
@Composable
fun ImageViewer(images: List<Image>, start: Int, onClose: () -> Unit) {
    var index by remember { mutableStateOf(start) }
    val count = images.size
    DisposableEffect(count) {
        val listener: (Event) -> Unit = { event ->
            when ((event as KeyboardEvent).key) {
                "Escape" -> onClose()
                "ArrowLeft" -> index = (index + count - 1) % count
                "ArrowRight" -> index = (index + 1) % count
            }
        }
        document.addEventListener("keydown", listener)
        onDispose { document.removeEventListener("keydown", listener) }
    }
    val image = images[index]
    val viewerLabel = stringResource(MR.strings.media_viewer)
    Div({
        classes("t-layer", "t-scrim-media", "t-viewer")
        attr("role", "dialog")
        attr("aria-modal", "true")
        attr("aria-label", viewerLabel)
        onClick { it.stopPropagation(); onClose() }
    }) {
        Img(src = image.url, alt = image.alt.orEmpty()) { classes("t-viewer-full"); onClick { it.stopPropagation() } }
        ViewerButton(TalkIcons.Close, stringResource(MR.strings.action_close), "t-viewer-close", onClick = onClose)
        if (count > 1) {
            ViewerButton(TalkIcons.Back, stringResource(MR.strings.media_previous), "t-viewer-prev") { index = (index + count - 1) % count }
            ViewerButton(TalkIcons.Back, stringResource(MR.strings.media_next), "t-viewer-next", flip = true) { index = (index + 1) % count }
            Span({ classes("t-viewer-count") }) { Text(stringResource(MR.strings.media_position, index + 1, count)) }
        }
    }
}

@Composable
private fun ViewerButton(icon: dev.icerock.moko.resources.ImageResource, label: String, place: String, flip: Boolean = false, onClick: () -> Unit) {
    Button({
        classes("t-viewer-btn", place)
        if (flip) classes("t-flip")
        type(ButtonType.Button)
        attr("aria-label", label)
        onClick { it.stopPropagation(); onClick() }
    }) { TalkIcon(icon) }
}

private val Image.isGif: Boolean get() = mimeType == "image/gif" || url.substringBefore('?').endsWith(".gif", ignoreCase = true)
