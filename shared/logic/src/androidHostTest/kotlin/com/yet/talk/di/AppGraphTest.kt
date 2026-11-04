package com.yet.talk.di

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.yet.talk.feature.root.RootComponent
import com.yet.talk.feature.tab.main.MainComponent
import kotlin.test.Test
import kotlin.test.assertIs

class AppGraphTest {

    @Test
    fun rootStartsOnHomeTab() {
        val root = createAndroidAppGraph().rootComponentFactory(DefaultComponentContext(LifecycleRegistry()))

        val main = assertIs<RootComponent.Child.Main>(root.stack.value.active.instance).component
        assertIs<MainComponent.Child.Home>(main.stack.value.active.instance)
    }
}
