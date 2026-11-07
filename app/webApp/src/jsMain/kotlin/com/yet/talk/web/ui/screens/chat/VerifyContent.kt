package com.yet.talk.web.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.feature.chat.component.verify.VerifyComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.QrScanner
import com.yet.talk.web.browser.isQrScanSupported
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.QrCode
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Verify a contact (`VerifyContact`): the safety code (four groups of five digits, the same on both devices), its QR
 * code, Scan code where the browser can read one, and Mark as verified.
 */
@Composable
fun VerifyContent(component: VerifyComponent) {
    val model by component.model.subscribeAsState()
    val name = model.person.displayLabel
    var scanning by remember { mutableStateOf(false) }
    TopBar(title = stringResource(MR.strings.verify_title, name), onBack = component::onBack)
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
    Div({ classes("t-verify") }) {
        P({ classes("t-flush") }) { Text(stringResource(MR.strings.verify_body, name)) }
        val code = model.code
        val codeLabel = stringResource(MR.strings.verify_code)
        if (code != null) {
            Div({ classes("t-words", "t-code"); attr("aria-label", codeLabel) }) {
                code.groups.forEach { Span { Text(it) } }
            }
            Div({ classes("t-verify-qr") }) {
                if (scanning) {
                    QrScanner(
                        onResult = { payload ->
                            scanning = false
                            component.onScanned(payload)
                        },
                        onError = { scanning = false },
                    )
                    P({ classes("t-caption", "t-flush") }) { Text(stringResource(MR.strings.verify_scanning)) }
                    TalkButton(stringResource(MR.strings.action_cancel), { scanning = false }, ButtonStyle.Secondary, attrs = { classes("t-btn-44") })
                } else {
                    QrCode(code.qrPayload, stringResource(MR.strings.verify_qr))
                    if (isQrScanSupported()) {
                        Span({ classes("t-caption") }) { Text(stringResource(MR.strings.verify_scan_hint)) }
                        TalkButton(stringResource(MR.strings.verify_scan), { scanning = true }, ButtonStyle.Secondary, attrs = { classes("t-btn-44") })
                    }
                }
            }
        }
        if (model.scanMismatch) {
            Div({ classes("t-alert-row", "t-verify-alert"); attr("role", "alert") }) {
                Span({ classes("x-alert", "t-grow") }) { Text(stringResource(MR.strings.verify_mismatch)) }
            }
        }
        val verifiedAt = model.verifiedAt
        if (verifiedAt == null) {
            TalkButton(stringResource(MR.strings.verify_mark), component::onMarkVerified, ButtonStyle.Post, attrs = { if (code == null) attr("disabled", "") })
            P({ classes("t-caption", "t-flush") }) { Text(stringResource(MR.strings.verify_note)) }
        } else {
            val day = verifiedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val date = kotlin.js.Date(day.year, day.month.ordinal, day.day).toLocaleDateString()
            Div({ classes("t-notice", "t-verified-box"); attr("role", "status") }) {
                Span({ classes("t-on-blue-plain") }) { TalkIcon(TalkIcons.Shield) }
                Span {
                    Span({ classes("t-strong", "t-block") }) { Text(stringResource(MR.strings.chat_verified)) }
                    Span({ classes("t-menu-sub") }) { Text(stringResource(MR.strings.verify_verified_on, name, date)) }
                }
            }
            TalkButton(stringResource(MR.strings.verify_unverify), component::onUnverify, ButtonStyle.Secondary, attrs = { classes("t-btn-44") })
        }
    }
}
