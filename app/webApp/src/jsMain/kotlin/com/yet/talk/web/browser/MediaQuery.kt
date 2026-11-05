package com.yet.talk.web.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import org.w3c.dom.events.Event

/** Whether [query] matches now, updated as the window changes. */
@Composable
fun rememberMediaQuery(query: String): Boolean {
    val list = remember(query) { window.matchMedia(query) }
    var matches by remember(query) { mutableStateOf(list.matches) }
    DisposableEffect(list) {
        val listener: (Event) -> Unit = { matches = list.matches }
        list.addEventListener("change", listener)
        onDispose { list.removeEventListener("change", listener) }
    }
    return matches
}

/**
 * Phone width (below the design's 688 px breakpoint): the phone artboards apply — bottom navigation capsule,
 * home header, compose button, Edit · Preview editor.
 */
@Composable
fun rememberIsPhone(): Boolean = rememberMediaQuery("(max-width: 687px)")
