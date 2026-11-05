package com.yet.talk.web.ui.screens.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.yet.talk.feature.compose.ComposeComponent
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.screens.compose.component.EditorBar
import com.yet.talk.web.ui.screens.compose.component.EditorPanes
import com.yet.talk.web.ui.screens.compose.component.ImageStrip
import com.yet.talk.web.ui.screens.compose.component.TargetPreview
import org.jetbrains.compose.web.dom.Div

/**
 * The editor (`Compose · Web`): the bar (Close, Edit · Split · Preview, Publish), the post it replies to,
 * quotes or edits, attached images, then the markdown body and its preview. No title or summary fields:
 * a post's title is its first heading.
 */
@Composable
fun ComposeContent(component: ComposeComponent) {
    val model by component.model.subscribeAsState()
    Div({ classes("t-part", "t-composer", "t-composer-page") }) {
        EditorBar(model, component)
        model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown, onRetry = component::onPublish.takeIf { model.canPublish }) }
        model.target?.let { TargetPreview(model.mode, it) }
        if (model.images.isNotEmpty()) ImageStrip(model.images, component::onRemoveImage)
        EditorPanes(model, component::onTextChanged)
    }
}
