package com.yet.talk.ui.screens.tab.activity

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.paging.compose.collectAsLazyPagingItems
import com.yet.talk.feature.tab.activity.ActivityComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.EmptyState
import com.yet.talk.ui.components.glass.BackBar
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.components.list.PagedList
import com.yet.talk.ui.screens.tab.activity.component.ActivityRow
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource

/** Activity (`Activity-Mobile`, decision 1): who replied, quoted, reposted, liked or followed, newest first. */
@Composable
fun ActivityContent(component: ActivityComponent) {
    val items = component.items.collectAsLazyPagingItems()
    val sizes = MaterialTheme.sizes
    GlassPage(bar = { BackBar(stringResource(MR.strings.activity_title), onBack = null) }) { padding ->
        PagedList(
            items = items,
            key = { "${it.type}:${it.actor.id.value}:${it.createdAt}:${(it.eventPost ?: it.subjectPost)?.id?.value}" },
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + sizes.capsuleHeight + sizes.capsuleOffset + MaterialTheme.spacing.lg),
            contentType = { it.type },
            empty = { EmptyState(stringResource(MR.strings.activity_empty_title), stringResource(MR.strings.activity_empty_body), icon = TalkIcons.Activity) },
        ) { item -> ActivityRow(item, onOpenPost = component::onOpenPost, onOpenProfile = component::onOpenProfile) }
    }
}
