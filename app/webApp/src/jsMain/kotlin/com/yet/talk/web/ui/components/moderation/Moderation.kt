package com.yet.talk.web.ui.components.moderation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
import com.yet.talk.domain.model.moderation.Moderation
import com.yet.talk.domain.model.moderation.HiddenReason
import com.yet.talk.domain.model.moderation.ReportReason
import com.yet.talk.domain.model.moderation.ReportTarget
import com.yet.talk.feature.root.moderation.ModerationComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.LocalToaster
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** The viewer's network and hidden posts, read by avatars, banners and post cards; provided by the root content. */
val LocalModeration = compositionLocalOf { Moderation() }

/** Report; provided by the root content. */
val LocalModerationActions = staticCompositionLocalOf<ModerationComponent?> { null }

/**
 * Report (`MoreFlow`, report step): the note that reports are public, then Nudity, Spam, Illegal, Impersonation
 * and Other. Picking one reports and shows "Reported. Your report is public."
 */
@Composable
fun ReportMenu(target: ReportTarget, onDismiss: () -> Unit) {
    val actions = LocalModerationActions.current
    val toaster = LocalToaster.current
    val reported = stringResource(MR.strings.toast_reported)
    val title = stringResource(MR.strings.report_title)
    Div({ classes("t-menu-backdrop"); onClick { it.stopPropagation(); onDismiss() } })
    Div({ classes("t-menu-anchor") }) {
        Div({ classes("x-menu"); attr("role", "menu"); attr("aria-label", title) }) {
            P({ classes("t-muted", "t-menu-note") }) { Text(stringResource(MR.strings.report_note)) }
            ReportReason.entries.forEach { reason ->
                val label = stringResource(reason.label)
                Button({
                    classes("t-menu-row")
                    type(ButtonType.Button)
                    attr("role", "menuitem")
                    onClick {
                        it.stopPropagation()
                        onDismiss()
                        actions?.report(target, reason)
                        toaster.show(reported)
                    }
                }) {
                    TalkIcon(if (reason == ReportReason.Nudity) TalkIcons.EyeOff else TalkIcons.Report, size = 18f)
                    Text(label)
                }
            }
        }
    }
}

/** A post the viewer's network reported (`PostCard`, hidden): the row, with Show to reveal it. */
@Composable
fun HiddenRow(reason: HiddenReason, onShow: () -> Unit) {
    Div({ classes("t-part", "t-hidden-row"); onClick { it.stopPropagation() } }) {
        TalkIcon(TalkIcons.EyeOff, size = 18f)
        Span({ classes("t-grow") }) { Text(stringResource(if (reason == HiddenReason.Blocked) MR.strings.moderation_hidden_blocked else MR.strings.moderation_hidden)) }
        Button({
            classes("t-text-btn")
            type(ButtonType.Button)
            onClick { it.stopPropagation(); onShow() }
        }) { Text(stringResource(MR.strings.action_show)) }
    }
}

private val ReportReason.label
    get() = when (this) {
        ReportReason.Nudity -> MR.strings.report_nudity
        ReportReason.Spam -> MR.strings.report_spam
        ReportReason.Illegal -> MR.strings.report_illegal
        ReportReason.Impersonation -> MR.strings.report_impersonation
        ReportReason.Other -> MR.strings.report_other
    }
