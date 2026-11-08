package com.yet.talk.web

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.router.webhistory.withWebHistory
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import com.yet.talk.di.createJsAppGraph
import com.yet.talk.resources.MR
import androidx.compose.runtime.getValue
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.ToastHost
import com.yet.talk.web.ui.root.RootContent
import com.yet.talk.web.ui.theme.TalkTheme
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.renderComposable

fun main() {
    installHistoryBasePath(BASE_PATH)
    val lifecycle = LifecycleRegistry()
    val graph = createJsAppGraph()
    // Binds the root's navigation to the browser history and restores it after a reload.
    val root = withWebHistory { stateKeeper, deepLink ->
        val normalizedLink = normalizeDeepLink(deepLink, BASE_PATH)
        graph.rootComponentFactory(DefaultComponentContext(lifecycle = lifecycle, stateKeeper = stateKeeper), normalizedLink)
    }
    lifecycle.attachToDocument()

    MainScope().launch {
        // moko loads the strings file before the first frame.
        val strings = MR.stringsLoader.getOrLoad()
        renderComposable(rootElementId = "root") {
            val theme by root.theme.subscribeAsState()
            TalkTheme(strings, theme) {
                ToastHost { RootContent(root) }
            }
        }
    }
}

/** Resumed while the tab is visible, stopped while it is hidden. */
private fun LifecycleRegistry.attachToDocument() {
    fun onVisibilityChanged() {
        // `Document.hidden` is missing from the Kotlin/JS DOM declarations.
        val hidden = document.asDynamic().hidden as Boolean
        if (hidden) stop() else resume()
    }
    onVisibilityChanged()
    document.addEventListener("visibilitychange", { onVisibilityChanged() })
}
