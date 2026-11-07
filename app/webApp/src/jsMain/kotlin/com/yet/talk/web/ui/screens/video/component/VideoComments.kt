package com.yet.talk.web.ui.screens.video.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.video.VideoComment
import com.yet.talk.feature.video.VideoComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.components.EmptyState
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.markdown.Markdown
import com.yet.talk.web.ui.theme.stringResource
import com.yet.talk.web.ui.theme.text
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Article
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** Comments (`Replies` with "Add a comment"): the field and Reply, then the comments (NIP-22), oldest first. */
@Composable
fun VideoComments(model: VideoComponent.Model, component: VideoComponent) {
    val title = stringResource(MR.strings.video_comments)
    val placeholderText = stringResource(MR.strings.video_comment_placeholder)
    Section({ classes("t-part", "t-video-comments"); attr("aria-label", title) }) {
        H2({ classes("t-section", "t-flush") }) { Text(title) }
        Div({ classes("t-reply-box") }) {
            Label(forId = "video-comment", attrs = { classes("t-sr") }) { Text(placeholderText) }
            Input(InputType.Text) {
                id("video-comment")
                classes("x-input", "t-reply-input")
                placeholder(placeholderText)
                value(model.comment)
                onInput { component.onCommentChanged(it.value) }
                onKeyDown { if (it.key == "Enter" && model.canSend) component.onSendComment() }
            }
            TalkButton(stringResource(MR.strings.action_reply), component::onSendComment, enabled = model.canSend)
        }
        if (model.comments.isEmpty()) {
            EmptyState(title = stringResource(MR.strings.video_comments_empty))
        } else {
            model.comments.forEach { CommentCard(it, onOpenAuthor = { component.onOpenAuthor(it.author.id) }) }
        }
    }
}

@Composable
private fun CommentCard(comment: VideoComment, onOpenAuthor: () -> Unit) {
    Article({ classes("x-post") }) {
        Avatar(comment.author, onClick = onOpenAuthor)
        Div({ classes("x-post-body") }) {
            Div({ classes("x-post-head") }) {
                Span({ classes("x-name") }) { Text(comment.author.displayLabel) }
                Span({ classes("x-handle") }) { Text("${comment.author.handle} · ${comment.createdAt.toRelativeDesc().text()}") }
                when (comment.publishState) {
                    PublishState.Pending -> Span({ classes("t-caption") }) { Text(stringResource(MR.strings.post_pending)) }
                    PublishState.Failed -> Span({ classes("x-alert", "t-caption") }) { Text(stringResource(MR.strings.post_failed)) }
                    PublishState.Published -> Unit
                }
            }
            Markdown(comment.content, "t-reply-body")
        }
    }
}
