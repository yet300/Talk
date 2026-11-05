package com.yet.talk.web.ui.screens.post.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.post.ThreadReply
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.markdown.Markdown
import com.yet.talk.web.ui.components.post.ActionButton
import com.yet.talk.web.ui.components.post.PostHead
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.pluralResource
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Article
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Text

/** The replies (`Replies`): "N replies", the inline reply field, and the reply cards. */
@Composable
fun Replies(model: PostComponent.Model, component: PostComponent, idSuffix: String) {
    val fieldId = "reply-field-$idSuffix"
    val sectionLabel = stringResource(MR.strings.profile_replies)
    val placeholderText = stringResource(MR.strings.post_write_reply)
    Section({ classes("t-part"); attr("aria-label", sectionLabel) }) {
        H2({ classes("t-section", "t-flush") }) { Text(pluralResource(MR.plurals.replies_count, model.replies.size)) }
        Div({ classes("t-reply-box") }) {
            Label(forId = fieldId, attrs = { classes("t-sr") }) { Text(stringResource(MR.strings.post_write_reply)) }
            Input(InputType.Text) {
                id(fieldId)
                classes("x-input", "t-reply-input")
                placeholder(placeholderText)
                value(model.replyText)
                onInput { component.onReplyTextChanged(it.value) }
                onKeyDown { if (it.key == "Enter" && model.canSendReply) component.onSendReply() }
            }
            TalkButton(stringResource(MR.strings.action_reply), component::onSendReply, enabled = model.canSendReply)
        }
        model.replies.forEach { reply -> ReplyCard(reply, component) }
    }
}

@Composable
private fun ReplyCard(reply: ThreadReply, component: PostComponent) {
    val post = reply.post
    Article({ classes("x-post"); onClick { component.onOpenPost(post.id) } }) {
        Avatar(post.author, onClick = { component.onOpenAuthor(post.author.id) })
        Div({ classes("x-post-body") }) {
            PostHead(post)
            Markdown(post.content, "t-reply-body")
            Div({ classes("x-actions", "t-reply-actions") }) {
                ActionButton("x-reply", TalkIcons.Reply, post.stats.replies, stringResource(MR.strings.a11y_replies, post.stats.replies.toInt())) {
                    component.onReplyInEditor(post)
                }
                ActionButton(
                    "x-like", if (post.viewer.liked) TalkIcons.LikeFilled else TalkIcons.Like, post.stats.likes,
                    stringResource(MR.strings.a11y_likes, post.stats.likes.toInt()),
                    activeClass = "t-on-like".takeIf { post.viewer.liked },
                ) { component.onLike(post) }
            }
        }
    }
}
