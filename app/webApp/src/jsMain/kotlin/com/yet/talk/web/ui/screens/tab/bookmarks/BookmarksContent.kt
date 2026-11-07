package com.yet.talk.web.ui.screens.tab.bookmarks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.yet.talk.feature.tab.bookmarks.BookmarksComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.rememberIsPhone
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.paging.collectAsPagingItems
import com.yet.talk.web.ui.components.EmptyState
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.components.list.PagedList
import com.yet.talk.web.ui.components.post.PostActionHandlers
import com.yet.talk.web.ui.components.post.PostCard
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource

/** Bookmarks (`Bookmarks · Web`); removing one is the card's bookmark action. */
@Composable
fun BookmarksContent(component: BookmarksComponent) {
    val model by component.model.subscribeAsState()
    val posts = component.posts.collectAsPagingItems()
    val actions = PostActionHandlers(component::onReply, component::onRepost, component::onLike, component::onBookmark, component::onOpenRevisions)

    // On the phone Bookmarks is not in the navigation (it is a tab of your profile): Back returns home.
    TopBar(stringResource(MR.strings.bookmarks_title), onBack = component::onBrowseHome.takeIf { rememberIsPhone() })
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
    PagedList(
        posts,
        empty = {
            EmptyState(
                icon = TalkIcons.Bookmark,
                title = stringResource(MR.strings.bookmarks_empty_title),
                body = stringResource(MR.strings.bookmarks_empty_body),
                action = { TalkButton(stringResource(MR.strings.action_back_home), component::onBrowseHome) },
            )
        },
    ) { post ->
        PostCard(post, onOpen = { component.onOpenPost(it.id) }, onOpenAuthor = { component.onOpenAuthor(it.author.id) }, actions = actions)
    }
}
