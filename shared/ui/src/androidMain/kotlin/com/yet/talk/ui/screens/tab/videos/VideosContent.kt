package com.yet.talk.ui.screens.tab.videos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.domain.model.video.VideoList
import com.yet.talk.feature.tab.videos.VideosComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.Caption
import com.yet.talk.ui.components.EmptyState
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.SectionTitle
import com.yet.talk.ui.components.SkeletonCards
import com.yet.talk.ui.components.glass.BackBar
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.components.list.PagedList
import com.yet.talk.ui.components.video.VideoCard
import com.yet.talk.ui.screens.tab.videos.component.BrowseFilters
import com.yet.talk.ui.screens.tab.videos.component.ListChips
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.TalkMotion
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource

/**
 * Videos (`Videos-Mobile`, `VideoHome`): the list chips; on Videos, For you as a row to swipe, then Browse (category
 * chips, Level and Type, the catalog 20 at a time); on Liked, Watch later and History, that list. "Getting the
 * catalog ready…" while the catalog loads into the database (decision 16).
 */
@Composable
fun VideosContent(component: VideosComponent) {
    val model by component.model.subscribeAsState()
    val sizes = MaterialTheme.sizes
    GlassPage(bar = { BackBar(stringResource(MR.strings.tab_videos), onBack = null) }) { padding ->
        val bottom = padding.calculateBottomPadding() + sizes.capsuleHeight + sizes.capsuleOffset + MaterialTheme.spacing.lg
        val content = PaddingValues(top = padding.calculateTopPadding(), bottom = bottom)
        val shape = when (model.catalog) {
            VideosComponent.CatalogState.Loading -> VideosShape.Loading
            is VideosComponent.CatalogState.Failed -> VideosShape.Failed
            VideosComponent.CatalogState.Ready -> if (model.list == null) VideosShape.Home else VideosShape.List
        }
        AnimatedContent(shape, transitionSpec = { fadeIn(TalkMotion.fade()) togetherWith fadeOut(TalkMotion.fade()) }, label = "videos") { state ->
            when (state) {
                VideosShape.Loading -> Column(Modifier.padding(content)) {
                    ListChips(model.list, component::onListSelected)
                    Caption(stringResource(MR.strings.videos_loading))
                    SkeletonCards(3)
                }
                VideosShape.Failed -> Column(Modifier.padding(content)) {
                    ListChips(model.list, component::onListSelected)
                    ErrorRow((model.catalog as? VideosComponent.CatalogState.Failed)?.error, onDismiss = null, onRetry = component::onRetry)
                }
                VideosShape.Home -> Home(model, component, content)
                VideosShape.List -> ListVideos(model, component, content)
            }
        }
    }
}

@Composable
private fun Home(model: VideosComponent.Model, component: VideosComponent, padding: PaddingValues) {
    val videos = component.browse.collectAsLazyPagingItems()
    PagedList(
        items = videos,
        key = { it.id },
        contentPadding = padding,
        header = {
            item(key = "lists") { ListChips(model.list, component::onListSelected) }
            if (model.forYou.isNotEmpty()) {
                item(key = "foryou-title") { SectionTitle(stringResource(MR.strings.videos_for_you)) }
                item(key = "foryou") {
                    LazyRow(contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.sm), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                        items(model.forYou, key = { it.id }) { VideoCard(it, component::onOpenVideo, Modifier.width(260.dp), small = true) }
                    }
                }
            }
            item(key = "filters") { Column(Modifier.padding(top = MaterialTheme.spacing.md)) { BrowseFilters(model, component) } }
        },
        empty = { EmptyState(stringResource(MR.strings.videos_no_match)) },
    ) { video ->
        VideoCard(video, component::onOpenVideo, Modifier.padding(horizontal = MaterialTheme.spacing.sm))
    }
}

@Composable
private fun ListVideos(model: VideosComponent.Model, component: VideosComponent, padding: PaddingValues) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item(key = "lists") { ListChips(model.list, component::onListSelected) }
        if (model.listVideos.isEmpty()) {
            item(key = "empty") {
                when (model.list) {
                    VideoList.Liked -> EmptyState(stringResource(MR.strings.videos_empty_liked), icon = TalkIcons.Like)
                    VideoList.WatchLater -> EmptyState(stringResource(MR.strings.videos_empty_later), icon = TalkIcons.Clock)
                    VideoList.History, null -> EmptyState(stringResource(MR.strings.videos_empty_history), icon = TalkIcons.History)
                }
            }
        }
        items(model.listVideos, key = { it.id }) { VideoCard(it, component::onOpenVideo, Modifier.padding(horizontal = MaterialTheme.spacing.sm).animateItem()) }
    }
}

/** What the Videos tab shows: the catalog loading or failed, For you with Browse, or one of your lists. */
private enum class VideosShape { Loading, Failed, Home, List }
