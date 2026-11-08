package com.yet.talk.ui.components.post

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import com.yet.talk.ui.theme.TalkMotion
import com.yet.talk.ui.theme.reducedMotion
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yet.talk.domain.model.post.Post
import com.yet.talk.resources.*
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import dev.icerock.moko.resources.ImageResource

/** What the action row does; each screen routes these to its component (remember it, it never changes). */
@androidx.compose.runtime.Immutable
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
    val colors = MaterialTheme.talkColors
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Action(TalkIcons.Reply, post.stats.replies, stringResource(MR.strings.a11y_replies, post.stats.replies.toInt()), null) { handlers.onReply(post) }
        Action(
            TalkIcons.Repost, post.stats.reposts, stringResource(MR.strings.a11y_reposts, post.stats.reposts.toInt()),
            colors.repost.takeIf { post.viewer.reposted },
        ) { handlers.onRepost(post) }
        Action(
            if (post.viewer.liked) TalkIcons.LikeFilled else TalkIcons.Like, post.stats.likes,
            stringResource(MR.strings.a11y_likes, post.stats.likes.toInt()), colors.like.takeIf { post.viewer.liked },
        ) { handlers.onLike(post) }
        Action(
            if (post.viewer.bookmarked) TalkIcons.BookmarkFilled else TalkIcons.Bookmark, null,
            stringResource(if (post.viewer.bookmarked) MR.strings.action_remove_bookmark else MR.strings.action_bookmark),
            colors.accent.takeIf { post.viewer.bookmarked },
        ) { handlers.onBookmark(post) }
    }
}

/**
 * One action (`x-action`): an 18 dp icon with its count, a 44 dp target; its colour eases to the on colour, and
 * turning it on pops the icon with a small spring (only when the finger did it, not when the list scrolls it in).
 */
@Composable
private fun Action(icon: ImageResource, count: Long?, label: String, onColor: Color?, onClick: () -> Unit) {
    val on = onColor != null
    val tint by animateColorAsState(onColor ?: MaterialTheme.talkColors.textSecondary, TalkMotion.settle(), label = "tint")
    val scale = remember { Animatable(1f) }
    val seen = remember { BooleanHolder(on) }
    val reduced = reducedMotion
    LaunchedEffect(on) {
        if (on && !seen.value && !reduced) {
            scale.snapTo(0.6f)
            scale.animateTo(1f, TalkMotion.pop())
        }
        seen.value = on
    }
    Row(
        modifier = Modifier
            .heightIn(min = MaterialTheme.sizes.touchMin)
            .widthIn(min = MaterialTheme.sizes.touchMin)
            .clip(CircleShape)
            .toggleable(value = on, role = Role.Button, onValueChange = { onClick() })
            .semantics { contentDescription = label }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TalkIcon(
            icon, contentDescription = null, size = MaterialTheme.sizes.iconAction, tint = tint,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        )
        if (count != null && count > 0) {
            AnimatedContent(count, transitionSpec = { countTransition() }, label = "count") { value ->
                Text(value.toString(), style = MaterialTheme.talkTypography.small, color = tint, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

private class BooleanHolder(var value: Boolean)

/** A count rolls up when it grows and down when it shrinks. */
private fun AnimatedContentTransitionScope<Long>.countTransition(): ContentTransform {
    val up = targetState > initialState
    return (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith (slideOutVertically { if (up) -it else it } + fadeOut())
}
