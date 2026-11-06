package com.yet.talk.web.ui.components.post

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.domain.model.post.PostLink
import com.yet.talk.resources.*
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Iframe
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** The one link card of a post: a YouTube video, a GitHub card, or the plain link row. */
@Composable
fun LinkCard(link: PostLink) {
    when (link) {
        is PostLink.YouTube -> YouTubeCard(link)
        is PostLink.GitHub -> GitHubCardView(link.ref)
        is PostLink.Web -> LinkRow(link.url)
    }
}

/**
 * A YouTube link (`VideoCard`, embed): the thumbnail with Play, the title and channel from oEmbed. Play swaps
 * in YouTube's privacy-enhanced player, inline.
 */
@Composable
private fun YouTubeCard(link: PostLink.YouTube) {
    val cards = LocalPostCards.current
    val video by remember(link.videoId, cards) { cards?.youTube(link.videoId) ?: flowOf(null) }.collectAsState(null)
    var playing by remember(link.videoId) { mutableStateOf(false) }
    val title = video?.title ?: link.url
    Div({ classes("t-embed"); onClick { it.stopPropagation() } }) {
        if (playing) {
            Iframe({
                classes("t-player")
                attr("src", "https://www.youtube-nocookie.com/embed/${link.videoId}?autoplay=1")
                attr("title", title)
                attr("allow", "autoplay; encrypted-media; picture-in-picture; fullscreen")
                attr("allowfullscreen", "")
                attr("referrerpolicy", "strict-origin-when-cross-origin")
            })
        } else {
            val label = stringResource(MR.strings.video_play, title)
            Button({
                classes("t-thumb", "t-thumb-btn")
                type(ButtonType.Button)
                attr("aria-label", label)
                onClick { playing = true }
            }) {
                Img(src = link.thumbnail(), alt = "") { classes("t-thumb-img"); attr("loading", "lazy") }
                Span({ classes("t-play") }) { TalkIcon(TalkIcons.Play) }
            }
        }
        Div({ classes("t-embed-text") }) {
            Div({ classes("x-name", "t-clamp2") }) { Text(title) }
            video?.channel?.let { Div({ classes("t-caption") }) { Text(it) } }
        }
    }
}
