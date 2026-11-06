package com.yet.talk.web.ui.components.post

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.theme.stringResource
import com.yet.talk.web.ui.theme.text
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Article
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * The design's post card (`PostCard`): avatar, name, `handle · time`, the title and excerpt derived from the
 * markdown, `N min read #tags` (not when [dense]) and the action row. The whole card opens the post.
 */
@Composable
fun PostCard(
    post: Post,
    onOpen: (Post) -> Unit,
    onOpenAuthor: (Post) -> Unit,
    actions: PostActionHandlers,
    selected: Boolean = false,
    dense: Boolean = false,
) {
    val summary = post.summary
    Article({
        classes("t-part", "x-post")
        if (selected) classes("t-selected")
        if (dense) classes("t-dense")
        onClick { onOpen(post) }
    }) {
        Avatar(post.author, onClick = { onOpenAuthor(post) })
        Div({ classes("x-post-body") }) {
            PostHead(post, onEdited = { actions.onOpenRevisions(post) })
            H2({ classes("t-card-title") }) {
                A(href = "/post/${post.id.value}", attrs = { onClick { it.preventDefault(); it.stopPropagation(); onOpen(post) } }) {
                    Text(summary.title)
                }
            }
            if (summary.excerpt.isNotBlank()) P({ classes("t-excerpt") }) { Text(summary.excerpt) }
            if (!dense) {
                Div({ classes("t-tags") }) {
                    Span { Text(stringResource(MR.strings.post_read_time, summary.readMinutes)) }
                    summary.tags.forEach { tag -> Span { Text("#$tag") } }
                }
            }
            summary.github?.let { GitHubCardView(it) }
            PostActions(post, actions)
        }
    }
}

/**
 * `Name handle · time`, "· Edited" for an edited post (a link to its revisions when [onEdited] is set), and
 * Sending… / Not sent while the post is not on relays yet.
 */
@Composable
fun PostHead(post: Post, onEdited: (() -> Unit)? = null) {
    Div({ classes("x-post-head") }) {
        Span({ classes("x-name") }) { Text(post.author.displayLabel) }
        Span({ classes("x-handle") }) { Text("${post.author.handle} · ${post.createdAt.toRelativeDesc().text()}") }
        if (post.editedAt != null) EditedMark(onEdited)
        when (post.publishState) {
            PublishState.Pending -> Span({ classes("t-caption") }) { Text(stringResource(MR.strings.post_pending)) }
            PublishState.Failed -> Span({ classes("x-alert", "t-caption") }) { Text(stringResource(MR.strings.post_failed)) }
            PublishState.Published -> Unit
        }
    }
}

/** "· Edited": opens the post's revisions where it can. */
@Composable
fun EditedMark(onEdited: (() -> Unit)?) {
    val label = stringResource(MR.strings.post_edited)
    Span({ classes("x-handle") }) { Text("·") }
    if (onEdited == null) {
        Span({ classes("x-handle") }) { Text(label) }
    } else {
        Button({
            classes("t-text-btn", "t-edited")
            type(ButtonType.Button)
            onClick { it.stopPropagation(); onEdited() }
        }) { Text(label) }
    }
}
