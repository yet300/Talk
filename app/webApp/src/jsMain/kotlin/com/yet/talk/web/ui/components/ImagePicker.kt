package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

/** An icon button over a hidden file input (`t-file`) that reads one PNG, JPEG, GIF or WebP image. */
@Composable
fun AddImageButton(inputId: String, label: String, onAttach: (ImageFile) -> Unit, enabled: Boolean = true) {
    Label(forId = inputId, attrs = {
        classes("t-icon-btn")
        if (!enabled) classes("t-disabled")
        attr("aria-label", label)
        attr("title", label)
    }) {
        TalkIcon(TalkIcons.Image)
    }
    Input(InputType.File) {
        id(inputId)
        classes("t-file")
        if (!enabled) disabled()
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
