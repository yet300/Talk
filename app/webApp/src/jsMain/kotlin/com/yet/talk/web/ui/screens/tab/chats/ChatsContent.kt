package com.yet.talk.web.ui.screens.tab.chats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.yet.talk.feature.tab.chats.ChatsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.rememberIsPhone
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.EmptyState
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.IconButton
import com.yet.talk.web.ui.components.Tabs
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.screens.chat.ConversationContent
import com.yet.talk.web.ui.screens.chat.NewChatContent
import com.yet.talk.web.ui.screens.tab.chats.component.ChatRow
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

/**
 * Chats (`Chats-Web`, `Chats-Mobile`): wider than a phone, the list (`t-chat-col`) and the open conversation or
 * New chat (`t-chat-main`) side by side, the one exception to the web's single pane (decision 19). On a phone
 * the list, or the pane over it with no navigation.
 */
@Composable
fun ChatsContent(component: ChatsComponent) {
    val pane by component.pane.subscribeAsState()
    val open = pane.child?.instance
    if (rememberIsPhone()) {
        if (open != null) Pane(open) else ChatList(component, open)
        return
    }
    Div({ classes("t-part", "t-chat-col") }) { ChatList(component, open) }
    Div({ classes("t-chat-main") }) {
        if (open != null) {
            Pane(open)
        } else {
            P({ classes("t-muted", "t-chat-pick") }) { Text(stringResource(MR.strings.chats_pick)) }
        }
    }
}

@Composable
private fun Pane(pane: ChatsComponent.Pane) {
    when (pane) {
        is ChatsComponent.Pane.Conversation -> ConversationContent(pane.component)
        is ChatsComponent.Pane.NewChat -> NewChatContent(pane.component)
    }
}

@Composable
private fun ChatList(component: ChatsComponent, open: ChatsComponent.Pane?) {
    val model by component.model.subscribeAsState()
    val newChat = stringResource(MR.strings.chats_new)
    TopBar(title = stringResource(MR.strings.tab_chats)) {
        IconButton(TalkIcons.Plus, newChat, component::onNewChat)
    }
    val requestsLabel = if (model.requestCount > 0) stringResource(MR.strings.chats_tab_requests_count, model.requestCount)
    else stringResource(MR.strings.chats_tab_requests)
    Tabs(
        items = ChatsComponent.Tab.entries,
        selected = model.tab,
        label = { if (it == ChatsComponent.Tab.Chats) stringResource(MR.strings.chats_tab_chats) else requestsLabel },
        onSelect = component::onTabSelected,
        ariaLabel = stringResource(MR.strings.tab_chats),
    )
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
    if (!model.isLoading && model.chats.isEmpty()) {
        if (model.tab == ChatsComponent.Tab.Chats) {
            EmptyState(
                title = stringResource(MR.strings.chats_empty_title),
                body = stringResource(MR.strings.chats_empty_body),
                icon = TalkIcons.Chat,
                action = { TalkButton(newChat, component::onNewChat, ButtonStyle.Primary) },
            )
        } else {
            EmptyState(title = stringResource(MR.strings.chats_requests_empty_title), body = stringResource(MR.strings.chats_requests_empty_body))
        }
    }
    val selected = (open as? ChatsComponent.Pane.Conversation)?.id
    model.chats.forEach { chat -> ChatRow(chat, selected = chat.id == selected, onClick = { component.onOpenChat(chat.id) }) }
}
