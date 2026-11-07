package com.yet.talk.web.ui.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.yet.talk.feature.tab.main.MainComponent
import com.yet.talk.feature.tab.main.component.details.DetailsComponent
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.root.component.RailActions
import com.yet.talk.web.ui.root.component.RailSelection
import com.yet.talk.web.ui.root.component.WebShell
import com.yet.talk.web.ui.root.component.ShellWidth
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.web.ui.screens.video.VideoContent
import com.yet.talk.web.ui.screens.video.VideoSide
import com.yet.talk.web.ui.screens.post.PostContent
import com.yet.talk.web.ui.screens.post.PostSide
import com.yet.talk.web.ui.screens.profile.ProfileContent
import com.yet.talk.web.ui.screens.tab.TabsContent
import com.yet.talk.web.ui.screens.tab.TabsSide

/**
 * Main on the web: always one pane (the design's web layout), so an open post or profile covers the tabs.
 * The rail stays; the right column holds search over the tabs and the replies over a post.
 */
@Composable
fun MainContent(component: MainComponent, railActions: RailActions) {
    val panels by component.panels.subscribeAsState()
    val tabs = panels.main.instance
    val me by tabs.me.subscribeAsState()
    val details = panels.details?.instance
    if (details == null) {
        val tabStack by tabs.stack.subscribeAsState()
        val active = tabStack.active.instance
        // Videos is a wide grid with no right column (`Videos-Web`).
        val videos = active is TabsComponent.Child.Videos
        WebShell(
            selection = RailSelection.Tab(active.tab),
            me = me,
            actions = railActions,
            width = if (videos) ShellWidth.Wide else ShellWidth.Column,
            right = if (videos) null else { { TabsSide(active, onSearch = { railActions.onTab(it) }) } },
        ) { TabsContent(active) }
    } else {
        DetailsShell(details, me, railActions)
    }
}

@Composable
private fun DetailsShell(details: DetailsComponent, me: com.yet.talk.feature.tab.main.component.tabs.TabsComponent.Me, railActions: RailActions) {
    val stack by details.stack.subscribeAsState()
    when (val page = stack.active.instance) {
        is DetailsComponent.Child.Post -> WebShell(RailSelection.None, me, railActions, right = { PostSide(page.component) }, phoneNav = false) {
            PostContent(page.component)
        }
        is DetailsComponent.Child.Profile -> WebShell(
            RailSelection.None, me, railActions, right = { TabsSide(null, onSearch = { railActions.onTab(it) }) }, phoneNav = false,
        ) {
            ProfileContent(page.component)
        }
        is DetailsComponent.Child.Video -> WebShell(
            RailSelection.Tab(TabsComponent.Tab.Videos), me, railActions, right = { VideoSide(page.component) }, phoneNav = false,
        ) { VideoContent(page.component) }
    }
}
