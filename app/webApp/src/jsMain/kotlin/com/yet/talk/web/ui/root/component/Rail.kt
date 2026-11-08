package com.yet.talk.web.ui.root.component

import com.yet.talk.web.ui.components.rememberNavPill
import com.yet.talk.web.ui.components.navPillContainer
import com.yet.talk.web.ui.components.NavPill
import androidx.compose.runtime.Composable
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.pluralResource
import com.yet.talk.web.ui.theme.stringResource
import dev.icerock.moko.resources.ImageResource
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Nav
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** What the rail highlights: a tab, Settings, or nothing (reader, editor, revisions). */
sealed interface RailSelection {
    data class Tab(val tab: TabsComponent.Tab) : RailSelection
    data object Settings : RailSelection
    data object None : RailSelection
}

/** Where the rail leads; the root implements these so the rail works above Main too. */
class RailActions(
    val onTab: (TabsComponent.Tab) -> Unit,
    val onCompose: () -> Unit,
    val onSettings: () -> Unit,
)

/**
 * The web left rail (`Rail`): brand, Home · Search · Activity · Chats (with the unread count) · Videos · Bookmarks · Profile · Settings, the Compose
 * button, and the identity row. CSS collapses it to icons below 1225 px and turns it into a bottom bar
 * below 688 px.
 */
@Composable
fun Rail(selection: RailSelection, me: TabsComponent.Me, actions: RailActions) {
    val navLabel = stringResource(MR.strings.nav_primary)
    val appName = stringResource(MR.strings.app_name)
    val composeLabel = stringResource(MR.strings.action_compose)
    val identityLabel = stringResource(MR.strings.settings_identity)
    Nav({ classes("t-part", "t-rail"); attr("aria-label", navLabel) }) {
        A(href = "/", attrs = {
            classes("t-rail-brand", "t-wordmark")
            attr("aria-label", appName)
            onClick { it.preventDefault(); actions.onTab(TabsComponent.Tab.Home) }
        }) { Text(appName) }
        val pill = rememberNavPill(selection)
        Div({ classes("t-rail-items"); navPillContainer(pill) }) {
            NavPill()
            TabsComponent.Tab.entries.forEach { tab ->
                val badge = if (tab == TabsComponent.Tab.Chats) me.unreadChats else 0
                RailItem(tab.icon(selection == RailSelection.Tab(tab)), tab.label(), selection == RailSelection.Tab(tab), badge) { actions.onTab(tab) }
            }
            RailItem(TalkIcons.Settings, stringResource(MR.strings.action_settings), selection == RailSelection.Settings, onClick = actions.onSettings)
        }
        A(href = "/compose", attrs = {
            classes("x-btn", "x-btn-post", "t-rail-post")
            attr("aria-label", composeLabel)
            onClick { it.preventDefault(); actions.onCompose() }
        }) {
            Span({ classes("t-rail-label") }) { Text(composeLabel) }
            Span({ classes("t-rail-post-icon") }) { TalkIcon(TalkIcons.Compose) }
        }
        A(href = "/settings", attrs = {
            classes("t-rail-me")
            attr("aria-label", identityLabel)
            onClick { it.preventDefault(); actions.onSettings() }
        }) {
            Avatar(me.user)
            Span({ classes("t-rail-me-text") }) {
                Span({ classes("x-name", "t-block") }) { Text(me.user?.displayLabel ?: me.npub?.shortNpub().orEmpty()) }
                Span({ classes("t-caption", "t-block") }) {
                    Text(listOfNotNull(me.npub?.shortNpub(), pluralResource(MR.plurals.relays_count, me.relayCount)).joinToString(" · "))
                }
            }
        }
    }
}

@Composable
private fun RailItem(icon: ImageResource, label: String, active: Boolean, badge: Int = 0, onClick: () -> Unit) {
    val name = if (badge > 0) pluralResource(MR.plurals.chats_unread_count, badge).let { "$label, $it" } else label
    A(href = "#", attrs = {
        classes("x-nav-item")
        if (active) classes("is-active")
        attr("aria-label", name)
        if (active) attr("aria-current", "page")
        onClick { it.preventDefault(); onClick() }
    }) {
        TalkIcon(icon)
        Span({ classes("t-rail-label") }) { Text(label) }
        if (badge > 0) NavBadge(badge)
    }
}

internal fun TabsComponent.Tab.icon(active: Boolean): ImageResource = when (this) {
    TabsComponent.Tab.Home -> TalkIcons.Home
    TabsComponent.Tab.Search -> TalkIcons.Search
    TabsComponent.Tab.Activity -> TalkIcons.Activity
    TabsComponent.Tab.Chats -> TalkIcons.Chat
    TabsComponent.Tab.Videos -> TalkIcons.Video
    TabsComponent.Tab.Bookmarks -> if (active) TalkIcons.BookmarkFilled else TalkIcons.Bookmark
    TabsComponent.Tab.Profile -> TalkIcons.User
}

@Composable
internal fun TabsComponent.Tab.label(): String = stringResource(
    when (this) {
        TabsComponent.Tab.Home -> MR.strings.tab_home
        TabsComponent.Tab.Search -> MR.strings.tab_search
        TabsComponent.Tab.Activity -> MR.strings.tab_activity
        TabsComponent.Tab.Chats -> MR.strings.tab_chats
        TabsComponent.Tab.Videos -> MR.strings.tab_videos
        TabsComponent.Tab.Bookmarks -> MR.strings.tab_bookmarks
        TabsComponent.Tab.Profile -> MR.strings.tab_profile
    },
)

/** The unread count on a navigation item (`t-nav-badge`); the item's label says it to screen readers. */
@Composable
internal fun NavBadge(count: Int) {
    Span({ classes("t-nav-badge"); attr("aria-hidden", "true") }) { Text(if (count > 99) "99+" else count.toString()) }
}
