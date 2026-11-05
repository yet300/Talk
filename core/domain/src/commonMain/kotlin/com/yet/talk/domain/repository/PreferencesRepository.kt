package com.yet.talk.domain.repository

import com.yet.talk.domain.model.settings.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Device preferences: not account data, so signing out keeps them. */
interface PreferencesRepository {
    val theme: Flow<ThemeMode>

    suspend fun setTheme(mode: ThemeMode)
}
