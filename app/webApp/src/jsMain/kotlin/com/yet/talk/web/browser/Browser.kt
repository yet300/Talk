package com.yet.talk.web.browser

import kotlinx.browser.window
import org.w3c.dom.Element

/** `IntersectionObserver`, which the Kotlin/JS DOM declarations do not include. */
external class IntersectionObserver(callback: (entries: Array<IntersectionObserverEntry>, observer: IntersectionObserver) -> Unit) {
    fun observe(target: Element)
    fun disconnect()
}

external interface IntersectionObserverEntry {
    val isIntersecting: Boolean
}

/** Copies [text] to the clipboard (needs a user gesture). */
fun copyToClipboard(text: String) {
    window.navigator.asDynamic().clipboard?.writeText(text)
}

/** The system share sheet where the browser has one, else copies [text]. Returns whether it shared. */
fun shareOrCopy(text: String): Boolean {
    val navigator = window.navigator.asDynamic()
    if (navigator.share != undefined) {
        val data: dynamic = js("({})")
        data.text = text
        navigator.share(data)
        return true
    }
    copyToClipboard(text)
    return false
}
