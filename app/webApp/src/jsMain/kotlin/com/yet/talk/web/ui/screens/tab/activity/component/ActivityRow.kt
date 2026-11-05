package com.yet.talk.web.ui.screens.tab.activity.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.activity.ActivityItem
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.list.PersonRow
import com.yet.talk.web.ui.theme.stringResource
import com.yet.talk.web.ui.theme.text

/** "<name> liked your post" with the time; opens the reply or quote, the post, or (a follow) the person. */
@Composable
fun ActivityRow(item: ActivityItem, onOpenPost: (PostId) -> Unit, onOpenProfile: (UserId) -> Unit) {
    val name = item.actor.displayLabel
    val title = stringResource(
        when (item.type) {
            ActivityItem.Type.Reply -> MR.strings.activity_replied
            ActivityItem.Type.Quote -> MR.strings.activity_quoted
            ActivityItem.Type.Repost -> MR.strings.activity_reposted
            ActivityItem.Type.Like -> MR.strings.activity_liked
            ActivityItem.Type.Follow -> MR.strings.activity_followed
        },
        name,
    )
    val excerpt = (item.eventPost ?: item.subjectPost)?.summary?.title
    val caption = listOfNotNull(item.createdAt.toRelativeDesc().text(), excerpt).joinToString(" · ")
    PersonRow(
        user = item.actor,
        title = title,
        caption = caption,
        onClick = {
            val post = item.eventPost ?: item.subjectPost
            if (item.type == ActivityItem.Type.Follow || post == null) onOpenProfile(item.actor.id) else onOpenPost(post.id)
        },
    )
}
