package com.yet.talk.web.ui.screens.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.rememberIsPhone
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
    val actions = PostActionHandlers(component::onReply, component::onRepost, component::onLike, component::onBookmark, component::onOpenRevisions)

    val phone = rememberIsPhone()
    TopBar(
        title = model.user?.displayLabel ?: stringResource(MR.strings.tab_profile),
        // Your profile is a tab on the web; on the phone it opens from the home header, so it has Back too.
        onBack = component::onBack.takeIf { model.canGoBack || phone },
    )
    ProfileHeader(model, onFollow = component::onFollowToggle, onEditProfile = component::onEditProfile)
    // Bookmarks is a tab of your own profile on the phone only; the web has its own Bookmarks destination.
    val tabs = ProfileComponent.Tab.entries.filter { it != ProfileComponent.Tab.Bookmarks || (phone && model.isMe) }
    LaunchedEffect(tabs, model.tab) { if (model.tab !in tabs) component.onTabSelected(ProfileComponent.Tab.Posts) }
    Tabs(
        items = tabs,
        selected = model.tab,
        label = {
            stringResource(
                when (it) {
                    ProfileComponent.Tab.Posts -> MR.strings.profile_posts
                    ProfileComponent.Tab.Replies -> MR.strings.profile_replies
                    ProfileComponent.Tab.Bookmarks -> MR.strings.profile_bookmarks
                },
            )
        },
        onSelect = component::onTabSelected,
        ariaLabel = stringResource(MR.strings.tab_profile),
    )
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
    PagedList(
        posts,
        empty = {
            when (model.tab) {
                ProfileComponent.Tab.Posts -> EmptyState(title = stringResource(MR.strings.profile_empty_title))
                ProfileComponent.Tab.Replies -> EmptyState(title = stringResource(MR.strings.profile_empty_replies_title))
                ProfileComponent.Tab.Bookmarks -> EmptyState(
                    title = stringResource(MR.strings.bookmarks_empty_title),
                    body = stringResource(MR.strings.bookmarks_empty_body),
                )
            }
        },
    ) { post ->
        PostCard(post, onOpen = { component.onOpenPost(it.id) }, onOpenAuthor = { component.onOpenAuthor(it.author.id) }, actions = actions)
    }
}
