package com.yet.talk.ui.screens.tab.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.tab.home.HomeComponent
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.Caption
import com.yet.talk.ui.components.EmptyState
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.glass.GlassSegments
import com.yet.talk.ui.components.glass.SwipePages
import com.yet.talk.ui.components.glass.navigationBarInset
import com.yet.talk.ui.components.glass.rememberPagesPosition
import com.yet.talk.ui.components.glass.topBarInset
import com.yet.talk.ui.components.list.PagedList
import com.yet.talk.ui.components.post.PostActionHandlers
import com.yet.talk.ui.components.post.PostCard
import com.yet.talk.ui.screens.main.component.HomeHeader
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkShapes

/**
 * Home (`Timeline · Mobile`): Following, Popular and Latest as pages to swipe (`ChildPages`), each edge to edge
 * under the glass header and the floating capsule whose pill follows the swipe; each feed keeps its own scroll.
 * Compose floats above the tab capsule on a [phone].
 */
@Composable
fun HomeContent(component: HomeComponent, me: TabsComponent.Me, onProfile: () -> Unit, onSettings: () -> Unit, phone: Boolean) {
    val model by component.model.subscribeAsState()
    val position = rememberPagesPosition()
    val actions = remember(component) {
        PostActionHandlers(component::onReply, component::onRepost, component::onLike, component::onBookmark, component::onOpenRevisions)
    }
    val sizes = MaterialTheme.sizes
    val bottom = navigationBarInset + (if (phone) sizes.capsuleHeight + sizes.capsuleOffset else 0.dp) + MaterialTheme.spacing.lg
    val padding = PaddingValues(top = topBarInset + CapsuleArea, bottom = bottom)
    Box(Modifier.fillMaxSize()) {
        SwipePages(component.pages, onPageSelected = { component.onFeedSelected(HomeComponent.Feed.entries[it]) }, position = position) { page ->
            FeedPage(page, component, actions, padding)
        }
        HomeHeader(me, onProfile = onProfile, onSettings = onSettings)
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = topBarInset + 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GlassSegments(
                items = HomeComponent.Feed.entries,
                selected = model.feed,
                label = { stringResource(it.label) },
                onSelect = component::onFeedSelected,
                position = position::value,
            )
            ErrorRow(model.error, onDismiss = component::onErrorShown)
        }
        val compose = stringResource(MR.strings.action_compose)
        AnimatedVisibility(
            visible = phone,
            modifier = Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = MaterialTheme.spacing.lg, bottom = sizes.capsuleHeight + sizes.capsuleOffset),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            FloatingActionButton(
                onClick = component::onCompose,
                containerColor = MaterialTheme.talkColors.accent,
                contentColor = MaterialTheme.talkColors.onAccent,
                shape = MaterialTheme.talkShapes.pill,
                modifier = Modifier.semantics { contentDescription = compose },
            ) { TalkIcon(TalkIcons.Compose, contentDescription = null) }
        }
    }
}

@Composable
private fun FeedPage(page: HomeComponent.FeedPage, component: HomeComponent, actions: PostActionHandlers, padding: PaddingValues) {
    val posts = page.posts.collectAsLazyPagingItems()
    PagedList(
        items = posts,
        key = { it.id.value },
        contentPadding = padding,
        state = rememberLazyListState(),
        contentType = { if (it.summary.isArticle) "article" else "text" },
        header = {
            if (page.feed == HomeComponent.Feed.Popular) item(key = "caption", contentType = "caption") { Caption(stringResource(MR.strings.feed_popular_caption)) }
        },
        empty = { FeedEmpty(page.feed) { component.onFeedSelected(HomeComponent.Feed.Latest) } },
    ) { post ->
        PostCard(post, onOpen = { component.onOpenPost(it.id) }, onOpenAuthor = { component.onOpenAuthor(it.author.id) }, actions = actions)
    }
}

@Composable
private fun FeedEmpty(feed: HomeComponent.Feed, onBrowseLatest: () -> Unit) {
    if (feed == HomeComponent.Feed.Following) {
        EmptyState(
            title = stringResource(MR.strings.feed_following_empty_title),
            body = stringResource(MR.strings.feed_following_empty_body),
            action = { TalkButton(stringResource(MR.strings.action_browse_latest), onBrowseLatest, ButtonStyle.Primary) },
        )
    } else {
        EmptyState(title = stringResource(MR.strings.feed_empty_title), body = stringResource(MR.strings.feed_empty_body))
    }
}

private val HomeComponent.Feed.label
    get() = when (this) {
        HomeComponent.Feed.Following -> MR.strings.feed_following
        HomeComponent.Feed.Popular -> MR.strings.feed_popular
        HomeComponent.Feed.Latest -> MR.strings.feed_latest
    }

/** The floating feed capsule and its margins under the header. */
private val CapsuleArea = 68.dp
