package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import com.yet.talk.resources.TalkTokens
import com.yet.talk.web.ui.theme.TalkIcon
import dev.icerock.moko.resources.ImageResource
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLButtonElement

/** The design's buttons: primary (blue fill), secondary (outline), post (the large rail button). */
enum class ButtonStyle(internal val className: String) { Primary("x-btn-primary"), Secondary("x-btn-secondary"), Post("x-btn-post") }

@Composable
fun TalkButton(
    text: String,
    onClick: () -> Unit,
    style: ButtonStyle = ButtonStyle.Primary,
    enabled: Boolean = true,
    attrs: (AttrsScope<HTMLButtonElement>.() -> Unit)? = null,
) {
    Button({
        classes("x-btn", style.className)
        type(ButtonType.Button)
        if (!enabled) disabled()
        onClick { it.stopPropagation(); onClick() }
        attrs?.invoke(this)
    }) { Text(text) }
}

/** A 44 px round icon button (`t-icon-btn`); [label] is its accessible name. */
@Composable
fun IconButton(
    icon: ImageResource,
    label: String,
    onClick: () -> Unit,
    size: Float = TalkTokens.Size.iconNav,
    attrs: (AttrsScope<HTMLButtonElement>.() -> Unit)? = null,
) {
    Button({
        classes("t-icon-btn")
        type(ButtonType.Button)
        attr("aria-label", label)
        onClick { it.stopPropagation(); onClick() }
        attrs?.invoke(this)
    }) { TalkIcon(icon, size = size) }
}
