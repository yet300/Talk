package com.yet.talk.ui.screens.editprofile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.domain.rule.ProfileImage
import com.yet.talk.feature.editprofile.EditProfileComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.Caption
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.SkeletonCards
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.components.glass.GlassTopBar
import com.yet.talk.ui.screens.editprofile.component.GitHubSection
import com.yet.talk.ui.screens.editprofile.component.ImageFields
import com.yet.talk.ui.screens.editprofile.component.ProfileFields
import com.yet.talk.ui.screens.editprofile.dialog.ImageLinkDialog
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * The profile editor (`EditProfile-Mobile`): Cancel · Edit profile · Save, the banner and avatar (each opens the link
 * dialog, decision 13), then the fields, NIP-05 with Verified, and Link GitHub.
 */
@Composable
fun EditProfileContent(component: EditProfileComponent) {
    val model by component.model.subscribeAsState()
    var linkFor by rememberSaveable { mutableStateOf<ProfileImage?>(null) }
    GlassPage(
        modifier = Modifier.imePadding(),
        bar = {
            GlassTopBar(
                leading = { TalkButton(stringResource(MR.strings.action_cancel), component::onCancel, ButtonStyle.Secondary) },
                title = { Text(stringResource(MR.strings.action_edit_profile), style = MaterialTheme.talkTypography.headingMedium, color = MaterialTheme.talkColors.textPrimary, modifier = Modifier.weight(1f)) },
                trailing = { TalkButton(stringResource(MR.strings.action_save), component::onSave, ButtonStyle.Post, enabled = model.canSave) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding)) {
            ImageFields(model, onPick = { linkFor = it })
            Spacer(Modifier.height(64.dp))
            Caption(stringResource(MR.strings.edit_profile_images))
            ErrorRow(model.error, onDismiss = component::onErrorShown)
            if (model.isLoading) SkeletonCards(2) else {
                ProfileFields(model, enabled = !model.isSaving, onChange = component::onFieldChanged)
                GitHubSection(model.github, component)
            }
            Spacer(Modifier.height(MaterialTheme.spacing.xl))
        }
    }
    linkFor?.let { kind -> ImageLinkDialog(kind, onSubmit = { component.onImageLinkSubmitted(kind, it); linkFor = null }, onDismiss = { linkFor = null }) }
}
