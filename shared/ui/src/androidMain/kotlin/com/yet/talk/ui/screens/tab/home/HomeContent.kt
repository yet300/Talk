package com.yet.talk.ui.screens.tab.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.tab.home.HomeComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.glass.GlassSegments
import com.yet.talk.ui.components.list.EmptyState
import com.yet.talk.ui.components.list.PagedList
import com.yet.talk.ui.components.post.PostActionHandlers
import com.yet.talk.ui.components.post.PostCard
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.message
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkShapes
import com.yet.talk.ui.theme.talkTypography

/**
 * Home (`Timeline · Mobile`): the feed edge to edge under the glass header and the floating Following · Popular ·
 * Latest capsule; the compose button above the tab capsule. The header itself is drawn by the main screen.
 */
@Composable
fun HomeContent(component: HomeComponent) {
    val model by component.model.subscribeAsState()
    val posts = component.posts.collectAsLazyPagingItems()
    val actions = PostActionHandlers(component::onReply, component::onRepost, component::onLike, component::onBookmark, component::onOpenRevisions)
    val sizes = MaterialTheme.sizes
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + sizes.barHeight + CapsuleArea
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + sizes.capsuleHeight + sizes.capsuleOffset + MaterialTheme.spacing.lg
    Box(Modifier.fillMaxSize()) {
        PagedList(
            items = posts,
            key = { it.id.value },
            contentPadding = PaddingValues(top = top, bottom = bottom),
            header = {
                model.error?.let { error ->
                    item { Text(error.message(), style = MaterialTheme.talkTypography.body, color = MaterialTheme.talkColors.error, modifier = Modifier.padding(MaterialTheme.spacing.lg)) }
                }
            },
            empty = { FeedEmpty(model.feed) { component.onFeedSelected(HomeComponent.Feed.Latest) } },
        ) { post ->
            PostCard(post, onOpen = { component.onOpenPost(it.id) }, onOpenAuthor = { component.onOpenAuthor(it.author.id) }, actions = actions)
        }
        GlassSegments(
            items = HomeComponent.Feed.entries,
            selected = model.feed,
            label = { stringResource(it.label) },
            onSelect = component::onFeedSelected,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = sizes.barHeight + 8.dp),
        )
        val compose = stringResource(MR.strings.action_compose)
        FloatingActionButton(
            onClick = component::onCompose,
            containerColor = MaterialTheme.talkColors.accent,
            contentColor = MaterialTheme.talkColors.onAccent,
            shape = MaterialTheme.talkShapes.pill,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = MaterialTheme.spacing.lg, bottom = sizes.capsuleHeight + sizes.capsuleOffset)
                .semantics { contentDescription = compose },
        ) { TalkIcon(TalkIcons.Compose, contentDescription = null) }
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
