package com.yet.talk.feature.tab.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yet.talk.feature.tab.home.HomeComponent

/** The tab container: one child per bottom-bar tab. */
interface MainComponent {
    val stack: Value<ChildStack<*, Child>>

    fun onTabClicked(tab: Tab)

    enum class Tab { Home }

    sealed class Child {
        class Home(val component: HomeComponent) : Child()
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext): MainComponent
    }
}
