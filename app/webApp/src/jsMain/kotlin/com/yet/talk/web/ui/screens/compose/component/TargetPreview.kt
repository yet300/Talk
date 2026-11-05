package com.yet.talk.web.ui.screens.compose.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.Post
import com.yet.talk.feature.compose.ComposeMode
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.IconButton
import com.yet.talk.web.ui.components.post.PostHead
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** What the editor works on: "Reply" / "Quote post" / "Edit post" and that post's head and title. */
@Composable
fun TargetPreview(mode: ComposeMode, post: Post) {
    val heading = when (mode) {
        is ComposeMode.Reply -> MR.strings.compose_reply
        is ComposeMode.Quote -> MR.strings.compose_quote
        is ComposeMode.Edit -> MR.strings.compose_edit
        ComposeMode.New -> return
    }
    Div({ classes("t-target") }) {
        Span({ classes("t-caption") }) { Text(stringResource(heading)) }
        if (mode !is ComposeMode.Edit) {
            Div({ classes("x-quote") }) {
                PostHead(post)
                Div({ classes("t-card-title") }) { Text(post.summary.title) }
            }
        }
    }
}

/** Attached images (also in the text as markdown); each can be removed. */
@Composable
fun ImageStrip(images: List<Image>, onRemove: (String) -> Unit) {
    val label = stringResource(MR.strings.compose_images)
    val removeLabel = stringResource(MR.strings.compose_remove_image)
    Div({ classes("t-images"); attr("aria-label", label) }) {
        images.forEach { image ->
            Div({ classes("t-image") }) {
                Img(src = image.url, alt = image.alt.orEmpty())
                IconButton(TalkIcons.Close, removeLabel, onClick = { onRemove(image.url) }, size = 18f)
            }
        }
    }
}
