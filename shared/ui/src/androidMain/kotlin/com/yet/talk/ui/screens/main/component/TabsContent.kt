@file:OptIn(ExperimentalDecomposeApi::class)

package com.yet.talk.ui.screens.main.component

import com.yet.talk.ui.navigation.Page
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.ui.components.glass.GlassTabBar
import com.yet.talk.ui.components.glass.LocalGlassSource
import com.yet.talk.ui.components.glass.PhoneTabs
import com.yet.talk.ui.navigation.pagesAnimation
import com.yet.talk.ui.screens.profile.ProfileContent
import com.yet.talk.ui.screens.tab.activity.ActivityContent
import com.yet.talk.ui.screens.tab.bookmarks.BookmarksContent
import com.yet.talk.ui.screens.tab.chats.ChatsContent
import com.yet.talk.ui.screens.tab.home.HomeContent
import com.yet.talk.ui.screens.tab.search.SearchContent
import com.yet.talk.ui.screens.tab.videos.VideosContent
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.talkColors
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * The tabs (`ChildStack`): switching tabs crossfades; your profile and Bookmarks, pages above the tabs on a phone
 * (decision 17), slide over them and go back Home with the predictive back gesture. On a [phone] the floating glass
 * capsule shows on its five tabs only, and hides while a chat is open over the Chats list.
 */
@Composable
fun TabsContent(tabs: TabsComponent, onOpenSettings: () -> Unit, phone: Boolean) {
    val stack by tabs.stack.subscribeAsState()
    val me by tabs.me.subscribeAsState()
    val active = stack.active.instance
    val chatOpen = (active as? TabsComponent.Child.Chats)?.component?.pane?.subscribeAsState()?.value?.child != null
    val glass = rememberHazeState()
    CompositionLocalProvider(LocalGlassSource provides glass) {
        Box(Modifier.fillMaxSize()) {
            ChildStack(
                stack = tabs.stack,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.talkColors.bg).hazeSource(glass),
                animation = pagesAnimation(
                    backHandler = tabs.backHandler,
                    onBack = { stack.active.instance.leave() },
                    canGoBack = { it.backStack.isNotEmpty() && it.active.instance.isAbove },
                    swap = { child, other -> !child.instance.isAbove && !other.instance.isAbove },
                ),
            ) { child -> Page {
                when (val tab = child.instance) {
                    is TabsComponent.Child.Home -> HomeContent(tab.component, me, onProfile = { tabs.onTabClicked(TabsComponent.Tab.Profile) }, onSettings = onOpenSettings, phone = phone)
                    is TabsComponent.Child.Search -> SearchContent(tab.component)
                    is TabsComponent.Child.Activity -> ActivityContent(tab.component)
                    is TabsComponent.Child.Chats -> ChatsContent(tab.component)
                    is TabsComponent.Child.Videos -> VideosContent(tab.component)
                    is TabsComponent.Child.Bookmarks -> BookmarksContent(tab.component, canGoBack = phone)
                    is TabsComponent.Child.Profile -> ProfileContent(tab.component, canGoBack = phone)
                }
            }}
            AnimatedVisibility(
                visible = phone && active.tab in PhoneTabs && !chatOpen,
                modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = MaterialTheme.sizes.capsuleOffset / 2),
                enter = slideInVertically { it } + fadeIn() + scaleIn(initialScale = 0.9f),
                exit = slideOutVertically { it } + fadeOut() + scaleOut(targetScale = 0.9f),
            ) {
                GlassTabBar(selected = active.tab, unreadChats = me.unreadChats, onSelect = tabs::onTabClicked)
            }
        }
    }
}

/** Your profile and Bookmarks are pages above the tabs on a phone, with Back (decision 17). */
private val TabsComponent.Child.isAbove: Boolean
    get() = this is TabsComponent.Child.Profile || this is TabsComponent.Child.Bookmarks

private fun TabsComponent.Child.leave() = when (this) {
    is TabsComponent.Child.Profile -> component.onBack()
    is TabsComponent.Child.Bookmarks -> component.onBrowseHome()
    else -> Unit
}
