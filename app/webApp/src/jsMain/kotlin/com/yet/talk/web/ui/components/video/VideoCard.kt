package com.yet.talk.web.ui.components.video

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.video.Video
import com.yet.talk.web.appUrl
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text

/** A catalog video (`VideoCard`): thumbnail, title, `channel · category · level`, why HN recommends it. Opens the video page. */
@Composable
fun VideoCard(video: Video, onOpen: (String) -> Unit) {
    A(href = appUrl("/video/${video.id}"), attrs = {
        classes("t-video-card")
        onClick { it.preventDefault(); onOpen(video.id) }
    }) {
        Div({ classes("t-thumb") }) { Img(src = video.thumbnail(), alt = "") { classes("t-thumb-img"); attr("loading", "lazy") } }
        Div({ classes("x-name", "t-clamp2") }) { Text(video.title) }
        Div({ classes("t-caption") }) { Text(listOfNotNull(video.channel, video.categories.firstOrNull() ?: video.primaryTopic, video.level?.label()).joinToString(" · ")) }
        video.whyHn?.let { Div({ classes("t-caption", "t-clamp2") }) { Text(it) } }
    }
}

/** "intro" → "Intro": the catalog's levels and types, as the design writes them. */
fun String.label(): String = replaceFirstChar { it.uppercase() }
