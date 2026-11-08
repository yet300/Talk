package com.yet.talk.ui.screens.tab.activity.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.activity.ActivityItem
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.resources.*
import com.yet.talk.ui.components.PersonRow
import com.yet.talk.ui.components.LocalSelectedPost
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.text

/** "<name> liked your post" with the time and the post's title; opens the reply or quote, the post, or the person. */
@Composable
fun ActivityRow(item: ActivityItem, onOpenPost: (PostId) -> Unit, onOpenProfile: (UserId) -> Unit) {
    val title = stringResource(
        when (item.type) {
            ActivityItem.Type.Reply -> MR.strings.activity_replied
            ActivityItem.Type.Quote -> MR.strings.activity_quoted
            ActivityItem.Type.Repost -> MR.strings.activity_reposted
            ActivityItem.Type.Like -> MR.strings.activity_liked
            ActivityItem.Type.Follow -> MR.strings.activity_followed
        },
        item.actor.displayLabel,
    )
    val post = item.eventPost ?: item.subjectPost
    val caption = listOfNotNull(item.createdAt.toRelativeDesc().text(), post?.summary?.title?.takeIf(String::isNotBlank)).joinToString(" · ")
    PersonRow(
        user = item.actor,
        title = title,
        caption = caption,
        selected = post != null && LocalSelectedPost.current == post.id,
        onClick = { if (item.type == ActivityItem.Type.Follow || post == null) onOpenProfile(item.actor.id) else onOpenPost(post.id) },
    )
}
