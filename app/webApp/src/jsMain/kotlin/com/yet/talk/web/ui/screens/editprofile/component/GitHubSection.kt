package com.yet.talk.web.ui.screens.editprofile.component

import androidx.compose.runtime.Composable
import com.yet.talk.feature.editprofile.EditProfileComponent
import com.yet.talk.feature.editprofile.EditProfileComponent.GitHub
import com.yet.talk.resources.*
import com.yet.talk.web.browser.copyToClipboard
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.LocalToaster
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.TextField
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Link GitHub (`GitHubLink`, NIP-39): the username and Link GitHub; then the proof text to put in a public
 * gist, Copy, the gist link and Verify ("The gist doesn't contain the text" when it fails); then the verified
 * login, with Use GitHub avatar (decision 13: the only way to set a GitHub avatar).
 */
@Composable
fun GitHubSection(github: GitHub, component: EditProfileComponent) {
    Div({ classes("t-gh-link") }) {
        Div({ classes("t-inline", "x-name") }) {
            TalkIcon(TalkIcons.GitHub, size = 18f)
            Text(stringResource(MR.strings.github_title))
        }
        when (github) {
            is GitHub.NotLinked -> {
                TextField(
                    value = github.login,
                    onValueChange = component::onGitHubLoginChanged,
                    label = stringResource(MR.strings.github_username),
                    placeholder = stringResource(MR.strings.github_username_placeholder),
                    id = "github-login",
                    onEnter = component::onLinkGitHub,
                )
                TalkButton(stringResource(MR.strings.github_link), component::onLinkGitHub, ButtonStyle.Secondary, enabled = github.login.isNotBlank(), attrs = { classes("t-start") })
            }
            is GitHub.Waiting -> {
                val toaster = LocalToaster.current
                val copied = stringResource(MR.strings.action_copied)
                P({ classes("t-caption", "t-flush") }) { Text(stringResource(MR.strings.github_gist_hint)) }
                Div({ classes("t-mono-box") }) { Text(github.proofText) }
                TalkButton(stringResource(MR.strings.action_copy), { copyToClipboard(github.proofText); toaster.show(copied) }, ButtonStyle.Secondary, attrs = { classes("t-start") })
                if (github.failed) P({ classes("x-alert", "t-flush"); attr("role", "alert") }) { Text(stringResource(MR.strings.github_proof_missing)) }
                TextField(
                    value = github.gistLink,
                    onValueChange = component::onGistLinkChanged,
                    label = stringResource(MR.strings.github_gist_link),
                    placeholder = stringResource(MR.strings.github_gist_placeholder),
                    id = "github-gist",
                    onEnter = component::onVerifyGitHub,
                )
                TalkButton(
                    stringResource(if (github.isChecking) MR.strings.github_verifying else MR.strings.github_verify),
                    component::onVerifyGitHub,
                    enabled = github.gistLink.isNotBlank() && !github.isChecking,
                    attrs = { classes("t-start") },
                )
            }
            is GitHub.Linked -> {
                Div({ classes("t-gh") }) {
                    Text(github.login)
                    TalkIcon(TalkIcons.Check, size = 18f)
                    Span({ classes("t-caption", "t-normal") }) { Text(stringResource(MR.strings.github_verified)) }
                }
                if (!github.usesAvatar) {
                    TalkButton(stringResource(MR.strings.github_use_avatar), component::onUseGitHubAvatar, ButtonStyle.Secondary, attrs = { classes("t-start") })
                }
            }
        }
    }
}
