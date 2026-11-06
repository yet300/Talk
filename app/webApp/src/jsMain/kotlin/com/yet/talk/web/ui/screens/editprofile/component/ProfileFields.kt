package com.yet.talk.web.ui.screens.editprofile.component

import androidx.compose.runtime.Composable
import com.yet.talk.feature.editprofile.EditProfileComponent
import com.yet.talk.feature.editprofile.EditProfileComponent.Field
import com.yet.talk.resources.*
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea

/** Name, Display name, About with its counter, Website, NIP-05 and Verified once it resolves to this key. */
@Composable
fun ProfileFields(model: EditProfileComponent.Model, enabled: Boolean, onChange: (Field, String) -> Unit) {
    Div({ classes("t-edit-fields") }) {
        Line(stringResource(MR.strings.edit_profile_name), model.name, enabled) { onChange(Field.Name, it) }
        Line(stringResource(MR.strings.edit_profile_display_name), model.displayName, enabled) { onChange(Field.DisplayName, it) }
        Label(attrs = { classes("t-field") }) {
            Text(stringResource(MR.strings.edit_profile_about))
            TextArea(model.about) {
                classes("x-input")
                attr("rows", "3")
                if (!enabled) disabled()
                onInput { onChange(Field.About, it.value) }
            }
            Span({ if (model.aboutLength > model.aboutMax) classes("t-over") }) {
                Text(stringResource(MR.strings.compose_length, model.aboutLength, model.aboutMax))
            }
        }
        Line(stringResource(MR.strings.edit_profile_website), model.website, enabled) { onChange(Field.Website, it) }
        Line(stringResource(MR.strings.edit_profile_nip05), model.nip05, enabled, stringResource(MR.strings.edit_profile_nip05_placeholder)) {
            onChange(Field.Nip05, it)
        }
        if (model.nip05Status == EditProfileComponent.Nip05Status.Verified) {
            Div({ classes("t-inline", "t-caption") }) {
                TalkIcon(TalkIcons.Check, size = 18f)
                Text(stringResource(MR.strings.edit_profile_verified))
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String, enabled: Boolean, placeholder: String? = null, onChange: (String) -> Unit) {
    Label(attrs = { classes("t-field") }) {
        Text(label)
        Input(InputType.Text) {
            classes("x-input")
            value(value)
            placeholder?.let { placeholder(it) }
            attr("spellcheck", "false")
            if (!enabled) disabled()
            onInput { onChange(it.value) }
        }
    }
}
