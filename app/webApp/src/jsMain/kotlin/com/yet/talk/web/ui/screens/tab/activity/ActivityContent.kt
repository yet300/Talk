package com.yet.talk.web.ui.screens.tab.activity

import androidx.compose.runtime.Composable
import com.yet.talk.feature.tab.activity.ActivityComponent
import com.yet.talk.resources.*
import com.yet.talk.web.paging.collectAsPagingItems
import com.yet.talk.web.ui.components.EmptyState
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.components.list.PagedList
import com.yet.talk.web.ui.screens.tab.activity.component.ActivityRow
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource

/** Activity (no artboard; decided to reuse the Search people row): who replied, quoted, reposted, liked, followed. */
@Composable
fun ActivityContent(component: ActivityComponent) {
    val items = component.items.collectAsPagingItems()
    TopBar(stringResource(MR.strings.activity_title))
    PagedList(
        items,
        empty = {
            EmptyState(
                icon = TalkIcons.Activity,
                title = stringResource(MR.strings.activity_empty_title),
                body = stringResource(MR.strings.activity_empty_body),
            )
        },
    ) { item -> ActivityRow(item, onOpenPost = component::onOpenPost, onOpenProfile = component::onOpenProfile) }
}
