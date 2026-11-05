package com.yet.talk.feature.tab.activity

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.yet.talk.domain.model.activity.ActivityItem
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.ActivityRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

internal class DefaultActivityComponent(
    componentContext: ComponentContext,
    private val output: (ActivityComponent.Output) -> Unit,
    activityRepository: ActivityRepository,
) : ActivityComponent, ComponentContext by componentContext {

    override val items: Flow<PagingData<ActivityItem>> = activityRepository.activity().cachedIn(coroutineScope())

    override fun onOpenPost(id: PostId) = output(ActivityComponent.Output.OpenPost(id))

    override fun onOpenProfile(id: UserId) = output(ActivityComponent.Output.OpenProfile(id))
}

@Inject
internal class DefaultActivityComponentFactory(private val activityRepository: ActivityRepository) : ActivityComponent.Factory {
    override fun invoke(componentContext: ComponentContext, output: (ActivityComponent.Output) -> Unit): ActivityComponent =
        DefaultActivityComponent(componentContext, output, activityRepository)
}
