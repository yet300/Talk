package com.yet.talk.feature.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yet.talk.feature.tab.main.MainComponent

/** The app's root: full-screen destinations above the tabs (later: auth, post, compose). */
interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        class Main(val component: MainComponent) : Child()
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext): RootComponent
    }
}
