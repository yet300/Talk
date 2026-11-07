package com.yet.talk.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.yet.talk.feature.tab.main.MainComponent
import com.yet.talk.feature.tab.main.component.details.DetailsComponent
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.glass.GlassIconButton
import com.yet.talk.ui.components.glass.GlassTabBar
import com.yet.talk.ui.components.glass.GlassTopBar
import com.yet.talk.ui.components.glass.LocalGlassSource
import com.yet.talk.ui.components.glass.PhoneTabs
import com.yet.talk.ui.components.glass.label
import com.yet.talk.ui.screens.main.component.HomeHeader
import com.yet.talk.ui.screens.tab.home.HomeContent
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Main on a phone (single pane): the active tab under the glass bars, edge to edge, and the floating glass capsule;
 * an open post, profile or video covers the tabs (no capsule, Back instead).
 */
@Composable
fun MainContent(component: MainComponent, onOpenSettings: () -> Unit) {
    LaunchedEffect(component) { component.onModeChanged(ChildPanelsMode.SINGLE) }
    val panels by component.panels.subscribeAsState()
    val details = panels.details?.instance
    if (details != null) DetailsContent(details) else TabsContent(panels.main.instance, onOpenSettings)
}

@Composable
private fun TabsContent(tabs: TabsComponent, onOpenSettings: () -> Unit) {
    val stack by tabs.stack.subscribeAsState()
    val me by tabs.me.subscribeAsState()
    val active = stack.active.instance
    val glass = rememberHazeState()
    CompositionLocalProvider(LocalGlassSource provides glass) {
        Box(Modifier.fillMaxSize()) {
            // What scrolls under the bars; the bars refract it.
            Box(Modifier.fillMaxSize().background(MaterialTheme.talkColors.bg).hazeSource(glass)) {
                TabScreen(active)
            }
            if (active is TabsComponent.Child.Home) {
                HomeHeader(me, onProfile = { tabs.onTabClicked(TabsComponent.Tab.Profile) }, onSettings = onOpenSettings)
            }
            if (active.tab in PhoneTabs) {
                GlassTabBar(
                    selected = active.tab,
                    unreadChats = me.unreadChats,
                    onSelect = tabs::onTabClicked,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = MaterialTheme.sizes.capsuleOffset / 2),
                )
            }
        }
    }
}

/** Each tab's screen; the ones not built yet show their name. */
@Composable
private fun TabScreen(child: TabsComponent.Child) {
    when (child) {
        is TabsComponent.Child.Home -> HomeContent(child.component)
        else -> Placeholder(child.tab.label())
    }
}

@Composable
private fun DetailsContent(details: DetailsComponent) {
    val stack by details.stack.subscribeAsState()
    val glass = rememberHazeState()
    val title = when (stack.active.instance) {
        is DetailsComponent.Child.Post -> stringResource(MR.strings.post_title)
        is DetailsComponent.Child.Profile -> stringResource(MR.strings.tab_profile)
        is DetailsComponent.Child.Video -> stringResource(MR.strings.tab_videos)
    }
    CompositionLocalProvider(LocalGlassSource provides glass) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.talkColors.bg).hazeSource(glass)) { Placeholder(title) }
            GlassTopBar(
                leading = { GlassIconButton(TalkIcons.Back, stringResource(MR.strings.action_back), details::onBack) },
                title = { Text(title, style = MaterialTheme.talkTypography.headingMedium, color = MaterialTheme.talkColors.textPrimary) },
            )
        }
    }
}

@Composable
internal fun Placeholder(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.talkTypography.headingLarge, color = MaterialTheme.talkColors.textSecondary, textAlign = TextAlign.Center)
    }
}
