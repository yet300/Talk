package com.yet.talk.web.ui.screens.tab.chats.component

import androidx.compose.runtime.Composable
import com.yet.talk.resources.*
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** The Location channels entry at the top of the Chats list (`ChatList`). */
@Composable
fun ChannelsRow(selected: Boolean, onClick: () -> Unit) {
    A(href = "#", attrs = {
        classes("t-row")
        if (selected) classes("t-selected")
        onClick { it.preventDefault(); onClick() }
    }) {
        Span({ classes("x-avatar", "t-channel-icon") }) { TalkIcon(TalkIcons.Pin, size = 18f) }
        Span({ classes("t-row-text") }) {
            Span({ classes("x-name", "t-block") }) { Text(stringResource(MR.strings.channels_title)) }
            Span({ classes("t-caption", "t-block") }) { Text(stringResource(MR.strings.channels_row_caption)) }
        }
        Span({ classes("t-muted") }) { TalkIcon(TalkIcons.Chevron, size = 18f) }
    }
}
