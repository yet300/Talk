package com.yet.talk.ui.screens.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.chat.component.verify.VerifyComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.glass.BackBar
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.components.qr.QrCode
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Verify a contact (`VerifyContact`, `Verify-Mobile`): the safety code (four groups of five digits, the same on both
 * devices) and its QR code to show the other person, then Mark as verified; once verified, the date and Unverify.
 * Android shows the code to be scanned; scanning theirs is the web's (decision: no camera here).
 */
@Composable
fun VerifyContent(component: VerifyComponent) {
    val model by component.model.subscribeAsState()
    val name = model.person.displayLabel
    val colors = MaterialTheme.talkColors
    GlassPage(bar = { BackBar(stringResource(MR.strings.verify_title, name), onBack = component::onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(MaterialTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
        ) {
            ErrorRow(model.error, onDismiss = component::onErrorShown)
            Text(stringResource(MR.strings.verify_body, name), style = MaterialTheme.talkTypography.body, color = colors.textPrimary)
            val code = model.code
            if (code != null) {
                val codeLabel = stringResource(MR.strings.verify_code)
                FlowRow(
                    Modifier.semantics(mergeDescendants = true) { contentDescription = "$codeLabel: ${code.groups.joinToString(" ")}" },
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md, Alignment.CenterHorizontally),
                ) {
                    code.groups.forEach { Text(it, style = MaterialTheme.talkTypography.code.copy(fontSize = MaterialTheme.talkTypography.headingMedium.fontSize), color = colors.textPrimary) }
                }
                QrCode(code.qrPayload, stringResource(MR.strings.verify_qr))
            }
            if (model.scanMismatch) Text(stringResource(MR.strings.verify_mismatch), style = MaterialTheme.talkTypography.body, color = colors.error)
            AnimatedContent(model.verifiedAt, label = "verified") { verifiedAt ->
                if (verifiedAt == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                        TalkButton(stringResource(MR.strings.verify_mark), component::onMarkVerified, ButtonStyle.Post, enabled = code != null, fullWidth = true)
                        Text(stringResource(MR.strings.verify_note), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
                    }
                } else {
                    val date = verifiedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surface).padding(MaterialTheme.spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                        ) {
                            TalkIcon(TalkIcons.Shield, null, tint = colors.accent)
                            Column {
                                Text(stringResource(MR.strings.chat_verified), style = MaterialTheme.talkTypography.bodyBold, color = colors.textPrimary)
                                Text(stringResource(MR.strings.verify_verified_on, name, date.toString()), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
                            }
                        }
                        TalkButton(stringResource(MR.strings.verify_unverify), component::onUnverify, ButtonStyle.Secondary)
                    }
                }
            }
        }
    }
}
