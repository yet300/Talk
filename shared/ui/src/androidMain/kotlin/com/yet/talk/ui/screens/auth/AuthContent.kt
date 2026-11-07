package com.yet.talk.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.auth.AuthComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.theme.message
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkShapes
import com.yet.talk.ui.theme.talkTypography

/**
 * Sign in (`AuthPanel`): paste a private key, or create one and go through "Save your key" first. The key stays
 * on this device (KSafe).
 */
@Composable
fun AuthContent(component: AuthComponent) {
    val model by component.model.subscribeAsState()
    val spacing = MaterialTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.talkColors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg, vertical = spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
            Text(stringResource(MR.strings.app_name), style = MaterialTheme.talkTypography.wordmark, color = MaterialTheme.talkColors.textPrimary)
            val newKey = model.newKey
            if (newKey != null) SaveKey(model, newKey, component) else KeyForm(model, component)
            model.error?.let { Text(it.message(), style = MaterialTheme.talkTypography.body, color = MaterialTheme.talkColors.error) }
        }
    }
}

@Composable
private fun KeyForm(model: AuthComponent.Model, component: AuthComponent) {
    val colors = MaterialTheme.talkColors
    Text(stringResource(MR.strings.auth_title), style = MaterialTheme.talkTypography.headingLarge, color = colors.textPrimary)
    Text(stringResource(MR.strings.auth_subtitle), style = MaterialTheme.talkTypography.body, color = colors.textSecondary)
    OutlinedTextField(
        value = model.key,
        onValueChange = component::onKeyChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(MR.strings.auth_key_label)) },
        placeholder = { Text(stringResource(MR.strings.auth_key_placeholder)) },
        supportingText = { Text(stringResource(MR.strings.auth_key_caption)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onGo = { if (model.canImport) component.onImportKey() }),
        shape = MaterialTheme.talkShapes.control,
    )
    TalkButton(stringResource(MR.strings.auth_use_key), component::onImportKey, ButtonStyle.Post, enabled = model.canImport && !model.isWorking, fullWidth = true)
    TalkButton(stringResource(MR.strings.auth_create_key), component::onCreateKey, ButtonStyle.Secondary, enabled = !model.isWorking, fullWidth = true)
}

/** "Save your key": the new `nsec` once, Copy, and "I saved it". */
@Composable
private fun SaveKey(model: AuthComponent.Model, newKey: String, component: AuthComponent) {
    val colors = MaterialTheme.talkColors
    val clipboard = LocalClipboardManager.current
    Text(stringResource(MR.strings.auth_save_title), style = MaterialTheme.talkTypography.headingLarge, color = colors.textPrimary)
    Text(stringResource(MR.strings.auth_save_warning), style = MaterialTheme.talkTypography.body, color = colors.textSecondary)
    Text(
        newKey,
        style = MaterialTheme.talkTypography.code,
        color = colors.textPrimary,
        modifier = Modifier.fillMaxWidth().background(colors.surface, MaterialTheme.talkShapes.control).padding(MaterialTheme.spacing.md),
    )
    TalkButton(
        stringResource(if (model.newKeyCopied) MR.strings.action_copied else MR.strings.action_copy),
        {
            clipboard.setText(AnnotatedString(newKey))
            component.onNewKeyCopied()
        },
        ButtonStyle.Secondary,
        fullWidth = true,
    )
    TalkButton(stringResource(MR.strings.auth_saved_it), component::onNewKeySaved, ButtonStyle.Post, enabled = model.canConfirmSaved && !model.isWorking, fullWidth = true)
}
