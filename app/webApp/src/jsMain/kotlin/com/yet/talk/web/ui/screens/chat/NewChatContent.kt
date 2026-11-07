package com.yet.talk.web.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.yet.talk.feature.chat.component.newchat.NewChatComponent
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.SearchField
import com.yet.talk.web.ui.components.TopBar
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

/** New chat (`NewChat`): search people and pick one; someone with no inbox relays can't be picked. */
@Composable
fun NewChatContent(component: NewChatComponent) {
    val model by component.model.subscribeAsState()
    TopBar(title = stringResource(MR.strings.chats_new), onBack = component::onBack)
    Div({ classes("t-new-chat-search") }) {
        SearchField(model.query, component::onQueryChanged, stringResource(MR.strings.chat_new_search), id = "new-chat-search")
    }
    LaunchedEffect(Unit) { (document.getElementById("new-chat-search") as? HTMLElement)?.focus() }
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
    if (model.isSearching) P({ classes("t-caption", "t-search-hint") }) { Text(stringResource(MR.strings.search_searching)) }
    model.people.forEach { user ->
        val blocked = user.id in model.cannotReceive
        Button({
            classes("t-row", "t-row-button")
            type(ButtonType.Button)
            if (blocked || model.checking != null) disabled()
            if (model.checking == user.id) attr("aria-busy", "true")
            onClick { component.onPick(user) }
        }) {
            Avatar(user)
            Span({ classes("t-row-text") }) {
                Span({ classes("x-name", "t-block") }) { Text(user.displayLabel) }
                Span({ classes("t-caption", "t-block") }) {
                    Text(if (blocked) stringResource(MR.strings.chat_new_cannot_receive) else user.handle)
                }
            }
        }
    }
}
