package com.yet.talk.web.ui.screens.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.yet.talk.web.browser.rememberIsPhone
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.feature.compose.ComposeComponent
import com.yet.talk.feature.compose.ComposeMode
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.AddImageButton
import com.yet.talk.web.ui.components.IconButton
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.Tabs
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** Close · Edit/Split/Preview · (counter, add image) · Publish. */
@Composable
fun EditorBar(model: ComposeComponent.Model, component: ComposeComponent) {
    val phone = rememberIsPhone()
    // Compose · Mobile has no Split: one view at a time.
    LaunchedEffect(phone, model.view) {
        if (phone && model.view == ComposeComponent.View.Split) component.onViewChanged(ComposeComponent.View.Edit)
    }
    val views = if (phone) listOf(ComposeComponent.View.Edit, ComposeComponent.View.Preview) else ComposeComponent.View.entries
    Div({ classes("t-composer-bar") }) {
        IconButton(TalkIcons.Close, stringResource(MR.strings.compose_close), component::onClose)
        Tabs(
            items = views,
            selected = model.view,
            label = { stringResource(it.label) },
            onSelect = component::onViewChanged,
            ariaLabel = stringResource(MR.strings.compose_view),
            segmented = true,
        )
        Span({ classes("t-spacer") })
        if (!phone) Span({ classes("t-counter") }) {
            Text(
                when {
                    model.isUploading -> stringResource(MR.strings.compose_uploading)
                    model.isPublishing -> stringResource(MR.strings.compose_publishing)
                    else -> stringResource(MR.strings.compose_length, model.text.length, model.maxLength)
                },
            )
        }
        AddImage(component::onAttachImage)
        TalkButton(
            text = stringResource(if (model.mode is ComposeMode.Edit) MR.strings.compose_save else MR.strings.compose_publish),
            onClick = component::onPublish,
            enabled = model.canPublish,
        )
    }
}

/** The image goes to Blossom and its markdown into the text. */
@Composable
private fun AddImage(onAttach: (ImageFile) -> Unit) = AddImageButton("compose-image", stringResource(MR.strings.compose_add_image), onAttach)

private val ComposeComponent.View.label
    get() = when (this) {
        ComposeComponent.View.Edit -> MR.strings.compose_view_edit
        ComposeComponent.View.Split -> MR.strings.compose_view_split
        ComposeComponent.View.Preview -> MR.strings.compose_view_preview
    }
