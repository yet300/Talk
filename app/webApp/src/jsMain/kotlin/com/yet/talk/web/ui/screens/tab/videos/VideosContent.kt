package com.yet.talk.web.ui.screens.tab.videos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.yet.talk.domain.model.video.VideoList
import com.yet.talk.feature.tab.videos.VideosComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.rememberIsPhone
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.paging.collectAsPagingItems
import com.yet.talk.web.ui.components.EmptyState
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.components.list.PagedList
import com.yet.talk.web.ui.components.video.VideoCard
import com.yet.talk.web.ui.screens.tab.videos.component.BrowseFilters
import com.yet.talk.web.ui.screens.tab.videos.component.ListChips
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

/**
 * Videos (`Videos-Web`, `Videos-Mobile`, `VideoHome`): the list chips; on Videos, For you then Browse (category
 * chips, Level and Type, the catalog by recommendation); on Liked, Watch later and History, that list.
 */
@Composable
fun VideosContent(component: VideosComponent) {
    val model by component.model.subscribeAsState()
    TopBar(stringResource(MR.strings.tab_videos))
    ListChips(model.list, component::onListSelected)
    when (val catalog = model.catalog) {
        VideosComponent.CatalogState.Loading -> P({ classes("t-caption", "t-video-note") }) { Text(stringResource(MR.strings.videos_loading)) }
        is VideosComponent.CatalogState.Failed -> ErrorRow(catalog.error, onDismiss = null, onRetry = component::onRetry)
        VideosComponent.CatalogState.Ready -> if (model.list == null) Home(model, component) else ListVideos(model, component)
    }
}

@Composable
private fun Home(model: VideosComponent.Model, component: VideosComponent) {
    val phone = rememberIsPhone()
    if (model.forYou.isNotEmpty()) {
        H2({ classes("t-section", "t-flush") }) { Text(stringResource(MR.strings.videos_for_you)) }
        // On the phone a row to swipe, so Browse and its filters are on the first screen.
        Div({ classes(if (phone) "t-video-row" else "t-video-grid", "t-video-grid-web") }) {
            model.forYou.forEach { VideoCard(it, component::onOpenVideo) }
        }
    }
    BrowseFilters(model, component, phone)
    val videos = component.browse.collectAsPagingItems()
    Div({ classes("t-video-grid", if (phone) "t-video-grid-phone" else "t-video-grid-web") }) {
        PagedList(videos, empty = { EmptyState(title = stringResource(MR.strings.videos_no_match)) }) { video ->
            VideoCard(video, component::onOpenVideo)
        }
    }
    P({ classes("t-caption", "t-video-note") }) { Text(stringResource(MR.strings.videos_credit)) }
}

@Composable
private fun ListVideos(model: VideosComponent.Model, component: VideosComponent) {
    if (model.listVideos.isEmpty()) {
        when (model.list) {
            VideoList.Liked -> EmptyState(title = stringResource(MR.strings.videos_empty_liked), icon = TalkIcons.Like)
            VideoList.WatchLater -> EmptyState(title = stringResource(MR.strings.videos_empty_later), icon = TalkIcons.Clock)
            VideoList.History, null -> EmptyState(title = stringResource(MR.strings.videos_empty_history), icon = TalkIcons.History)
        }
        return
    }
    val phone = rememberIsPhone()
    Div({ classes("t-video-grid", if (phone) "t-video-grid-phone" else "t-video-grid-web") }) {
        model.listVideos.forEach { VideoCard(it, component::onOpenVideo) }
    }
}
