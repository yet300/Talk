package com.yet.talk.web.decompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arkivanov.decompose.value.Value

/** A Decompose [Value] as Compose [State] (Decompose's own extension needs Compose UI, not the DOM runtime). */
@Composable
fun <T : Any> Value<T>.subscribeAsState(): State<T> {
    val state = remember(this) { mutableStateOf(value) }
    DisposableEffect(this) {
        val cancellation = subscribe { state.value = it }
        onDispose { cancellation.cancel() }
    }
    return state
}
