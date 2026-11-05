package com.yet.talk.web.ui.components.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.dom.Div

/**
 * Rendered markdown in the design's long-form style (`.t-md`: 17/28, h1 31/36, code blocks on surface).
 * The HTML is sanitized by DOMPurify before it reaches the DOM.
 */
@Composable
fun Markdown(markdown: String, className: String = "t-md") {
    val html = remember(markdown) { MarkdownHtml.render(markdown) }
    Div({
        classes(className)
        ref { element ->
            element.innerHTML = html
            onDispose { }
        }
    })
}
