package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

/**
 * The design's underline tabs (`x-tabs`): feeds, profile Posts/Replies, Settings theme. [segmented] is the
 * compact variant of the editor bar (`t-seg`).
 */
@Composable
fun <T> Tabs(
    items: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    ariaLabel: String,
    segmented: Boolean = false,
    capsule: Boolean = false,
) {
    Div({
        classes(
            when {
                capsule -> "t-seg-capsule"
                segmented -> "t-seg"
                else -> "x-tabs"
            },
        )
        attr("role", "tablist")
        attr("aria-label", ariaLabel)
    }) {
        items.forEach { item ->
            val active = item == selected
            Button({
                classes(if (capsule) "t-seg-item" else "x-tab")
                if (active) classes("is-active")
                type(ButtonType.Button)
                attr("role", "tab")
                attr("aria-selected", active.toString())
                onClick { onSelect(item) }
            }) { Text(label(item)) }
        }
    }
}
