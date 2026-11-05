package com.yet.talk.web.ui.root.component

import androidx.compose.runtime.Composable
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.web.browser.rememberIsPhone
import org.jetbrains.compose.web.dom.Aside
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Main

/** How wide the content is: the 600 px column with the right column, or the wide editor column. */
enum class ShellWidth { Column, Wide }

/**
 * The signed-in page. Wider than a phone (`t-web`): the rail, the main column, and the right column
 * (search, replies, the revision list), which CSS hides below 1018 px. On a phone (the phone artboards):
 * the home header on Home, the page, the compose button on Home, and the bottom navigation capsule.
 */
@Composable
fun WebShell(
    selection: RailSelection,
    me: TabsComponent.Me,
    actions: RailActions,
    width: ShellWidth = ShellWidth.Column,
    right: (@Composable () -> Unit)? = null,
    main: @Composable () -> Unit,
) {
    val home = selection == RailSelection.Tab(TabsComponent.Tab.Home)
    if (rememberIsPhone()) {
        Div({ classes("talk", "t-phone-page") }) {
            if (home) MobileHomeHeader(me, actions)
            Main({ classes("t-phone-main") }) { main() }
            // The editor is full screen on a phone, as in Compose · Mobile.
            if (width == ShellWidth.Column) {
                if (home) ComposeFab(actions)
                MobileNav(selection, actions)
            }
        }
        return
    }
    Div({ classes("talk", "t-web") }) {
        Rail(selection, me, actions)
        Main({ classes(if (width == ShellWidth.Wide) "t-wide" else "t-main") }) { main() }
        if (right != null && width == ShellWidth.Column) {
            Aside({ classes("t-right") }) { right() }
        }
    }
}
