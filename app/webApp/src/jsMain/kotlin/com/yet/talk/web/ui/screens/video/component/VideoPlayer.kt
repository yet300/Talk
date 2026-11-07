package com.yet.talk.web.ui.screens.video.component

import androidx.compose.runtime.Composable
import com.yet.talk.feature.video.VideoComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Iframe
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span

/** The player (`VideoPlayer`): the thumbnail with Play, then YouTube's privacy-enhanced embedded player. */
@Composable
fun VideoPlayer(model: VideoComponent.Model, onPlay: () -> Unit) {
    if (model.isPlaying) {
        Iframe({
            classes("t-player")
            attr("src", "https://www.youtube-nocookie.com/embed/${model.id}?autoplay=1")
            attr("title", model.video?.title ?: model.url)
            attr("allow", "autoplay; encrypted-media; picture-in-picture; fullscreen")
            attr("allowfullscreen", "")
            attr("referrerpolicy", "strict-origin-when-cross-origin")
        })
        return
    }
    val label = stringResource(MR.strings.video_play_video)
    Button({
        classes("t-player", "t-thumb-btn", "t-player-start")
        type(ButtonType.Button)
        attr("aria-label", label)
        onClick { onPlay() }
    }) {
        Img(src = "https://i.ytimg.com/vi/${model.id}/hqdefault.jpg", alt = "") { classes("t-thumb-img") }
        Span({ classes("t-play") }) { TalkIcon(TalkIcons.Play) }
    }
}
