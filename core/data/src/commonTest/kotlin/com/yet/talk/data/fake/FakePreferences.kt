package com.yet.talk.data.fake

import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakePreferences : PreferencesRepository {
    override val theme = MutableStateFlow(ThemeMode.System)
    override suspend fun setTheme(mode: ThemeMode) { theme.value = mode }
    override val channelNickname = MutableStateFlow("anon")
    override suspend fun setChannelNickname(nickname: String) { channelNickname.value = nickname }
    override val channelIntroSeen = MutableStateFlow(false)
    override suspend fun setChannelIntroSeen() { channelIntroSeen.value = true }
}
