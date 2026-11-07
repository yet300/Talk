package com.yet.talk.web.ui.screens.tab

import androidx.compose.runtime.Composable
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.SearchField
import com.yet.talk.web.ui.screens.profile.ProfileContent
import com.yet.talk.web.ui.screens.tab.activity.ActivityContent
import com.yet.talk.web.ui.screens.tab.bookmarks.BookmarksContent
import com.yet.talk.web.ui.screens.tab.chats.ChatsContent
import com.yet.talk.web.ui.screens.tab.home.HomeContent
import com.yet.talk.web.ui.screens.tab.search.SearchContent
import com.yet.talk.web.ui.screens.tab.videos.VideosContent
import com.yet.talk.web.ui.theme.stringResource

/** The active tab in the main column. */
@Composable
fun TabsContent(child: TabsComponent.Child) {
    when (child) {
        is TabsComponent.Child.Home -> HomeContent(child.component)
        is TabsComponent.Child.Search -> SearchContent(child.component)
        is TabsComponent.Child.Activity -> ActivityContent(child.component)
        is TabsComponent.Child.Chats -> ChatsContent(child.component)
        is TabsComponent.Child.Videos -> VideosContent(child.component)
        is TabsComponent.Child.Bookmarks -> BookmarksContent(child.component)
        is TabsComponent.Child.Profile -> ProfileContent(child.component)
    }
}

/**
 * The right column next to a tab: the search field (the design's timeline has only this, no widget).
 * Focusing it opens the Search tab, where results appear in the main column.
 */
@Composable
fun TabsSide(active: TabsComponent.Child?, onSearch: (TabsComponent.Tab) -> Unit) {
    if (active is TabsComponent.Child.Search) return
    SearchField(
        value = "",
        onValueChange = {},
        placeholder = stringResource(MR.strings.search_placeholder),
        id = "side-search",
        onFocus = { onSearch(TabsComponent.Tab.Search) },
    )
}
