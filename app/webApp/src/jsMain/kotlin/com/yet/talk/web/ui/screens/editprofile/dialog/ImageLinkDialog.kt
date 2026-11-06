package com.yet.talk.web.ui.screens.editprofile.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.domain.rule.ProfileImage
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.TextField
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

/**
 * Asks for a GIPHY or Unsplash link for the avatar or banner (decision 13 of the UI review): the two sites open
 * in a new tab, the pasted link is checked when Use is pressed.
 */
@Composable
fun ImageLinkDialog(kind: ProfileImage, onSubmit: (String) -> Unit, onDismiss: () -> Unit) {
    var link by remember { mutableStateOf("") }
    Div({ classes("x-scrim", "t-layer"); onClick { onDismiss() } }) {
        Div({
            classes("x-modal", "t-dialog")
            attr("role", "dialog")
            attr("aria-modal", "true")
            onClick { it.stopPropagation() }
        }) {
            val title = stringResource(if (kind == ProfileImage.Avatar) MR.strings.edit_profile_avatar_link else MR.strings.edit_profile_banner_link)
            H2({ classes("t-h1", "t-flush") }) { Text(title) }
            P({ classes("t-muted", "t-flush") }) { Text(stringResource(MR.strings.edit_profile_link_body)) }
            Div({ classes("t-row-actions") }) {
                Site("https://giphy.com", stringResource(MR.strings.edit_profile_open_giphy))
                Site("https://unsplash.com", stringResource(MR.strings.edit_profile_open_unsplash))
            }
            TextField(
                value = link,
                onValueChange = { link = it },
                label = stringResource(MR.strings.edit_profile_link_label),
                placeholder = stringResource(MR.strings.edit_profile_link_placeholder),
                id = "profile-image-link",
                onEnter = { if (link.isNotBlank()) onSubmit(link) },
            )
            TalkButton(stringResource(MR.strings.action_use), { onSubmit(link) }, enabled = link.isNotBlank(), attrs = { classes("t-btn-44") })
            TalkButton(stringResource(MR.strings.action_cancel), onDismiss, ButtonStyle.Secondary, attrs = { classes("t-btn-44") })
        }
    }
}

@Composable
private fun Site(url: String, label: String) {
    A(href = url, attrs = {
        classes("x-link")
        attr("target", "_blank")
        attr("rel", "noopener noreferrer")
    }) { Text(label) }
}
