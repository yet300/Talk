package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import com.yet.talk.resources.*
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

/** One entry of a [DropdownMenu]; [destructive] items are red (Delete). */
data class MenuItem(val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/**
 * The design's dropdown (`x-menu`), anchored under its button by the caller's `t-menu-anchor`; a click
 * outside or on an item closes it. Used for More actions on the web.
 */
@Composable
fun DropdownMenu(items: List<MenuItem>, onDismiss: () -> Unit) {
    Div({ classes("t-menu-backdrop"); onClick { it.stopPropagation(); onDismiss() } })
    Div({ classes("x-menu", "t-menu"); attr("role", "menu") }) {
        items.forEach { item ->
            Button({
                classes("x-menu-item", "t-menu-item")
                if (item.destructive) classes("t-danger")
                type(ButtonType.Button)
                attr("role", "menuitem")
                onClick { it.stopPropagation(); onDismiss(); item.onClick() }
            }) { Text(item.label) }
        }
    }
}

/** A confirmation (`x-scrim` + `x-modal`), e.g. before deleting a post. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    Div({ classes("x-scrim", "t-dialog-scrim"); onClick { onDismiss() } }) {
        Div({
            classes("x-modal", "t-dialog")
            attr("role", "alertdialog")
            attr("aria-modal", "true")
            onClick { it.stopPropagation() }
        }) {
            H2({ classes("t-h1", "t-flush") }) { Text(title) }
            P({ classes("t-muted") }) { Text(body) }
            Div({ classes("t-dialog-actions") }) {
                Button({
                    classes("x-btn", if (destructive) "t-btn-danger" else "x-btn-primary")
                    type(ButtonType.Button)
                    onClick { onConfirm() }
                }) { Text(confirm) }
                TalkButton(stringResource(MR.strings.action_cancel), onDismiss, ButtonStyle.Secondary)
            }
        }
    }
}
