package com.yet.talk.web.ui.screens.editprofile.component

import androidx.compose.runtime.Composable
import com.yet.talk.feature.editprofile.EditProfileComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Text

/** The sticky bar: Cancel leaves without saving, Save publishes the profile. */
@Composable
fun EditProfileBar(model: EditProfileComponent.Model, onCancel: () -> Unit, onSave: () -> Unit) {
    Header({ classes("t-bar", "t-bar-sticky") }) {
        TalkButton(stringResource(MR.strings.action_cancel), onCancel, ButtonStyle.Secondary)
        H1({ classes("t-bar-title") }) { Text(stringResource(MR.strings.action_edit_profile)) }
        TalkButton(stringResource(MR.strings.action_save), onSave, enabled = model.canSave)
    }
}
