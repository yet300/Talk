package com.yet.talk.web.ui.screens.compose.component

import androidx.compose.runtime.Composable
import com.yet.talk.feature.compose.ComposeComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.markdown.Markdown
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea

/** Edit (the monospace body), Preview (rendered), or Split (both, side by side). */
@Composable
fun EditorPanes(model: ComposeComponent.Model, onTextChanged: (String) -> Unit) {
    val split = model.view == ComposeComponent.View.Split
    val placeholderText = stringResource(MR.strings.compose_placeholder)
    Div({ classes("t-split"); if (split) classes("t-split-two") }) {
        if (model.view != ComposeComponent.View.Preview) {
            Div({ classes("t-split-col") }) {
                Label(forId = "draft-body", attrs = { classes("t-sr") }) { Text(stringResource(MR.strings.compose_body)) }
                TextArea(model.text) {
                    id("draft-body")
                    classes("t-body-input")
                    placeholder(placeholderText)
                    attr("spellcheck", "true")
                    onInput { onTextChanged(it.value) }
                }
            }
        }
        if (model.view != ComposeComponent.View.Edit) {
            Div({ classes("t-split-col") }) {
                Div({ classes("t-preview") }) { Markdown(model.text) }
            }
        }
    }
}
