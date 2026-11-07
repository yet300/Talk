package com.yet.talk.web.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.yet.talk.domain.repository.ChatRepository
import com.yet.talk.feature.chat.component.newchat.NewChatComponent
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.components.AvatarSize
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.SearchField
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.TextField
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement

/**
 * New chat (`NewChat`): search people and pick one, or several for a group (up to 10, with an optional name);
 * someone with no inbox relays shows "Can't receive chats yet" and can't be picked.
 */
@Composable
fun NewChatContent(component: NewChatComponent) {
    val model by component.model.subscribeAsState()
    TopBar(title = stringResource(MR.strings.chats_new), onBack = component::onBack) {
        TalkButton(stringResource(MR.strings.chat_start), component::onStart, ButtonStyle.Primary, attrs = {
            classes("t-header-action")
            if (!model.canStart) disabled()
        })
    }
    if (model.picked.isNotEmpty()) {
        Div({ classes("t-chips") }) {
            model.picked.forEach { user ->
                val remove = stringResource(MR.strings.chat_remove_pick, user.displayLabel)
                Button({
                    classes("t-chip")
                    type(ButtonType.Button)
                    attr("aria-label", remove)
                    onClick { component.onPick(user) }
                }) {
                    Avatar(user, AvatarSize.Small)
                    Span { Text(user.displayLabel) }
                    TalkIcon(TalkIcons.Close, size = 14f)
                }
            }
        }
    }
    if (model.picked.size > 1) {
        Div({ classes("t-new-chat-search") }) {
            TextField(
                value = model.groupName,
                onValueChange = component::onGroupNameChanged,
                label = stringResource(MR.strings.chat_group_name),
                placeholder = "",
                id = "group-name",
            )
        }
    }
    Div({ classes("t-new-chat-search") }) {
        SearchField(model.query, component::onQueryChanged, stringResource(MR.strings.chat_new_search), id = "new-chat-search")
    }
    LaunchedEffect(Unit) { (document.getElementById("new-chat-search") as? HTMLElement)?.focus() }
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
    if (model.isFull) P({ classes("t-caption", "t-search-hint") }) { Text(stringResource(MR.strings.chat_group_full, ChatRepository.MAX_GROUP_MEMBERS)) }
    if (model.isSearching) P({ classes("t-caption", "t-search-hint") }) { Text(stringResource(MR.strings.search_searching)) }
    model.people.forEach { user ->
        val cannot = user.id in model.cannotReceive
        val picked = model.picked.any { it.id == user.id }
        Button({
            classes("t-row", "t-row-button")
            if (picked) classes("t-selected")
            type(ButtonType.Button)
            attr("aria-pressed", picked.toString())
            if (cannot || model.checking != null || (model.isFull && !picked)) disabled()
            if (model.checking == user.id) attr("aria-busy", "true")
            onClick { component.onPick(user) }
        }) {
            Avatar(user)
            Span({ classes("t-row-text") }) {
                Span({ classes("x-name", "t-block") }) { Text(user.displayLabel) }
                Span({ classes("t-caption", "t-block") }) {
                    Text(if (cannot) stringResource(MR.strings.chat_new_cannot_receive) else user.handle)
                }
            }
            if (picked) Span({ classes("t-on-blue-plain") }) { TalkIcon(TalkIcons.Check) }
        }
    }
}
