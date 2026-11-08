package com.yet.talk.ui.screens.tab.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.tab.search.SearchComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.Caption
import com.yet.talk.ui.components.EmptyState
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.PersonRow
import com.yet.talk.ui.components.SearchField
import com.yet.talk.ui.components.SectionTitle
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.components.glass.GlassTopBar
import com.yet.talk.ui.components.post.PostActionHandlers
import com.yet.talk.ui.components.post.PostCard
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource

/**
 * Search (`Search · Mobile`): the field in the glass bar, then People and Posts; the idle hint, Searching… and "No
 * results" in between (design pass 2's states). Back first leaves the field (the keyboard), then the tab.
 */
@Composable
fun SearchContent(component: SearchComponent) {
    val model by component.model.subscribeAsState()
    val actions = remember(component) {
        PostActionHandlers(component::onReply, component::onRepost, component::onLike, component::onBookmark, component::onOpenRevisions)
    }
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    BackHandler(enabled = focused) { focusManager.clearFocus() }
    val sizes = MaterialTheme.sizes
    GlassPage(
        modifier = Modifier.imePadding(),
        bar = {
            GlassTopBar(title = {
                SearchField(
                    value = model.query,
                    onValueChange = component::onQueryChanged,
                    placeholder = stringResource(MR.strings.search_placeholder),
                    clearLabel = stringResource(MR.strings.action_clear),
                    modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                    onSearch = focusManager::clearFocus,
                )
            })
        },
    ) { padding ->
        val bottom = padding.calculateBottomPadding() + sizes.capsuleHeight + sizes.capsuleOffset + MaterialTheme.spacing.lg
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = padding.calculateTopPadding() + MaterialTheme.spacing.sm, bottom = bottom)) {
            item(key = "error") { ErrorRow(model.error, onDismiss = component::onErrorShown) }
            when {
                model.isIdle -> item(key = "idle") { Caption(stringResource(MR.strings.search_idle), Modifier.animateItem()) }
                model.isSearching && model.users.isEmpty() && model.posts.isEmpty() ->
                    item(key = "searching") { Caption(stringResource(MR.strings.search_searching), Modifier.animateItem()) }
                model.users.isEmpty() && model.posts.isEmpty() ->
                    item(key = "empty") { EmptyState(stringResource(MR.strings.search_empty, model.query.trim()), icon = TalkIcons.Search, modifier = Modifier.animateItem()) }
            }
            if (model.users.isNotEmpty()) {
                item(key = "people", contentType = "title") { SectionTitle(stringResource(MR.strings.search_people), Modifier.animateItem()) }
                items(model.users, key = { "u" + it.id.value }, contentType = { "person" }) { user ->
                    PersonRow(user, onClick = { component.onOpenProfile(user.id) }, modifier = Modifier.animateItem())
                }
            }
            if (model.posts.isNotEmpty()) {
                item(key = "posts", contentType = "title") { SectionTitle(stringResource(MR.strings.search_posts), Modifier.animateItem()) }
                items(model.posts, key = { "p" + it.id.value }, contentType = { "post" }) { post ->
                    Box(Modifier.animateItem()) {
                        PostCard(post, onOpen = { component.onOpenPost(it.id) }, onOpenAuthor = { component.onOpenProfile(it.author.id) }, actions = actions)
                    }
                }
            }
        }
    }
}
