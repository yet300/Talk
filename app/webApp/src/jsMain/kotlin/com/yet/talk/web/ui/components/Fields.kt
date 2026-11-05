package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.autoComplete
import org.jetbrains.compose.web.attributes.AutoComplete
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Text

/** The design's search field (`x-search`, pill on search-bg) with the search icon inside. */
@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, id: String, onFocus: (() -> Unit)? = null) {
    Div({ classes("x-search-wrap") }) {
        Label(forId = id, attrs = { classes("t-sr") }) { Text(placeholder) }
        Span18Icon()
        Input(InputType.Search) {
            id(id)
            classes("x-search")
            placeholder(placeholder)
            value(value)
            onInput { onValueChange(it.value) }
            if (onFocus != null) onFocus { onFocus() }
        }
    }
}

@Composable
private fun Span18Icon() {
    TalkIcon(TalkIcons.Search, size = 18f, attrs = { classes("x-icon", "t-search-icon") })
}

/** A labelled text field (`t-field` + `x-input`); [secret] hides the text (private keys). */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    id: String,
    secret: Boolean = false,
    visibleLabel: Boolean = true,
    onEnter: (() -> Unit)? = null,
) {
    Label(forId = id, attrs = { classes(if (visibleLabel) "t-field" else "t-sr") }) {
        Text(label)
        if (visibleLabel) FieldInput(value, onValueChange, placeholder, id, secret, onEnter)
    }
    if (!visibleLabel) FieldInput(value, onValueChange, placeholder, id, secret, onEnter)
}

@Composable
private fun FieldInput(value: String, onValueChange: (String) -> Unit, placeholder: String, id: String, secret: Boolean, onEnter: (() -> Unit)?) {
    Input(if (secret) InputType.Password else InputType.Text) {
        id(id)
        classes("x-input", "t-input-compact")
        placeholder(placeholder)
        value(value)
        if (secret) autoComplete(AutoComplete.off)
        attr("spellcheck", "false")
        onInput { onValueChange(it.value) }
        if (onEnter != null) onKeyDown { if (it.key == "Enter") onEnter() }
    }
}
