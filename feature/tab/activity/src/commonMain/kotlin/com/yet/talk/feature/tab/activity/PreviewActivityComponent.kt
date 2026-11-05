package com.yet.talk.feature.tab.activity

import androidx.paging.PagingData
import com.yet.talk.domain.model.activity.ActivityItem
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** For UI previews: fixed items, actions do nothing. */
class PreviewActivityComponent(items: List<ActivityItem> = emptyList()) : ActivityComponent {
    override val items: Flow<PagingData<ActivityItem>> = flowOf(PagingData.from(items))

    override fun onOpenPost(id: PostId) = Unit

    override fun onOpenProfile(id: UserId) = Unit
}
