package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

/** Shows a short confirmation ("Link copied") above the content for a moment (`Toast`). */
fun interface Toaster {
    fun show(text: String)
}

val LocalToaster = staticCompositionLocalOf<Toaster> { Toaster { } }

/** Hosts the app's toast: one at a time, centred above the bottom edge (`t-toast-wrap` + `x-toast`). */
@Composable
fun ToastHost(content: @Composable () -> Unit) {
    var message by remember { mutableStateOf<Toast?>(null) }
    val toaster = remember { Toaster { text -> message = Toast(text) } }
    CompositionLocalProvider(LocalToaster provides toaster, content = content)
    message?.let { toast ->
        LaunchedEffect(toast) {
            delay(TOAST_MILLIS)
            if (message === toast) message = null
        }
        Div({ classes("t-toast-wrap") }) {
            Div({ classes("x-toast", "t-part"); attr("role", "status") }) { Text(toast.text) }
        }
    }
}

/** A new instance per show, so the same text twice restarts the timer. */
private class Toast(val text: String)

private const val TOAST_MILLIS = 2_500L
