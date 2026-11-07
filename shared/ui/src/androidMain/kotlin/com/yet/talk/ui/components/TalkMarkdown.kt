package com.yet.talk.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown

/**
 * A post's markdown (mikepenz multiplatform-markdown-renderer): headings, lists, code, quotes, tables and images
 * (through Coil), in the theme's colours and type. Raw HTML is not rendered; links open in the browser.
 */
@Composable
fun TalkMarkdown(content: String, modifier: Modifier = Modifier) {
    Markdown(content = content, imageTransformer = Coil3ImageTransformerImpl, modifier = modifier)
}
