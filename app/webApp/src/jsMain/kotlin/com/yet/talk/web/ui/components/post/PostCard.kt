package com.yet.talk.web.ui.components.post

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import kotlinx.coroutines.flow.flowOf
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
 * The design's post card (`PostCard`): the repost header ("Name reposted") in Following, avatar, name,
 * `handle · time`, "Replying to" for replies; an article card (title, excerpt, `N min read #tags`, not when
 * [dense]) for a post starting with `# heading`, else a text card (6 lines, Show more); its images, its one
 * link card, the quoted post; then the action row, or Sending… / Not sent with Retry. The card opens the post.
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
        classes("t-part", "x-post", "t-post-wrap")
        if (selected) classes("t-selected")
        if (dense) classes("t-dense")
        onClick { onOpen(post) }
    }) {
        post.repostedBy?.let { reposter ->
            Div({ classes("t-post-context") }) {
                TalkIcon(TalkIcons.Repost, size = 18f)
                Text(stringResource(MR.strings.post_reposted, reposter.displayLabel))
            }
        }
        Avatar(post.author, onClick = { onOpenAuthor(post) })
        Div({ classes("x-post-body") }) {
            PostHead(post, onEdited = { actions.onOpenRevisions(post) }, showSendState = false)
            post.replyTo?.let { parent ->
                Div({ classes("t-reply-ctx") }) { Text(stringResource(MR.strings.post_replying_to, parent.handle)) }
            }
            if (summary.isArticle) {
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
            } else {
                TextBody(summary.text)
            }
            if (post.images.isNotEmpty()) MediaGrid(post.images)
            summary.link?.let { LinkCard(it) }
            post.quoteOf?.let { QuotedCard(it, onOpen) }
            when (post.publishState) {
                PublishState.Pending -> Div({ classes("t-send-row"); attr("role", "status") }) { Text(stringResource(MR.strings.post_pending)) }
                PublishState.Failed -> FailedRow(post)
                PublishState.Published -> PostActions(post, actions)
            }
        }
    }
}

/** A text card's body: up to six lines, then Show more expands it in place. */
@Composable
private fun TextBody(text: String) {
    var expanded by remember(text) { mutableStateOf(false) }
    val long = text.length > LONG_TEXT || text.count { it == '\n' } >= TEXT_LINES
    P({
        classes("x-post-text", "t-flush", "t-pre-line")
        if (long && !expanded) classes("t-clamp6")
    }) { Text(text) }
    if (long && !expanded) {
        Button({
            classes("t-text-btn")
            type(ButtonType.Button)
            onClick { it.stopPropagation(); expanded = true }
        }) { Text(stringResource(MR.strings.post_show_more)) }
    }
}

/** A quoted post (`x-quote`): its head, three lines of text, and an image placeholder when it has images. */
@Composable
private fun QuotedCard(id: PostId, onOpen: (Post) -> Unit) {
    val cards = LocalPostCards.current
    val quoted by remember(id, cards) { cards?.quote(id) ?: flowOf(null) }.collectAsState(null)
    val post = quoted ?: return
    Div({ classes("x-quote", "t-quote"); onClick { it.stopPropagation(); onOpen(post) } }) {
        PostHead(post, showSendState = false)
        Div({ classes("t-clamp2", "t-clamp3") }) { Text(if (post.summary.isArticle) post.summary.title else post.summary.text) }
        if (post.images.isNotEmpty()) Div({ classes("t-quote-img") }) { TalkIcon(TalkIcons.Image) }
    }
}

/** Not sent, with Retry: publishes the stored event again. */
@Composable
private fun FailedRow(post: Post) {
    val cards = LocalPostCards.current
    Div({ classes("t-send-row"); attr("role", "alert") }) {
        Span({ classes("t-send-fail") }) { Text(stringResource(MR.strings.post_failed)) }
        TalkButton(stringResource(MR.strings.action_retry), { cards?.retry(post) }, ButtonStyle.Secondary, attrs = {
            onClick { it.stopPropagation() }
        })
    }
}

private const val TEXT_LINES = 6
private const val LONG_TEXT = 420

/**
 * `Name handle · time`, "· Edited" for an edited post (a link to its revisions when [onEdited] is set), and
 * Sending… / Not sent while the post is not on relays yet (cards show those in place of the actions instead).
 */
@Composable
fun PostHead(post: Post, onEdited: (() -> Unit)? = null, showSendState: Boolean = true) {
    Div({ classes("x-post-head") }) {
        Span({ classes("x-name") }) { Text(post.author.displayLabel) }
        Span({ classes("x-handle") }) { Text("${post.author.handle} · ${post.createdAt.toRelativeDesc().text()}") }
        if (post.editedAt != null) EditedMark(onEdited)
        if (showSendState) when (post.publishState) {
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
