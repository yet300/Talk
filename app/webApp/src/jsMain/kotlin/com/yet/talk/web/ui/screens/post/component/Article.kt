package com.yet.talk.web.ui.screens.post.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yet.talk.domain.model.post.Post
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.copyToClipboard
import com.yet.talk.web.browser.shareOrCopy
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.markdown.Markdown
import com.yet.talk.web.ui.components.post.ActionButton
import com.yet.talk.web.ui.components.post.PostHead
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Article
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement

/**
 * The article (`Article`): title, author row with Follow, the sticky action row, the markdown body, tags and
 * the copyable event id. A leading `# heading` becomes the title, so it is not shown twice.
 */
@Composable
fun Article(post: Post, model: PostComponent.Model, component: PostComponent) {
    val (title, body) = remember(post.content) { splitTitle(post.content) }
    Article({ classes("t-part") }) {
        post.thread?.let { thread ->
            Div({ classes("t-reply-to") }) {
                A(href = "/post/${thread.parentId.value}", attrs = {
                    onClick { it.preventDefault(); component.onOpenPost(thread.parentId) }
                }) { Text(stringResource(MR.strings.post_show_parent)) }
            }
        }
        if (title != null) Div({ classes("t-md", "t-article-title") }) { H1 { Text(title) } }
        AuthorRow(post, model, component)
        ActionRow(post, model, component)
        model.quoted?.let { QuotedPost(it, onOpen = { component.onOpenPost(it.id) }) }
        Div({ classes("t-article-body") }) { Markdown(body) }
        val copyLabel = stringResource(MR.strings.post_copy_address)
        Div({ classes("t-article-footer") }) {
            if (post.summary.tags.isNotEmpty()) {
                Div({ classes("t-tags", "t-flush") }) { post.summary.tags.forEach { Span { Text("#$it") } } }
            }
            Button({
                classes("x-action", "t-caption", "t-copy-id")
                type(ButtonType.Button)
                attr("aria-label", copyLabel)
                onClick { copyToClipboard(model.shareLink) }
            }) {
                TalkIcon(TalkIcons.Copy, size = 18f)
                Text(model.shareLink.shortNpub())
            }
        }
    }
}

@Composable
private fun AuthorRow(post: Post, model: PostComponent.Model, component: PostComponent) {
    Div({ classes("t-author") }) {
        Avatar(post.author, onClick = { component.onOpenAuthor(post.author.id) })
        Div({ classes("t-grow") }) {
            Div({ classes("x-name") }) { Text(post.author.displayLabel) }
            Div({ classes("t-caption") }) {
                Text(listOf(post.author.handle, post.createdAt.toDateText(), stringResource(MR.strings.post_read_time, post.summary.readMinutes)).joinToString(" · "))
            }
        }
        if (!model.isMine) {
            TalkButton(
                text = stringResource(if (model.isFollowingAuthor) MR.strings.action_following else MR.strings.action_follow),
                onClick = component::onFollowAuthor,
                style = ButtonStyle.Secondary,
            )
        }
    }
}

/** The sticky row under the author: Reply, Repost, Like, Bookmark, Share, Revisions. */
@Composable
private fun ActionRow(post: Post, model: PostComponent.Model, component: PostComponent) {
    Div({ classes("t-article-actions") }) {
        ActionButton("x-reply", TalkIcons.Reply, post.stats.replies, stringResource(MR.strings.a11y_replies, post.stats.replies.toInt())) {
            // One pane on the web: the reply field is right there (under the article or in the right column).
            (document.querySelector(".t-reply-box input") as? HTMLElement)?.focus()
        }
        ActionButton(
            "x-repost", TalkIcons.Repost, post.stats.reposts, stringResource(MR.strings.a11y_reposts, post.stats.reposts.toInt()),
            activeClass = "t-on-repost".takeIf { post.viewer.reposted },
        ) { component.onRepost(post) }
        ActionButton(
            "x-like", if (post.viewer.liked) TalkIcons.LikeFilled else TalkIcons.Like, post.stats.likes,
            stringResource(MR.strings.a11y_likes, post.stats.likes.toInt()),
            activeClass = "t-on-like".takeIf { post.viewer.liked },
        ) { component.onLike(post) }
        ActionButton(
            "x-bookmark", if (post.viewer.bookmarked) TalkIcons.BookmarkFilled else TalkIcons.Bookmark, null,
            stringResource(if (post.viewer.bookmarked) MR.strings.action_remove_bookmark else MR.strings.action_bookmark),
            activeClass = "t-on-blue".takeIf { post.viewer.bookmarked },
        ) { component.onBookmark(post) }
        ActionButton("x-share", TalkIcons.Share, null, stringResource(MR.strings.action_share)) { shareOrCopy(model.shareLink) }
        ActionButton("x-share", TalkIcons.History, null, stringResource(MR.strings.action_revisions)) { component.onOpenRevisions() }
    }
}

/** A quoted post (`x-quote`): its head and title; opens it. */
@Composable
private fun QuotedPost(post: Post, onOpen: () -> Unit) {
    Div({ classes("x-quote", "t-quote"); onClick { onOpen() } }) {
        PostHead(post)
        Div({ classes("t-card-title") }) { Text(post.summary.title) }
    }
}

/** A leading `# Title` line, and the markdown after it; no title when the post does not start with one. */
private fun splitTitle(content: String): Pair<String?, String> {
    val trimmed = content.trimStart()
    val first = trimmed.lineSequence().firstOrNull().orEmpty()
    return if (first.startsWith("# ")) first.removePrefix("# ").trim() to trimmed.removePrefix(first).trimStart('\n') else null to content
}
