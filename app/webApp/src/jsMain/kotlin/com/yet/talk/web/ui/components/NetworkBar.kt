package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.yet.talk.domain.model.network.NetworkStatus
import com.yet.talk.resources.*
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.pluralResource
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** The network status, provided by the root content. */
val LocalNetwork = compositionLocalOf { NetworkStatus() }

/**
 * The offline indicator (`t-network-bar`): offline (the device, or every relay unreachable) with what still waits
 * to be sent; while online, only the waiting count when something does.
 */
@Composable
fun NetworkBar() {
    val status = LocalNetwork.current
    val waiting = status.waitingToSend
    if (!status.isOffline && waiting == 0) return
    Div({
        classes("t-network-bar")
        if (status.isOffline) classes("t-network-offline")
        attr("role", "status")
    }) {
        TalkIcon(if (status.isOffline) TalkIcons.Offline else TalkIcons.Clock, size = 18f)
        Span({ classes("t-grow") }) {
            val parts = buildList {
                if (status.isOffline) add(stringResource(if (status.deviceOnline) MR.strings.network_unreachable else MR.strings.network_offline))
                if (waiting > 0) add(pluralResource(MR.plurals.network_waiting, waiting))
            }
            Text(parts.joinToString(" · "))
        }
    }
}
