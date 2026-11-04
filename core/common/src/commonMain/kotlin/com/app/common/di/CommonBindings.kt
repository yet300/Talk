package com.app.common.di

import com.app.common.AppDispatchers
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@ContributesTo(AppScope::class)
@BindingContainer
object CommonBindings {
    @SingleIn(AppScope::class)
    @Provides
    fun provideStoreFactory(): StoreFactory = DefaultStoreFactory()

    @SingleIn(AppScope::class)
    @Provides
    fun provideAppDispatchers(): AppDispatchers = AppDispatchers()

    /** Lives as long as the app: relay connections and background sync. */
    @SingleIn(AppScope::class)
    @Provides
    fun provideAppScope(dispatchers: AppDispatchers): CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.default)

    @Provides
    fun provideClock(): Clock = Clock.System
}
