package com.yet.talk.ui.screens.tab.bookmarks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.tab.bookmarks.BookmarksComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.EmptyState
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.glass.BackBar
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.components.list.PagedList
import com.yet.talk.ui.components.post.PostActionHandlers
import com.yet.talk.ui.components.post.PostCard
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.stringResource

/**
 * Bookmarks (`Bookmarks-Mobile`): saved posts, removed with the card's bookmark; Back returns Home (decision 17).
 * On a phone it is your profile's third tab, so this page shows only on the Duo (its side column) or from a link.
 */
@Composable
fun BookmarksContent(component: BookmarksComponent, canGoBack: Boolean) {
    val model by component.model.subscribeAsState()
    val posts = component.posts.collectAsLazyPagingItems()
    val actions = remember(component) {
        PostActionHandlers(component::onReply, component::onRepost, component::onLike, component::onBookmark, component::onOpenRevisions)
    }
    GlassPage(bar = { BackBar(stringResource(MR.strings.bookmarks_title), onBack = component::onBrowseHome.takeIf { canGoBack }) }) { padding ->
        PagedList(
            items = posts,
            key = { it.id.value },
            contentPadding = padding,
            header = { item(key = "error") { ErrorRow(model.error, onDismiss = component::onErrorShown, modifier = Modifier) } },
            empty = {
                EmptyState(
                    title = stringResource(MR.strings.bookmarks_empty_title),
                    body = stringResource(MR.strings.bookmarks_empty_body),
                    icon = TalkIcons.Bookmark,
                    action = { TalkButton(stringResource(MR.strings.action_back_home), component::onBrowseHome, ButtonStyle.Primary) },
                )
            },
        ) { post ->
            PostCard(post, onOpen = { component.onOpenPost(it.id) }, onOpenAuthor = { component.onOpenAuthor(it.author.id) }, actions = actions)
        }
    }
}
