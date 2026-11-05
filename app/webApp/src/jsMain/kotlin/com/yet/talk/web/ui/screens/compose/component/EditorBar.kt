package com.yet.talk.web.ui.screens.compose.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.feature.compose.ComposeComponent
import com.yet.talk.feature.compose.ComposeMode
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.IconButton
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.Tabs
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

/** Close · Edit/Split/Preview · (counter, add image) · Publish. */
@Composable
fun EditorBar(model: ComposeComponent.Model, component: ComposeComponent) {
    Div({ classes("t-composer-bar") }) {
        IconButton(TalkIcons.Close, stringResource(MR.strings.compose_close), component::onClose)
        Tabs(
            items = ComposeComponent.View.entries,
            selected = model.view,
            label = { stringResource(it.label) },
            onSelect = component::onViewChanged,
            ariaLabel = stringResource(MR.strings.compose_view),
            segmented = true,
        )
        Span({ classes("t-spacer") })
        Span({ classes("t-counter") }) {
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

/** An icon button over a hidden file input; the image goes to Blossom and its markdown into the text. */
@Composable
private fun AddImage(onAttach: (ImageFile) -> Unit) {
    val label = stringResource(MR.strings.compose_add_image)
    Label(forId = "compose-image", attrs = { classes("t-icon-btn"); attr("aria-label", label); attr("title", label) }) {
        TalkIcon(TalkIcons.Image)
    }
    Input(InputType.File) {
        id("compose-image")
        classes("t-file")
        attr("accept", "image/png,image/jpeg,image/gif,image/webp")
        onChange { event ->
            val input = event.target as HTMLInputElement
            val file = input.files?.get(0) ?: return@onChange
            val reader = FileReader()
            reader.onload = {
                val bytes = Int8Array(reader.result as ArrayBuffer).unsafeCast<ByteArray>()
                onAttach(ImageFile(bytes, file.type))
            }
            reader.readAsArrayBuffer(file)
            input.value = ""
        }
    }
}

private val ComposeComponent.View.label
    get() = when (this) {
        ComposeComponent.View.Edit -> MR.strings.compose_view_edit
        ComposeComponent.View.Split -> MR.strings.compose_view_split
        ComposeComponent.View.Preview -> MR.strings.compose_view_preview
    }
