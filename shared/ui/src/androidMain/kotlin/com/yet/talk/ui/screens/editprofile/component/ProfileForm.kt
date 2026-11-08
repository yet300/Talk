package com.yet.talk.ui.screens.editprofile.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yet.talk.domain.rule.ProfileImage
import com.yet.talk.feature.editprofile.EditProfileComponent
import com.yet.talk.feature.editprofile.EditProfileComponent.Field
import com.yet.talk.feature.editprofile.EditProfileComponent.GitHub
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.TalkTextField
import com.yet.talk.ui.platform.copyText
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/** The banner (3:1) and the avatar over its edge: the current image under the image icon; each opens the link dialog. */
@Composable
fun ImageFields(model: EditProfileComponent.Model, onPick: (ProfileImage) -> Unit) {
    val colors = MaterialTheme.talkColors
    val banner = stringResource(MR.strings.edit_profile_change_banner)
    val avatar = stringResource(MR.strings.edit_profile_change_avatar)
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(3f).background(colors.surface).clickable(role = Role.Button) { onPick(ProfileImage.Banner) }.semantics { contentDescription = banner },
            contentAlignment = Alignment.Center,
        ) {
            model.banner?.let { AsyncImage(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            EditIcon()
        }
        Box(
            Modifier.padding(start = MaterialTheme.spacing.lg).offset(y = 60.dp).align(Alignment.BottomStart).size(88.dp).clip(CircleShape)
                .border(4.dp, colors.bg, CircleShape).background(colors.elevated)
                .clickable(role = Role.Button) { onPick(ProfileImage.Avatar) }.semantics { contentDescription = avatar },
            contentAlignment = Alignment.Center,
        ) {
            model.avatar?.let { AsyncImage(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            EditIcon()
        }
    }
}

@Composable
private fun EditIcon() {
    Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.talkColors.mediaScrim), contentAlignment = Alignment.Center) {
        TalkIcon(TalkIcons.Image, null, size = 20.dp, tint = Color.White)
    }
}

/** Name, Display name, About with its counter, Website, NIP-05 and Verified once it resolves to this key. */
@Composable
fun ProfileFields(model: EditProfileComponent.Model, enabled: Boolean, onChange: (Field, String) -> Unit) {
    Column(Modifier.padding(horizontal = MaterialTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        TalkTextField(model.name, { onChange(Field.Name, it) }, label = stringResource(MR.strings.edit_profile_name), enabled = enabled, imeAction = ImeAction.Next)
        TalkTextField(model.displayName, { onChange(Field.DisplayName, it) }, label = stringResource(MR.strings.edit_profile_display_name), enabled = enabled, imeAction = ImeAction.Next)
        TalkTextField(
            model.about, { onChange(Field.About, it) },
            label = stringResource(MR.strings.edit_profile_about), enabled = enabled, singleLine = false, minLines = 3,
            isError = model.aboutLength > model.aboutMax,
            supporting = stringResource(MR.strings.compose_length, model.aboutLength, model.aboutMax),
            imeAction = ImeAction.Default,
        )
        TalkTextField(model.website, { onChange(Field.Website, it) }, label = stringResource(MR.strings.edit_profile_website), enabled = enabled, keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next)
        TalkTextField(
            model.nip05, { onChange(Field.Nip05, it) },
            label = stringResource(MR.strings.edit_profile_nip05), placeholder = stringResource(MR.strings.edit_profile_nip05_placeholder),
            enabled = enabled, keyboardType = KeyboardType.Email,
        )
        AnimatedVisibility(model.nip05Status == EditProfileComponent.Nip05Status.Verified) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TalkIcon(TalkIcons.Check, null, size = 18.dp, tint = MaterialTheme.talkColors.accent)
                Text(stringResource(MR.strings.edit_profile_verified), style = MaterialTheme.talkTypography.small, color = MaterialTheme.talkColors.textSecondary)
            }
        }
    }
}

/**
 * Link GitHub (`GitHubLink`, NIP-39): the username and Link GitHub; then the proof text to put in a public gist, Copy,
 * the gist link and Verify; then the verified login, with Use GitHub avatar (decision 14).
 */
@Composable
fun GitHubSection(github: GitHub, component: EditProfileComponent) {
    val context = LocalContext.current
    val copied = stringResource(MR.strings.action_copied)
    val colors = MaterialTheme.talkColors
    Column(Modifier.padding(MaterialTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TalkIcon(TalkIcons.GitHub, null, size = 18.dp, tint = colors.textPrimary)
            Text(stringResource(MR.strings.github_title), style = MaterialTheme.talkTypography.bodyBold, color = colors.textPrimary)
        }
        AnimatedContent(github::class, label = "github") { _ ->
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                when (github) {
                    is GitHub.NotLinked -> {
                        TalkTextField(github.login, component::onGitHubLoginChanged, label = stringResource(MR.strings.github_username), placeholder = stringResource(MR.strings.github_username_placeholder), onDone = component::onLinkGitHub)
                        TalkButton(stringResource(MR.strings.github_link), component::onLinkGitHub, ButtonStyle.Secondary, enabled = github.login.isNotBlank())
                    }
                    is GitHub.Waiting -> {
                        Text(stringResource(MR.strings.github_gist_hint), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
                        Text(github.proofText, style = MaterialTheme.talkTypography.code, color = colors.textPrimary, modifier = Modifier.fillMaxWidth().background(colors.surface, MaterialTheme.shapes.small).padding(MaterialTheme.spacing.md))
                        TalkButton(stringResource(MR.strings.action_copy), { copyText(context, github.proofText, copied) }, ButtonStyle.Secondary)
                        if (github.failed) Text(stringResource(MR.strings.github_proof_missing), style = MaterialTheme.talkTypography.body, color = colors.error)
                        TalkTextField(github.gistLink, component::onGistLinkChanged, label = stringResource(MR.strings.github_gist_link), placeholder = stringResource(MR.strings.github_gist_placeholder), keyboardType = KeyboardType.Uri, onDone = component::onVerifyGitHub)
                        TalkButton(
                            stringResource(if (github.isChecking) MR.strings.github_verifying else MR.strings.github_verify),
                            component::onVerifyGitHub, ButtonStyle.Primary, enabled = github.gistLink.isNotBlank() && !github.isChecking,
                        )
                    }
                    is GitHub.Linked -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(github.login, style = MaterialTheme.talkTypography.bodyBold, color = colors.textPrimary)
                            TalkIcon(TalkIcons.Check, null, size = 18.dp, tint = colors.accent)
                            Text(stringResource(MR.strings.github_verified), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
                        }
                        if (!github.usesAvatar) TalkButton(stringResource(MR.strings.github_use_avatar), component::onUseGitHubAvatar, ButtonStyle.Secondary)
                    }
                }
            }
        }
    }
}
