package com.yet.talk.data.fake

import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakePreferences : PreferencesRepository {
    override val theme = MutableStateFlow(ThemeMode.System)
    override val privacy = MutableStateFlow(com.yet.talk.domain.model.settings.Privacy())
    override suspend fun setDeviceLock(enabled: Boolean) { privacy.value = privacy.value.copy(deviceLock = enabled) }
    override suspend fun setHideInScreenshots(hidden: Boolean) { privacy.value = privacy.value.copy(hideInScreenshots = hidden) }
    override suspend fun setTheme(mode: ThemeMode) { theme.value = mode }
    override val channelNickname = MutableStateFlow("anon")
    override suspend fun setChannelNickname(nickname: String) { channelNickname.value = nickname }
    override val channelIntroSeen = MutableStateFlow(false)
    override suspend fun setChannelIntroSeen() { channelIntroSeen.value = true }
}
