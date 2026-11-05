package com.yet.talk.web.ui.screens.tab.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.yet.talk.feature.tab.home.HomeComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.rememberIsPhone
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.paging.collectAsPagingItems
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.components.list.PagedList
import com.yet.talk.web.ui.components.post.PostActionHandlers
import com.yet.talk.web.ui.components.post.PostCard
import com.yet.talk.web.ui.screens.tab.home.component.FeedEmpty
import com.yet.talk.web.ui.screens.tab.home.component.FeedTabs
import com.yet.talk.web.ui.theme.stringResource

/** Home (`Timeline · Web`): "Home" bar, Following · Popular · Latest, the paged cards. */
@Composable
fun HomeContent(component: HomeComponent) {
    val model by component.model.subscribeAsState()
    val posts = component.posts.collectAsPagingItems()
    val actions = PostActionHandlers(component::onReply, component::onRepost, component::onLike, component::onBookmark, component::onOpenRevisions)

    if (!rememberIsPhone()) TopBar(stringResource(MR.strings.tab_home))
    FeedTabs(model.feed, component::onFeedSelected)
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
    PagedList(posts, empty = { FeedEmpty(model.feed, onBrowseLatest = { component.onFeedSelected(HomeComponent.Feed.Latest) }) }) { post ->
        PostCard(
            post = post,
            onOpen = { component.onOpenPost(it.id) },
            onOpenAuthor = { component.onOpenAuthor(it.author.id) },
            actions = actions,
        )
    }
}
