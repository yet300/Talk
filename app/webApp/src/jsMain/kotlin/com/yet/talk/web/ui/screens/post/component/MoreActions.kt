package com.yet.talk.web.ui.screens.post.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.domain.model.post.Post
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.copyToClipboard
import com.yet.talk.web.ui.components.DropdownMenu
import com.yet.talk.web.ui.components.IconButton
import com.yet.talk.web.ui.components.LocalToaster
import com.yet.talk.web.ui.components.MenuItem
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div

/** More actions (decided: a dropdown on the web): Quote, Copy link; Edit and Delete on your own post. */
@Composable
fun MoreActions(post: Post, model: PostComponent.Model, component: PostComponent, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val quote = stringResource(MR.strings.action_quote)
    val copy = stringResource(MR.strings.post_copy_link)
    val edit = stringResource(MR.strings.action_edit)
    val delete = stringResource(MR.strings.action_delete)
    val toaster = LocalToaster.current
    val linkCopied = stringResource(MR.strings.toast_link_copied)
    Div({ classes("t-menu-host") }) {
        IconButton(TalkIcons.More, stringResource(MR.strings.action_more), onClick = { open = !open }, attrs = {
            attr("aria-haspopup", "menu")
            attr("aria-expanded", open.toString())
        })
        if (open) {
            DropdownMenu(
                items = buildList {
                    add(MenuItem(quote) { component.onQuote(post) })
                    add(MenuItem(copy) { copyToClipboard(model.shareLink); toaster.show(linkCopied) })
                    if (model.isMine) {
                        add(MenuItem(edit) { component.onEdit() })
                        add(MenuItem(delete, destructive = true, onClick = onDelete))
                    }
                },
                onDismiss = { open = false },
            )
        }
    }
}
