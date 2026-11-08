package com.yet.talk.ui.screens.compose

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import com.yet.talk.ui.components.glass.GlassSegments
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.compose.ComposeComponent
import com.yet.talk.feature.compose.ComposeMode
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.Caption
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.TalkMarkdown
import com.yet.talk.ui.components.glass.GlassIconButton
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.components.glass.GlassTopBar
import com.yet.talk.ui.platform.rememberImagePicker
import com.yet.talk.ui.screens.compose.component.ImageStrip
import com.yet.talk.ui.screens.compose.component.TargetPreview
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.TalkMotion
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * The editor (`Compose-Mobile`, decision 5): Close, Edit · Preview (and Split when the window is wide, as on the Duo
 * inner screen), Add image (the system photo picker, uploaded to Blossom), Publish; the post it replies to, quotes or
 * edits, the attached images, then the markdown body (monospace) or its preview. The draft lives in the component.
 */
@Composable
fun ComposeContent(component: ComposeComponent) {
    val model by component.model.subscribeAsState()
    val pick = rememberImagePicker(component::onAttachImage)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        LaunchedEffect(wide, model.view) { if (!wide && model.view == ComposeComponent.View.Split) component.onViewChanged(ComposeComponent.View.Edit) }
        val views = if (wide) ComposeComponent.View.entries else listOf(ComposeComponent.View.Edit, ComposeComponent.View.Preview)
        GlassPage(
            modifier = Modifier.imePadding(),
            bar = {
                GlassTopBar(
                    leading = { GlassIconButton(TalkIcons.Close, stringResource(MR.strings.compose_close), component::onClose) },
                    title = {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            GlassSegments(
                                items = views,
                                selected = model.view,
                                label = { stringResource(it.label) },
                                onSelect = component::onViewChanged,
                                segmentWidth = 84.dp,
                            )
                        }
                    },
                    trailing = {
                        GlassIconButton(TalkIcons.Image, stringResource(MR.strings.compose_add_image), pick)
                        TalkButton(
                            stringResource(if (model.mode is ComposeMode.Edit) MR.strings.compose_save else MR.strings.compose_publish),
                            component::onPublish, ButtonStyle.Post, enabled = model.canPublish,
                        )
                    },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (model.mode is ComposeMode.Edit) Caption(stringResource(MR.strings.compose_edit_note))
                ErrorRow(model.error, onDismiss = component::onErrorShown, onRetry = component::onPublish.takeIf { model.canPublish })
                model.target?.let { TargetPreview(model.mode, it) }
                AnimatedVisibility(model.images.isNotEmpty()) { ImageStrip(model.images, component::onRemoveImage) }
                Caption(
                    when {
                        model.isUploading -> stringResource(MR.strings.compose_uploading)
                        model.isPublishing -> stringResource(MR.strings.compose_publishing)
                        else -> stringResource(MR.strings.compose_length, model.text.length, model.maxLength)
                    },
                )
                AnimatedContent(model.view, transitionSpec = { fadeIn(TalkMotion.fade()) togetherWith fadeOut(TalkMotion.fade()) }, label = "view", modifier = Modifier.weight(1f)) { view ->
                    when (view) {
                        ComposeComponent.View.Edit -> Editor(model.text, component::onTextChanged, Modifier.fillMaxSize())
                        ComposeComponent.View.Preview -> Preview(model.text, Modifier.fillMaxSize())
                        ComposeComponent.View.Split -> Row(Modifier.fillMaxSize()) {
                            Editor(model.text, component::onTextChanged, Modifier.weight(1f).fillMaxHeight())
                            VerticalDivider(color = MaterialTheme.talkColors.border, thickness = MaterialTheme.sizes.hairline)
                            Preview(model.text, Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
        }
    }
}

/** The markdown body (monospace 15/24), focused when the editor opens. */
@Composable
private fun Editor(text: String, onChange: (String) -> Unit, modifier: Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val label = stringResource(MR.strings.compose_body)
    val placeholder = stringResource(MR.strings.compose_placeholder)
    val colors = MaterialTheme.talkColors
    BasicTextField(
        value = text,
        onValueChange = onChange,
        modifier = modifier.focusRequester(focus).padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm).semantics { contentDescription = label },
        textStyle = MaterialTheme.talkTypography.codeBlock.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.accent),
        decorationBox = { inner ->
            if (text.isEmpty()) Text(placeholder, style = MaterialTheme.talkTypography.codeBlock, color = colors.textSecondary)
            inner()
        },
    )
}

@Composable
private fun Preview(text: String, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TalkMarkdown(text, Modifier.fillMaxWidth())
    }
}

private val ComposeComponent.View.label
    get() = when (this) {
        ComposeComponent.View.Edit -> MR.strings.compose_view_edit
        ComposeComponent.View.Split -> MR.strings.compose_view_split
        ComposeComponent.View.Preview -> MR.strings.compose_view_preview
    }
