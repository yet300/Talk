package com.yet.talk.web.ui.components.markdown

/** `marked`: GitHub-flavoured markdown to HTML. */
@JsModule("marked")
@JsNonModule
internal external val markedModule: dynamic

/** `DOMPurify`: strips scripts, event handlers and anything else unsafe from the generated HTML. */
@JsModule("dompurify")
@JsNonModule
internal external val dompurifyModule: dynamic

internal object MarkdownHtml {
    private val purify: dynamic by lazy {
        val instance = dompurifyModule.default ?: dompurifyModule
        // Links leave the app in a new tab without handing it the opener.
        instance.addHook("afterSanitizeAttributes") { node: dynamic ->
            if (node.tagName == "A") {
                node.setAttribute("target", "_blank")
                node.setAttribute("rel", "noopener noreferrer nofollow")
            }
        }
        instance
    }

    /** Safe HTML for [markdown]: KarpathyTalk's GFM (tables, task lists, strikethrough, autolinks). */
    fun render(markdown: String): String {
        val options = js("({ gfm: true, breaks: false, async: false })")
        val html = markedModule.parse(markdown, options) as String
        val config = js("({ USE_PROFILES: { html: true }, FORBID_TAGS: ['style', 'form', 'input', 'button'] })")
        return purify.sanitize(html, config) as String
    }
}
