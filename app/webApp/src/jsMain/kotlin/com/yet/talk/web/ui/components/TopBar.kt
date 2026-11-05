package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import com.yet.talk.resources.*
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** The 53 px sticky bar over a column (`t-bar`): optional Back, the title, then [trailing] actions. */
@Composable
fun TopBar(
    title: String?,
    onBack: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Header({ classes("t-bar", "t-bar-sticky") }) {
        if (onBack != null) IconButton(TalkIcons.Back, stringResource(MR.strings.action_back), onBack)
        if (title != null) {
            H1({ classes("t-bar-title") }) { Text(title) }
        } else {
            Span({ classes("t-spacer") })
        }
        trailing()
    }
}
