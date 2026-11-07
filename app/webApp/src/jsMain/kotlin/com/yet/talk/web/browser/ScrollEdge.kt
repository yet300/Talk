package com.yet.talk.web.browser

import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

/** Past this, the bar shows its scroll-edge shadow (design pass 4). */
private const val EDGE_PX = 4.0

/** Past this, a clear bar over a banner (Profile) turns to glass and shows its title. */
private const val FAR_PX = 160.0

/**
 * Marks a glass bar `t-scrolled` once content is under it, and `t-scrolled-far` once a banner has gone, from its
 * scroll container (the web's column, or the page on a phone). Works in every browser, unlike CSS scroll timelines.
 * Returns the cleanup.
 */
fun trackScroll(bar: HTMLElement): () -> Unit {
    val scroller = scrollParent(bar)
    val update: (Event?) -> Unit = {
        val top = scroller?.scrollTop ?: window.scrollY
        bar.classList.toggle("t-scrolled", top > EDGE_PX)
        bar.classList.toggle("t-scrolled-far", top > FAR_PX)
    }
    update(null)
    if (scroller != null) scroller.addEventListener("scroll", update) else window.addEventListener("scroll", update)
    return { if (scroller != null) scroller.removeEventListener("scroll", update) else window.removeEventListener("scroll", update) }
}

private fun scrollParent(element: Element): Element? {
    var current = element.parentElement
    while (current != null) {
        val overflowY = window.getComputedStyle(current).overflowY
        if ((overflowY == "auto" || overflowY == "scroll") && current != window.document.documentElement) return current
        current = current.parentElement
    }
    return null
}
