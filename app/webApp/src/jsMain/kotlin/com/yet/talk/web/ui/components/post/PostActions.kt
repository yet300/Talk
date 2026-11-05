package com.yet.talk.web.ui.components.post

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.post.Post
import com.yet.talk.resources.*
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import dev.icerock.moko.resources.ImageResource
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** What the action row does; each screen routes these to its component. */
class PostActionHandlers(
    val onReply: (Post) -> Unit,
    val onRepost: (Post) -> Unit,
    val onLike: (Post) -> Unit,
    val onBookmark: (Post) -> Unit,
    /** "Edited" next to the time. */
    val onOpenRevisions: (Post) -> Unit,
)

/** The card's action row (`x-actions`): Reply, Repost (green when reposted), Like (pink, filled), Bookmark (blue, filled). */
@Composable
fun PostActions(post: Post, handlers: PostActionHandlers) {
    Div({ classes("x-actions") }) {
        ActionButton(
            kind = "x-reply", icon = TalkIcons.Reply, count = post.stats.replies,
            label = stringResource(MR.strings.a11y_replies, post.stats.replies.toInt()),
        ) { handlers.onReply(post) }
        ActionButton(
            kind = "x-repost", icon = TalkIcons.Repost, count = post.stats.reposts,
            label = stringResource(MR.strings.a11y_reposts, post.stats.reposts.toInt()),
            activeClass = "t-on-repost".takeIf { post.viewer.reposted },
        ) { handlers.onRepost(post) }
        ActionButton(
            kind = "x-like", icon = if (post.viewer.liked) TalkIcons.LikeFilled else TalkIcons.Like, count = post.stats.likes,
            label = stringResource(MR.strings.a11y_likes, post.stats.likes.toInt()),
            activeClass = "t-on-like".takeIf { post.viewer.liked },
        ) { handlers.onLike(post) }
        ActionButton(
            kind = "x-bookmark", icon = if (post.viewer.bookmarked) TalkIcons.BookmarkFilled else TalkIcons.Bookmark, count = null,
            label = stringResource(if (post.viewer.bookmarked) MR.strings.action_remove_bookmark else MR.strings.action_bookmark),
            activeClass = "t-on-blue".takeIf { post.viewer.bookmarked },
        ) { handlers.onBookmark(post) }
    }
}

/** One action (`x-action`): a 34.75 px round target with an 18 px icon and an optional count. */
@Composable
fun ActionButton(
    kind: String,
    icon: ImageResource,
    count: Long?,
    label: String,
    activeClass: String? = null,
    onClick: () -> Unit,
) {
    Button({
        classes("x-action", kind)
        activeClass?.let { classes(it) }
        type(ButtonType.Button)
        attr("aria-label", label)
        attr("aria-pressed", (activeClass != null).toString())
        onClick { it.stopPropagation(); onClick() }
    }) {
        Span({ classes("x-action-icon") }) { TalkIcon(icon, size = 18f) }
        if (count != null && count > 0) Text(count.toString())
    }
}
