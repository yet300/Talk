package com.yet.talk.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import com.yet.talk.ui.components.glass.LocalGlassSource
import com.yet.talk.ui.components.glass.navigationBarInset
import com.yet.talk.ui.theme.talkColors
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.video.VideoComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.EmptyState
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.SectionTitle
import com.yet.talk.ui.components.glass.BackBar
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.components.video.VideoCard
import com.yet.talk.ui.screens.video.component.CommentCard
import com.yet.talk.ui.screens.video.component.CommentField
import com.yet.talk.ui.screens.video.component.VideoInfo
import com.yet.talk.ui.screens.video.component.VideoPlayer
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource

/**
 * The video page (`VideoPage-Mobile`): Back · Video on glass, the player under it, the details, the comments, and
 * Related under them. A related video opens at the top.
 */
@Composable
fun VideoContent(component: VideoComponent) {
    val model by component.model.subscribeAsState()
    val list = rememberLazyListState()
    LaunchedEffect(model.id) { list.scrollToItem(0) }
    // The player is a WebView whose video draws on its own surface, which a blurred (glass) layer would hide: the
    // page keeps a solid bar and the player fixed under it, as YouTube does, with the details scrolling below.
    val glass = rememberHazeState()
    CompositionLocalProvider(LocalGlassSource provides glass) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.talkColors.bg).imePadding()) {
            BackBar(stringResource(MR.strings.video_title), onBack = component::onBack, glass = false)
            VideoPlayer(model, onPlay = component::onPlay)
            LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = navigationBarInset + MaterialTheme.spacing.xl)) {
                item(key = "error") { ErrorRow(model.error, onDismiss = component::onErrorShown) }
                item(key = "info") { VideoInfo(model, component) }
                item(key = "comments-title") { SectionTitle(stringResource(MR.strings.video_comments)) }
                item(key = "comment-field") { CommentField(model, component) }
                if (model.comments.isEmpty()) item(key = "no-comments") { EmptyState(stringResource(MR.strings.video_comments_empty)) }
                items(model.comments, key = { it.id }, contentType = { "comment" }) { comment ->
                    CommentCard(comment, onOpenAuthor = { component.onOpenAuthor(comment.author.id) }, modifier = Modifier.animateItem())
                }
                if (model.related.isNotEmpty()) {
                    item(key = "related-title") { SectionTitle(stringResource(MR.strings.video_related)) }
                    items(model.related, key = { "r" + it.id }) { VideoCard(it, component::onOpenVideo, Modifier.padding(horizontal = MaterialTheme.spacing.sm)) }
                }
            }
        }
    }
}
