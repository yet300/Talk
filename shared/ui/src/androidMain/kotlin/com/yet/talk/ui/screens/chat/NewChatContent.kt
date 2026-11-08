package com.yet.talk.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.domain.repository.ChatRepository
import com.yet.talk.feature.chat.component.newchat.NewChatComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.Avatar
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.Caption
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.ListRow
import com.yet.talk.ui.components.SearchField
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.TalkTextField
import com.yet.talk.ui.components.glass.BackBar
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors

/**
 * New chat (`NewChat`): search people and pick one, or several for a group (up to 10, a name from two); someone with
 * no inbox relays shows "Can't receive chats yet" and can't be picked.
 */
@Composable
fun NewChatContent(component: NewChatComponent) {
    val model by component.model.subscribeAsState()
    val search = remember { FocusRequester() }
    LaunchedEffect(Unit) { search.requestFocus() }
    GlassPage(
        modifier = Modifier.imePadding(),
        bar = {
            BackBar(stringResource(MR.strings.chats_new), onBack = component::onBack) {
                TalkButton(stringResource(MR.strings.chat_start), component::onStart, ButtonStyle.Primary, enabled = model.canStart)
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            if (model.picked.isNotEmpty()) {
                item(key = "picked") {
                    LazyRow(contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.lg), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                        items(model.picked, key = { it.id.value }) { user ->
                            InputChip(
                                selected = true,
                                onClick = { component.onPick(user) },
                                label = { Text(user.displayLabel) },
                                avatar = { Avatar(user, size = 24.dp) },
                                trailingIcon = { TalkIcon(TalkIcons.Close, stringResource(MR.strings.chat_remove_pick, user.displayLabel), size = 14.dp) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
            if (model.picked.size > 1) {
                item(key = "group") {
                    TalkTextField(model.groupName, component::onGroupNameChanged, label = stringResource(MR.strings.chat_group_name), modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm))
                }
            }
            item(key = "search") {
                SearchField(
                    model.query, component::onQueryChanged, stringResource(MR.strings.chat_new_search),
                    clearLabel = stringResource(MR.strings.action_clear),
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm).focusRequester(search),
                )
            }
            item(key = "error") { ErrorRow(model.error, onDismiss = component::onErrorShown) }
            if (model.isFull) item(key = "full") { Caption(stringResource(MR.strings.chat_group_full, ChatRepository.MAX_GROUP_MEMBERS)) }
            if (model.isSearching) item(key = "searching") { Caption(stringResource(MR.strings.search_searching)) }
            items(model.people, key = { it.id.value }) { user ->
                val cannot = user.id in model.cannotReceive
                val picked = model.picked.any { it.id == user.id }
                val enabled = !cannot && model.checking == null && (!model.isFull || picked)
                ListRow(
                    title = user.displayLabel,
                    caption = if (cannot) stringResource(MR.strings.chat_new_cannot_receive) else user.handle,
                    selected = picked,
                    onClick = if (enabled) ({ component.onPick(user) }) else null,
                    leading = { Avatar(user) },
                    modifier = Modifier.animateItem(),
                    trailing = {
                        when {
                            model.checking == user.id -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.talkColors.accent)
                            picked -> TalkIcon(TalkIcons.Check, null, tint = MaterialTheme.talkColors.accent)
                        }
                    },
                )
            }
        }
    }
}
