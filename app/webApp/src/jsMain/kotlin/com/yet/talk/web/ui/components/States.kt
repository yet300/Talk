package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.resources.*
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.message
import com.yet.talk.web.ui.theme.stringResource
import dev.icerock.moko.resources.ImageResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** The design's empty state (`x-empty`): optional 48 px icon, title, body, one action. */
@Composable
fun EmptyState(
    title: String,
    body: String? = null,
    icon: ImageResource? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Div({ classes("x-empty", "t-empty") }) {
        if (icon != null) Span({ classes("t-muted") }) { TalkIcon(icon, size = 48f) }
        H2({ classes("t-h1") }) { Text(title) }
        if (body != null) P({ classes("t-muted", "t-flush") }) { Text(body) }
        action?.invoke()
    }
}

/** Four skeleton cards: a feed's first load. */
@Composable
fun SkeletonCards(count: Int = 4) {
    repeat(count) {
        Div({ classes("x-post"); attr("aria-hidden", "true") }) {
            Div({ classes("x-skel", "t-skel-avatar") })
            Div({ classes("t-skel-lines") }) {
                Div({ classes("x-skel", "t-skel-40") })
                Div({ classes("x-skel", "t-skel-title") })
                Div({ classes("x-skel", "t-skel-100") })
                Div({ classes("x-skel", "t-skel-92") })
            }
        }
    }
}

/** An error the user can dismiss, as the design's alert row (`t-alert-row`, error text in red). */
@Composable
fun ErrorRow(error: DomainError, onDismiss: (() -> Unit)?, onRetry: (() -> Unit)? = null) {
    Div({ classes("t-alert-row"); attr("role", "alert") }) {
        Span({ classes("x-alert", "t-grow") }) { Text(error.message()) }
        if (onRetry != null) TalkButton(stringResource(MR.strings.action_retry), onRetry, ButtonStyle.Secondary)
        if (onDismiss != null) IconButton(com.yet.talk.web.ui.theme.TalkIcons.Close, stringResource(MR.strings.action_dismiss), onDismiss, size = 18f)
    }
}

/** A status line above a list (`t-notice`), e.g. offline. */
@Composable
fun Notice(icon: ImageResource, text: String) {
    Div({ classes("t-notice"); attr("role", "status") }) {
        TalkIcon(icon, size = 18f)
        Span { Text(text) }
    }
}
