package com.yet.talk.web.ui.screens.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.browser.window
import androidx.compose.runtime.getValue
import com.yet.talk.feature.video.VideoComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.rememberIsPhone
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.components.video.VideoCard
import com.yet.talk.web.ui.screens.video.component.VideoComments
import com.yet.talk.web.ui.screens.video.component.VideoInfo
import com.yet.talk.web.ui.screens.video.component.VideoPlayer
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Text

/**
 * The video page (`VideoPage-Web`, `VideoPage-Mobile`): Back · Video, the player, the details, the comments;
 * Related in the right column on the web, under the comments on the phone.
 */
@Composable
fun VideoContent(component: VideoComponent) {
    val model by component.model.subscribeAsState()
    // Opened from a scrolled list (or a related video): start at the player.
    LaunchedEffect(model.id) { window.scrollTo(0.0, 0.0) }
    TopBar(stringResource(MR.strings.video_title), onBack = component::onBack)
    VideoPlayer(model, onPlay = component::onPlay)
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
    VideoInfo(model, component)
    VideoComments(model, component)
    if (rememberIsPhone() && model.related.isNotEmpty()) {
        H2({ classes("t-section", "t-flush") }) { Text(stringResource(MR.strings.video_related)) }
        Div({ classes("t-video-grid", "t-video-grid-phone") }) { model.related.forEach { VideoCard(it, component::onOpenVideo) } }
    }
}

/** The web's right column: Related. */
@Composable
fun VideoSide(component: VideoComponent) {
    val model by component.model.subscribeAsState()
    if (model.related.isEmpty()) return
    H2({ classes("t-h1", "t-flush") }) { Text(stringResource(MR.strings.video_related)) }
    model.related.forEach { VideoCard(it, component::onOpenVideo) }
}
