package com.yet.talk.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkShapes
import com.yet.talk.ui.theme.talkTypography

/**
 * The design's input (`x-input`): an outlined field in the theme's colours. [onDone] runs on the keyboard's action
 * (Search, Send, Done, depending on [imeAction]); [secret] hides what is typed.
 */
@Composable
fun TalkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    secret: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supporting: String? = null,
    pill: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onDone: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.talkColors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        supportingText = supporting?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        textStyle = MaterialTheme.talkTypography.inputText,
        shape = if (pill) MaterialTheme.talkShapes.pill else MaterialTheme.talkShapes.control,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            imeAction = imeAction,
            keyboardType = if (secret) KeyboardType.Password else keyboardType,
            autoCorrectEnabled = !secret,
        ),
        keyboardActions = KeyboardActions(onAny = { onDone?.invoke() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.inputBorder,
            cursorColor = colors.accent,
            focusedLabelColor = colors.accent,
            unfocusedContainerColor = colors.bg,
            focusedContainerColor = colors.bg,
        ),
    )
}

/** The search field (`x-search`): a pill with the search icon and Clear. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    clearLabel: String,
    onSearch: (() -> Unit)? = null,
) {
    TalkTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        pill = true,
        imeAction = ImeAction.Search,
        leadingIcon = { TalkIcon(TalkIcons.Search, contentDescription = null, tint = MaterialTheme.talkColors.textSecondary) },
        trailingIcon = if (value.isEmpty()) null else { { TalkIconButton(TalkIcons.Close, clearLabel, { onValueChange("") }) } },
        onDone = onSearch,
    )
}
