package com.yet.talk.feature.tab.activity

import androidx.paging.PagingData
import com.arkivanov.decompose.ComponentContext
import com.yet.talk.domain.model.activity.ActivityItem
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import kotlinx.coroutines.flow.Flow

/**
 * KarpathyTalk's Activity: replies, quotes, likes and reposts of the viewer's posts, and new followers.
 * Read-only, so there is no store: the paged list comes straight from the repository.
 */
interface ActivityComponent {
    /** Newest first, 20 a page; cached for the component's lifetime. */
    val items: Flow<PagingData<ActivityItem>>

    fun onOpenPost(id: PostId)

    fun onOpenProfile(id: UserId)

    sealed interface Output {
        data class OpenPost(val id: PostId) : Output

        data class OpenProfile(val id: UserId) : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, output: (Output) -> Unit): ActivityComponent
    }
}
