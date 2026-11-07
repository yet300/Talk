package com.yet.talk.web.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import org.w3c.dom.events.Event

/** Whether the browser thinks it is online (`navigator.onLine`), updated on `online` / `offline`. */
@Composable
fun rememberIsOnline(): Boolean {
    var online by remember { mutableStateOf(window.navigator.onLine) }
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { online = window.navigator.onLine }
        window.addEventListener("online", listener)
        window.addEventListener("offline", listener)
        onDispose {
            window.removeEventListener("online", listener)
            window.removeEventListener("offline", listener)
        }
    }
    return online
}
