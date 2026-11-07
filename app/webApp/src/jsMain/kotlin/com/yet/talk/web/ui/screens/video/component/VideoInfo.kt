package com.yet.talk.web.ui.screens.video.component

import androidx.compose.runtime.Composable
import com.yet.talk.feature.video.VideoComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.copyToClipboard
import com.yet.talk.web.ui.components.LocalToaster
import com.yet.talk.web.ui.components.video.label
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * The details (`VideoInfo`): title, `channel · N likes · N comments`, Like, Watch later, Share, Copy link, the
 * summary, why Hacker News recommends it, the category, type and level, and the HackerTube credit.
 */
@Composable
fun VideoInfo(model: VideoComponent.Model, component: VideoComponent) {
    val video = model.video
    Div({ classes("t-part", "t-video-info") }) {
        H1({ classes("t-video-title") }) { Text(video?.title ?: model.url) }
        Div({ classes("t-caption") }) {
            Text(stringResource(MR.strings.video_stats, video?.channel.orEmpty(), model.stats.likes.toString(), model.stats.comments.toString()))
        }
        Div({ classes("x-actions", "t-video-actions") }) {
            Action(TalkIcons.Like.takeUnless { model.viewer.liked } ?: TalkIcons.LikeFilled, stringResource(MR.strings.video_like), "x-like",
                "t-on-like".takeIf { model.viewer.liked }, component::onLike)
            Action(TalkIcons.Clock, stringResource(MR.strings.video_later), "x-bookmark", "t-on-blue-plain".takeIf { model.viewer.watchLater }, component::onWatchLater)
            Action(TalkIcons.Share, stringResource(MR.strings.video_share), "x-share", null, component::onShare)
            val toaster = LocalToaster.current
            val copied = stringResource(MR.strings.toast_link_copied)
            Action(TalkIcons.Link, stringResource(MR.strings.video_copy_link), "x-share", null) { copyToClipboard(model.url); toaster.show(copied) }
        }
        if (video == null) return@Div
        video.summary?.let { P({ classes("t-flush") }) { Text(it) } }
        if (video.whyHn != null) {
            Div({ classes("t-why") }) {
                H2({ classes("x-name", "t-flush") }) { Text(stringResource(MR.strings.video_why)) }
                P({ classes("t-flush") }) { Text(video.whyHn.orEmpty()) }
                video.quote?.let { P({ classes("t-muted", "t-flush") }) { Text("“$it”") } }
                if (video.recommenders > 0) Span({ classes("t-caption") }) { Text(stringResource(MR.strings.video_recommenders, video.recommenders.toString())) }
            }
        }
        val labels = video.categories + listOfNotNull(video.type?.label(), video.level?.label())
        if (labels.isNotEmpty()) Div({ classes("t-tags", "t-flush") }) { labels.forEach { Span { Text(it) } } }
        if (video.whyHn != null) P({ classes("t-caption", "t-flush") }) { Text(stringResource(MR.strings.videos_credit)) }
    }
}

@Composable
private fun Action(icon: dev.icerock.moko.resources.ImageResource, text: String, kind: String, activeClass: String?, onClick: () -> Unit) {
    Button({
        classes("x-action", kind)
        activeClass?.let { classes(it) }
        type(ButtonType.Button)
        onClick { onClick() }
    }) {
        Span({ classes("x-action-icon") }) { TalkIcon(icon, size = 18f) }
        Text(text)
    }
}
