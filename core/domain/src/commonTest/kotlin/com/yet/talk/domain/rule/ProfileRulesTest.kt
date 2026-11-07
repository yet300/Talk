package com.yet.talk.domain.rule

import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.ProfileEdit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileRulesTest {

    @Test
    fun trimsFieldsAndDropsBlankOnes() {
        val clean = ProfileRules.validate(ProfileEdit(name = "  ann\n", displayName = "Ann\nB", about = " ", nip05 = "Ann@Example.com")).getOrThrow()
        assertEquals(ProfileEdit(name = "ann", displayName = "Ann B", nip05 = "ann@example.com"), clean)
    }

    @Test
    fun limitsTheAboutLineInCharactersNotUnits() {
        assertEquals("🚀".repeat(160), ProfileRules.validate(ProfileEdit(about = "🚀".repeat(160))).getOrThrow().about)
        assertEquals(DomainError.AboutTooLong(160), ProfileRules.validate(ProfileEdit(about = "a".repeat(161))).exceptionOrNull())
    }

    @Test
    fun imagesComeOnlyFromAllowedSites() {
        assertEquals(
            "https://media.giphy.com/media/l0MYt5jPR6QX5pnqM/giphy.gif",
            ProfileRules.validate(ProfileEdit(picture = " https://giphy.com/gifs/cat-dance-l0MYt5jPR6QX5pnqM ")).getOrThrow().picture,
        )
        val unsplash = "https://images.unsplash.com/photo-1500000000000-abc?w=1500"
        assertEquals(unsplash, ProfileRules.validate(ProfileEdit(banner = unsplash)).getOrThrow().banner)
        listOf(
            "http://images.unsplash.com/photo-1", "https://evil.example/x.gif", "https://images.unsplash.com.evil.example/x",
            "https://user@images.unsplash.com/x", "https://avatars.githubusercontent.com/u/1", "https://giphy.com/explore/cats",
        ).forEach { assertEquals(DomainError.ImageHostNotAllowed, ProfileRules.validate(ProfileEdit(picture = it)).exceptionOrNull(), it) }
    }

    @Test
    fun showsOnlyAllowedHosts() {
        assertEquals("https://avatars.githubusercontent.com/u/1", ProfileImageHosts.shownOrNull("https://avatars.githubusercontent.com/u/1"))
        assertNull(ProfileImageHosts.shownOrNull("https://nostr.build/i/x.gif"))
        assertNull(ProfileImageHosts.shownOrNull(null))
    }

    @Test
    fun gifsHaveAStillFrame() {
        assertEquals("https://media.giphy.com/media/abc/giphy_s.gif", ProfileImageHosts.still("https://media.giphy.com/media/abc/giphy.gif"))
        assertEquals("https://media.giphy.com/media/abc/giphy_s.gif", ProfileImageHosts.still("https://i.giphy.com/abc.gif"))
        assertEquals("https://images.unsplash.com/photo-1", ProfileImageHosts.still("https://images.unsplash.com/photo-1"))
    }
}
