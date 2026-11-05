package com.yet.talk.web.ui.screens.auth.component

import androidx.compose.runtime.Composable
import com.yet.talk.feature.auth.AuthComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.TextField
import com.yet.talk.web.ui.theme.message
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

/** The private key field (hidden text), its caution, Use this key, and Create a new key. */
@Composable
fun KeySection(model: AuthComponent.Model, component: AuthComponent) {
    TextField(
        value = model.key,
        onValueChange = component::onKeyChanged,
        label = stringResource(MR.strings.auth_key_label),
        placeholder = stringResource(MR.strings.auth_key_placeholder),
        id = "nsec",
        secret = true,
        onEnter = component::onImportKey.takeIf { model.canImport },
    )
    model.error?.let { P({ classes("x-alert", "t-flush"); attr("role", "alert") }) { Text(it.message()) } }
    P({ classes("t-caption", "t-flush") }) { Text(stringResource(MR.strings.auth_key_caption)) }
    TalkButton(
        text = stringResource(MR.strings.auth_use_key),
        onClick = component::onImportKey,
        style = ButtonStyle.Post,
        enabled = model.canImport && !model.isWorking,
    )
    TalkButton(
        text = stringResource(MR.strings.auth_create_key),
        onClick = component::onCreateKey,
        style = ButtonStyle.Secondary,
        enabled = !model.isWorking,
    )
}
