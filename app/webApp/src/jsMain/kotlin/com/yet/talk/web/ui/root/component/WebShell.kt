package com.yet.talk.web.ui.root.component

import androidx.compose.runtime.Composable
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import org.jetbrains.compose.web.dom.Aside
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Main

/** How wide the content is: the 600 px column with the right column, or the wide editor column. */
enum class ShellWidth { Column, Wide }

/**
 * The signed-in page (`t-web`): the rail, the main column, and the right column (search, replies, the
 * revision list). CSS hides the right column below 1018 px.
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
    Div({ classes("talk", "t-web") }) {
        Rail(selection, me, actions)
        Main({ classes(if (width == ShellWidth.Wide) "t-wide" else "t-main") }) { main() }
        if (right != null && width == ShellWidth.Column) {
            Aside({ classes("t-right") }) { right() }
        }
    }
}
