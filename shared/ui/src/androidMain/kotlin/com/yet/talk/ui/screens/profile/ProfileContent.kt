package com.yet.talk.ui.screens.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.yet.talk.ui.components.rememberCollapsingHeader
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.domain.model.moderation.ReportTarget
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.EmptyState
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.ReportSheet
import com.yet.talk.ui.components.glass.GlassIconButton
import com.yet.talk.ui.components.glass.GlassSegments
import com.yet.talk.ui.components.glass.GlassTopBar
import com.yet.talk.ui.components.glass.LocalGlassSource
import com.yet.talk.ui.components.glass.SwipePages
import com.yet.talk.ui.components.glass.navigationBarInset
import com.yet.talk.ui.components.glass.rememberPagesPosition
import com.yet.talk.ui.components.glass.topBarInset
import com.yet.talk.ui.components.list.PagedList
import com.yet.talk.ui.components.post.PostActionHandlers
import com.yet.talk.ui.components.post.PostCard
import com.yet.talk.ui.screens.profile.component.ProfileHeader
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.TalkIcons as Icons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * A profile (`Profile · Mobile`): the banner reaches the top edge under a clear bar with glass Back and More; once it
 * scrolls away the bar turns to glass and the name fades in. Posts · Replies (· Bookmarks on your own) are pages to
 * swipe (`ChildPages`); each page lists the header, the capsule (pinned under the bar) and its posts, with its own scroll.
 */
@Composable
fun ProfileContent(component: ProfileComponent, canGoBack: Boolean) {
    val model by component.model.subscribeAsState()
    val pages by component.pages.subscribeAsState()
    val tabs = pages.items.map { it.configuration as ProfileComponent.Tab }
    val position = rememberPagesPosition()
    val actions = remember(component) {
        PostActionHandlers(component::onReply, component::onRepost, component::onLike, component::onBookmark, component::onOpenRevisions)
    }
    // One list per tab: each keeps its scroll position while another shows.
    val lists = ProfileComponent.Tab.entries.associateWith { rememberLazyListState() }
    val current = lists.getValue(model.tab)
    val header = rememberCollapsingHeader { current }
    val pinnedHeight = topBarInset + TabsHeight
    val density = LocalDensity.current
    val glass = rememberHazeState()
    var reporting by rememberSaveable { mutableStateOf(false) }

    // A page shown while the header is collapsed starts just under the pinned tabs, not under a gap.
    val latestLists by rememberUpdatedState(lists)
    val latestCurrent by rememberUpdatedState(current)
    LaunchedEffect(header, position) {
        snapshotFlow { position.state?.targetPage }.collect {
            // The other lists line up with the header as it is, so it does not jump when their page settles.
            val collapsed = header.collapsed
            latestLists.values.filter { it !== latestCurrent }.forEach { list ->
                val scrolled = if (list.firstVisibleItemIndex == 0) list.firstVisibleItemScrollOffset else Int.MAX_VALUE
                if (scrolled != collapsed && (scrolled < collapsed || collapsed < header.range)) list.scrollToItem(0, collapsed)
            }
        }
    }

    CompositionLocalProvider(LocalGlassSource provides glass) {
        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.talkColors.bg)) {
            val viewport = maxHeight
            // Only the lists swipe; the header and the tabs above them stay where they are.
            SwipePages(
                component.pages,
                onPageSelected = { index -> tabs.getOrNull(index)?.let(component::onTabSelected) },
                position = position,
                modifier = Modifier.fillMaxSize().hazeSource(glass),
            ) { page ->
                val headerHeight = with(density) { header.height.toDp() }
                val list = lists.getValue(page.tab)
                // A short list still scrolls far enough for the header to collapse to its pinned part, so the
                // header never jumps when you switch to it.
                val room by remember(list, density) {
                    derivedStateOf {
                        val info = list.layoutInfo
                        if (info.totalItemsCount == 0 || info.visibleItemsInfo.size < info.totalItemsCount) {
                            0.dp
                        } else {
                            val content = with(density) { info.visibleItemsInfo.sumOf { it.size }.toDp() }
                            (viewport - pinnedHeight - content).coerceAtLeast(0.dp)
                        }
                    }
                }
                TabPage(
                    page = page,
                    component = component,
                    actions = actions,
                    list = list,
                    contentPadding = PaddingValues(top = headerHeight, bottom = navigationBarInset + MaterialTheme.spacing.xl + room),
                    refreshIndicatorOffset = headerHeight,
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { header.setSize(it.height, with(density) { pinnedHeight.roundToPx() }) }
                    .offset { IntOffset(0, header.offset.roundToInt()) }
                    .background(MaterialTheme.talkColors.bg)
                    .scrollable(header.scrollable, Orientation.Vertical),
            ) {
                ProfileHeader(model, component)
                Box(Modifier.fillMaxWidth().height(TabsHeight), contentAlignment = Alignment.Center) {
                    GlassSegments(
                        items = tabs,
                        selected = model.tab,
                        label = { stringResource(it.label) },
                        onSelect = component::onTabSelected,
                        position = position::value,
                    )
                }
                ErrorRow(model.error, onDismiss = component::onErrorShown)
            }
            val bannerPx = with(density) { MaterialTheme.sizes.bannerHeight.toPx() * 0.66f }
            val scrolledPast by remember(header) { derivedStateOf { -header.offset > bannerPx } }
            val titleAlpha = animateFloatAsState(if (scrolledPast) 1f else 0f, label = "title")
            GlassTopBar(
                glass = scrolledPast,
                leading = { if (canGoBack || model.canGoBack) GlassIconButton(TalkIcons.Back, stringResource(MR.strings.action_back), component::onBack) },
                title = {
                    Text(
                        model.user?.displayLabel.orEmpty(),
                        style = MaterialTheme.talkTypography.headingMedium,
                        color = MaterialTheme.talkColors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).graphicsLayer { alpha = titleAlpha.value },
                    )
                },
                trailing = {
                    when {
                        model.isMe -> GlassIconButton(Icons.Settings, stringResource(MR.strings.action_settings), component::onOpenSettings)
                        model.user != null -> GlassIconButton(Icons.More, stringResource(MR.strings.action_more), { reporting = true })
                    }
                },
            )
        }
    }
    val user = model.user
    if (reporting && user != null) ReportSheet(ReportTarget.Profile(user.id), onDismiss = { reporting = false })
}

@Composable
private fun TabPage(
    page: ProfileComponent.TabPage,
    component: ProfileComponent,
    actions: PostActionHandlers,
    list: LazyListState,
    contentPadding: PaddingValues,
    refreshIndicatorOffset: Dp,
) {
    val posts = page.posts.collectAsLazyPagingItems()
    PagedList(
        items = posts,
        key = { it.id.value },
        state = list,
        contentPadding = contentPadding,
        refreshIndicatorOffset = refreshIndicatorOffset,
        modifier = Modifier.background(MaterialTheme.talkColors.bg),
        empty = {
            when (page.tab) {
                ProfileComponent.Tab.Posts -> EmptyState(stringResource(MR.strings.profile_empty_title))
                ProfileComponent.Tab.Replies -> EmptyState(stringResource(MR.strings.profile_empty_replies_title))
                ProfileComponent.Tab.Bookmarks -> EmptyState(stringResource(MR.strings.bookmarks_empty_title), stringResource(MR.strings.bookmarks_empty_body), icon = Icons.Bookmark)
            }
        },
    ) { post ->
        PostCard(post, onOpen = { component.onOpenPost(it.id) }, onOpenAuthor = { component.onOpenAuthor(it.author.id) }, actions = actions)
    }
}

/** The height of the row holding the tabs capsule, which stays pinned under the bar. */
private val TabsHeight = 56.dp

private val ProfileComponent.Tab.label
    get() = when (this) {
        ProfileComponent.Tab.Posts -> MR.strings.profile_posts
        ProfileComponent.Tab.Replies -> MR.strings.profile_replies
        ProfileComponent.Tab.Bookmarks -> MR.strings.profile_bookmarks
    }
