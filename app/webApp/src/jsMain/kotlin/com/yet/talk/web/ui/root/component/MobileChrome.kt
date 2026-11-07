package com.yet.talk.web.ui.root.component

import androidx.compose.runtime.Composable
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.trackScroll
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.components.AvatarSize
import com.yet.talk.web.ui.components.IconButton
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Nav
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * The phone's bottom navigation (`MobileNav`): a glass capsule pinned above the bottom edge, icons only,
 * the active tab on a raised pill: Home, Search, Activity, Videos (decision 9). Your profile (with its
 * Bookmarks tab) and Settings open from the home header.
 */
@Composable
fun MobileNav(selection: RailSelection, actions: RailActions) {
    val label = stringResource(MR.strings.nav_primary)
    Div({ classes("x-bottom-nav-dock", "t-mobile-dock") }) {
        Nav({ classes("x-bottom-nav"); attr("aria-label", label) }) {
            PhoneTabs.forEach { tab ->
                val active = selection == RailSelection.Tab(tab)
                val name = tab.label()
                A(href = "#", attrs = {
                    classes("x-bottom-nav-item")
                    attr("aria-label", name)
                    if (active) attr("aria-current", "page")
                    onClick { it.preventDefault(); actions.onTab(tab) }
                }) { TalkIcon(if (tab == TabsComponent.Tab.Videos) TalkIcons.Play else tab.icon(active)) }
            }
        }
    }
}

/** The tabs in the phone's navigation; Profile opens from the home header's avatar, Bookmarks is a tab of it. */
internal val PhoneTabs = TabsComponent.Tab.entries - TabsComponent.Tab.Profile - TabsComponent.Tab.Bookmarks

/** The home header on phones (`x-mobile-header`): your avatar (Profile), the wordmark, Settings. */
@Composable
fun MobileHomeHeader(me: TabsComponent.Me, actions: RailActions) {
    Header({
        classes("x-mobile-header", "t-bar-sticky")
        ref { bar -> val stop = trackScroll(bar); onDispose { stop() } }
    }) {
        Avatar(me.user, AvatarSize.Small, onClick = { actions.onTab(TabsComponent.Tab.Profile) })
        Span({ classes("t-wordmark") }) { Text(stringResource(MR.strings.app_name)) }
        IconButton(TalkIcons.Settings, stringResource(MR.strings.action_settings), actions.onSettings, attrs = { classes("t-header-end") })
    }
}

/** The compose button floating above the navigation (`x-fab`), on the home timeline. */
@Composable
fun ComposeFab(actions: RailActions) {
    val label = stringResource(MR.strings.action_compose)
    Div({ classes("t-fab") }) {
        A(href = "/compose", attrs = {
            classes("x-fab")
            attr("aria-label", label)
            onClick { it.preventDefault(); actions.onCompose() }
        }) { TalkIcon(TalkIcons.Compose) }
    }
}
