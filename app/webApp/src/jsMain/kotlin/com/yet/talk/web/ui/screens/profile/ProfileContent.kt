package com.yet.talk.web.ui.screens.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.paging.collectAsPagingItems
import com.yet.talk.web.ui.components.EmptyState
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.Tabs
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.components.list.PagedList
import com.yet.talk.web.ui.components.post.PostActionHandlers
import com.yet.talk.web.ui.components.post.PostCard
import com.yet.talk.web.ui.screens.profile.component.ProfileHeader
import com.yet.talk.web.ui.theme.stringResource

/** A profile (`Profile · Web`): bar with the name, the full header, Posts · Replies, the paged cards. */
@Composable
fun ProfileContent(component: ProfileComponent) {
    val model by component.model.subscribeAsState()
    val posts = component.posts.collectAsPagingItems()
    val actions = PostActionHandlers(component::onReply, component::onRepost, component::onLike, component::onBookmark)

    TopBar(
        title = model.user?.displayLabel ?: stringResource(MR.strings.tab_profile),
        onBack = component::onBack.takeIf { model.canGoBack },
    )
    ProfileHeader(model, onFollow = component::onFollowToggle)
    Tabs(
        items = ProfileComponent.Tab.entries,
        selected = model.tab,
        label = { stringResource(if (it == ProfileComponent.Tab.Posts) MR.strings.profile_posts else MR.strings.profile_replies) },
        onSelect = component::onTabSelected,
        ariaLabel = stringResource(MR.strings.tab_profile),
    )
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
    PagedList(
        posts,
        empty = {
            EmptyState(
                title = stringResource(
                    if (model.tab == ProfileComponent.Tab.Posts) MR.strings.profile_empty_title else MR.strings.profile_empty_replies_title,
                ),
            )
        },
    ) { post ->
        PostCard(post, onOpen = { component.onOpenPost(it.id) }, onOpenAuthor = { component.onOpenAuthor(it.author.id) }, actions = actions)
    }
}
