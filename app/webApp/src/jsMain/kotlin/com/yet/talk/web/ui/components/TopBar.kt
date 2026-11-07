package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import com.yet.talk.resources.*
import com.yet.talk.web.browser.trackScroll
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * The 53 px sticky glass bar over a column (`t-bar`, design pass 4): optional Back, the title, then [trailing]
 * actions in round glass buttons. Content scrolls under it; the scroll-edge shadow shows once it does. [clear]
 * (Profile): transparent over the banner, turning to glass with the title once the banner has scrolled away.
 */
@Composable
fun TopBar(
    title: String?,
    onBack: (() -> Unit)? = null,
    clear: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    Header({
        classes("t-bar", "t-bar-sticky")
        if (clear) classes("t-bar-clear")
        ref { bar -> val stop = trackScroll(bar); onDispose { stop() } }
    }) {
        if (onBack != null) IconButton(TalkIcons.Back, stringResource(MR.strings.action_back), onBack)
        if (title != null) {
            H1({ classes("t-bar-title") }) { Text(title) }
        } else {
            Span({ classes("t-spacer") })
        }
        trailing()
    }
}
