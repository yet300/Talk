package com.yet.talk.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yet.talk.domain.model.network.NetworkStatus
import com.yet.talk.resources.*
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.pluralResource
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/** The network status (offline, relays, what waits to be sent); provided by the root content. */
val LocalNetwork = compositionLocalOf { NetworkStatus() }

/** Whether the [NetworkBar] shows: offline, or something still waits to be sent. */
val NetworkStatus.showsBar: Boolean get() = isOffline || waitingToSend > 0

/** The height the [NetworkBar] adds under a top bar while it shows (fixed, so lists pad for it without measuring). */
val networkBarHeight: Dp
    @Composable @ReadOnlyComposable get() = if (LocalNetwork.current.showsBar) NetworkBarHeight else 0.dp

/**
 * The offline indicator (`t-network-bar`), under every top bar: offline (the device, or no relay reachable) with what
 * still waits to be sent; online, only the waiting count when something does. It slides open and closed.
 */
@Composable
fun NetworkBar(modifier: Modifier = Modifier) {
    val status = LocalNetwork.current
    AnimatedVisibility(status.showsBar, modifier, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        val colors = MaterialTheme.talkColors
        val parts = buildList {
            if (status.isOffline) add(stringResource(if (status.deviceOnline) MR.strings.network_unreachable else MR.strings.network_offline))
            if (status.waitingToSend > 0) add(pluralResource(MR.plurals.network_waiting, status.waitingToSend))
        }
        Row(
            Modifier.fillMaxWidth().height(NetworkBarHeight).padding(horizontal = MaterialTheme.spacing.lg)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            TalkIcon(if (status.isOffline) TalkIcons.Offline else TalkIcons.Clock, contentDescription = null, size = 16.dp, tint = if (status.isOffline) colors.warning else colors.textSecondary)
            Text(parts.joinToString(" · "), style = MaterialTheme.talkTypography.small, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val NetworkBarHeight = 32.dp
