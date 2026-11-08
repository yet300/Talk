package com.yet.talk.ui.screens.tab.chats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.feature.tab.chats.ChatsComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.Caption
import com.yet.talk.ui.components.EmptyState
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.SkeletonCards
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.glass.BackBar
import com.yet.talk.ui.components.glass.GlassIconButton
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.components.glass.GlassSegments
import com.yet.talk.ui.components.glass.SwipePages
import com.yet.talk.ui.components.glass.rememberPagesPosition
import com.yet.talk.ui.navigation.AnimatedSlot
import com.yet.talk.ui.platform.SecureScreen
import com.yet.talk.ui.screens.channels.ChannelsContent
import com.yet.talk.ui.screens.chat.ChatInfoContent
import com.yet.talk.ui.screens.chat.ConversationContent
import com.yet.talk.ui.screens.chat.NewChatContent
import com.yet.talk.ui.screens.chat.VerifyContent
import com.yet.talk.ui.screens.tab.chats.component.ChannelsRow
import com.yet.talk.ui.screens.tab.chats.component.ChatRow
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors

/**
 * Chats (`Chats-Mobile`, `Chats-Inner`): the list with Chats · Requests as pages to swipe (`ChildPages`) and the
 * Location channels row; the open conversation, its info, Verify, New chat or Location channels in the [pane slot]
 * (`ChildSlot`): on a phone it slides over the list (and back with the predictive back gesture), wider it sits beside
 * it. Chats are kept out of screenshots (Settings, Security).
 */
@Composable
fun ChatsContent(component: ChatsComponent) {
    SecureScreen()
    val pane by component.pane.subscribeAsState()
    val open = pane.child?.instance
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 720.dp) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.width(360.dp).fillMaxHeight()) { ChatList(component, open, phone = false) }
                VerticalDivider(color = MaterialTheme.talkColors.border, thickness = MaterialTheme.sizes.hairline)
                AnimatedSlot(pane, component.backHandler, onBack = ::closePane, modifier = Modifier.weight(1f), slide = false, empty = { Picker() }) { Pane(it) }
            }
        } else {
            ChatList(component, open, phone = true)
            val roomOpen = (open as? ChatsComponent.Pane.Channels)?.component?.room?.subscribeAsState()?.value?.child != null
            AnimatedSlot(pane, component.backHandler, onBack = ::closePane, backEnabled = !roomOpen) { Pane(it) }
        }
    }
}

/** Back from a pane: each goes where its own Back goes (info back to the conversation, the rest to the list). */
private fun closePane(pane: ChatsComponent.Pane) = when (pane) {
    is ChatsComponent.Pane.Conversation -> pane.component.onBack()
    is ChatsComponent.Pane.NewChat -> pane.component.onBack()
    is ChatsComponent.Pane.Info -> pane.component.onBack()
    is ChatsComponent.Pane.Verify -> pane.component.onBack()
    is ChatsComponent.Pane.Channels -> pane.component.onBack()
}

@Composable
private fun Pane(pane: ChatsComponent.Pane) {
    when (pane) {
        is ChatsComponent.Pane.Conversation -> ConversationContent(pane.component)
        is ChatsComponent.Pane.NewChat -> NewChatContent(pane.component)
        is ChatsComponent.Pane.Info -> ChatInfoContent(pane.component)
        is ChatsComponent.Pane.Verify -> VerifyContent(pane.component)
        is ChatsComponent.Pane.Channels -> ChannelsContent(pane.component)
    }
}

@Composable
private fun Picker() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Caption(stringResource(MR.strings.chats_pick)) }
}

@Composable
private fun ChatList(component: ChatsComponent, open: ChatsComponent.Pane?, phone: Boolean) {
    val model by component.model.subscribeAsState()
    val position = rememberPagesPosition()
    val newChat = stringResource(MR.strings.chats_new)
    val selected: ChatId? = when (open) {
        is ChatsComponent.Pane.Conversation -> open.id
        is ChatsComponent.Pane.Info -> open.id
        is ChatsComponent.Pane.Verify -> open.id
        else -> null
    }
    val sizes = MaterialTheme.sizes
    GlassPage(bar = { BackBar(stringResource(MR.strings.tab_chats), onBack = null) { GlassIconButton(TalkIcons.Plus, newChat, component::onNewChat) } }) { padding ->
        val top = padding.calculateTopPadding() + SegmentsArea
        val bottom = padding.calculateBottomPadding() + (if (phone) sizes.capsuleHeight + sizes.capsuleOffset else 0.dp) + MaterialTheme.spacing.lg
        Box(Modifier.fillMaxSize()) {
            SwipePages(component.pages, onPageSelected = { component.onTabSelected(ChatsComponent.Tab.entries[it]) }, position = position) { page ->
                val chats by page.chats.subscribeAsState()
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = top, bottom = bottom)) {
                    item(key = "error") { ErrorRow(model.error, onDismiss = component::onErrorShown) }
                    if (page.tab == ChatsComponent.Tab.Chats) {
                        item(key = "channels") { ChannelsRow(selected = open is ChatsComponent.Pane.Channels, onClick = component::onOpenChannels) }
                    } else {
                        item(key = "note") { Caption(stringResource(MR.strings.chats_blocked_note)) }
                    }
                    when {
                        model.isLoading -> item(key = "loading") { SkeletonCards(3) }
                        chats.isEmpty() && page.tab == ChatsComponent.Tab.Chats -> item(key = "empty") {
                            EmptyState(
                                title = stringResource(MR.strings.chats_empty_title),
                                body = stringResource(MR.strings.chats_empty_body),
                                icon = TalkIcons.Chat,
                                action = { TalkButton(newChat, component::onNewChat, ButtonStyle.Primary) },
                            )
                        }
                        chats.isEmpty() -> item(key = "empty") {
                            EmptyState(stringResource(MR.strings.chats_requests_empty_title), stringResource(MR.strings.chats_requests_empty_body))
                        }
                    }
                    items(chats, key = { it.id.value }, contentType = { "chat" }) { chat ->
                        ChatRow(chat, selected = chat.id == selected, onClick = { component.onOpenChat(chat.id) }, modifier = Modifier.animateItem())
                    }
                }
            }
            val requests = if (model.requestCount > 0) stringResource(MR.strings.chats_tab_requests_count, model.requestCount) else stringResource(MR.strings.chats_tab_requests)
            val chatsLabel = stringResource(MR.strings.chats_tab_chats)
            GlassSegments(
                items = ChatsComponent.Tab.entries,
                selected = model.tab,
                label = { if (it == ChatsComponent.Tab.Chats) chatsLabel else requests },
                onSelect = component::onTabSelected,
                position = position::value,
                segmentWidth = 120.dp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = padding.calculateTopPadding() + 8.dp),
            )
        }
    }
}

private val SegmentsArea = 64.dp
