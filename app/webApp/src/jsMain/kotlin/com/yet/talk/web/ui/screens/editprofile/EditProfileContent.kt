package com.yet.talk.web.ui.screens.editprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.domain.rule.ProfileImage
import com.yet.talk.feature.editprofile.EditProfileComponent
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.screens.editprofile.component.EditProfileBar
import com.yet.talk.web.ui.screens.editprofile.component.GitHubSection
import com.yet.talk.web.ui.screens.editprofile.component.ImageFields
import com.yet.talk.web.ui.screens.editprofile.component.ProfileFields
import com.yet.talk.web.ui.screens.editprofile.dialog.ImageLinkDialog
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

/**
 * The profile editor (`ProfileEditor`): Cancel · Edit profile · Save, the banner and avatar (each opens
 * [ImageLinkDialog]), then Name, Display name, About (/ 160), Website, NIP-05 with Verified, and Link GitHub.
 */
@Composable
fun EditProfileContent(component: EditProfileComponent) {
    val model by component.model.subscribeAsState()
    var linkFor by remember { mutableStateOf<ProfileImage?>(null) }

    Div({ classes("t-part", "t-editor") }) {
        EditProfileBar(model, onCancel = component::onCancel, onSave = component::onSave)
        ImageFields(model, onPick = { linkFor = it })
        Div({ classes("t-caption", "t-edit-caption") }) { Text(stringResource(MR.strings.edit_profile_images)) }
        model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
        ProfileFields(model, enabled = !model.isLoading && !model.isSaving, onChange = component::onFieldChanged)
        if (!model.isLoading) GitHubSection(model.github, component)
    }
    linkFor?.let { kind ->
        ImageLinkDialog(
            kind = kind,
            onSubmit = { component.onImageLinkSubmitted(kind, it); linkFor = null },
            onDismiss = { linkFor = null },
        )
    }
}
