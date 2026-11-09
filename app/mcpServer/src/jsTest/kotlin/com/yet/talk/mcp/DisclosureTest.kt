package com.yet.talk.mcp

import com.yet.talk.mcp.agent.Disclosure
import com.yet.talk.mcp.agent.ModelNames
import com.yet.talk.mcp.agent.ToolError
import com.yet.talk.nostr.nip.Metadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DisclosureTest {
    private fun build(edit: Disclosure.Edit = Disclosure.Edit(), previous: String? = null, model: String? = null): JsonObject {
        val before = previous?.let { it to Metadata.parse(com.yet.talk.nostr.event.Event("", "", 0, 0, emptyList(), it, ""))!! }
        return Json.parseToJsonElement(Disclosure.profileContent(before, edit, "Scout", model)).jsonObject
    }

    private fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content

    @Test
    fun aNewProfileSaysItIsAnAgent() {
        val profile = build()
        assertEquals("true", profile.text("bot"))
        assertEquals(Disclosure.ABOUT_LINE, profile.text("about"))
        assertEquals("Scout (AI)", profile.text("display_name"))
        assertTrue(Disclosure.isDisclosed(profile.toString()))
    }

    @Test
    fun theDisclosureCannotBeEditedAway() {
        val profile = build(Disclosure.Edit(about = "Just a regular person who loves posting", displayName = "Alex"))
        assertTrue(profile.text("about").startsWith(Disclosure.ABOUT_LINE))
        assertTrue("regular person" in profile.text("about"))
        assertEquals("Alex (AI)", profile.text("display_name"))
    }

    @Test
    fun anExistingDisclosureIsNotRepeated() {
        val once = build(Disclosure.Edit(about = "Reads papers"))
        val twice = build(previous = once.toString())
        assertEquals(once.text("about"), twice.text("about"))
        assertEquals(once.text("display_name"), twice.text("display_name"))
    }

    @Test
    fun theDeclaredModelIsInTheAboutLine() {
        assertTrue("Model: Claude Opus 5.5." in build(model = "Claude Opus 5.5").text("about"))
    }

    @Test
    fun otherFieldsAreKept() {
        assertEquals("a@b.c", build(previous = """{"name":"x","lud16":"a@b.c"}""").text("lud16"))
    }

    @Test
    fun anAlreadyLabelledNameIsLeftAlone() {
        assertEquals("Paper Bot", build(Disclosure.Edit(displayName = "Paper Bot")).text("display_name"))
    }

    @Test
    fun aModelThatDoesNotWriteThePostsCannotBeNamed() {
        assertFailsWith<ToolError> { build(Disclosure.Edit(displayName = "GPT-5 News")) }
        assertFailsWith<ToolError> { build(Disclosure.Edit(about = "Powered by Gemini"), model = "Claude Opus 5.5") }
        assertFailsWith<ToolError> { build(Disclosure.Edit(name = "grok")) }
    }

    @Test
    fun theDeclaredModelAndItsMakerMayBeNamed() {
        ModelNames.check("Claude, made by Anthropic", "Claude Opus 5.5")
    }

    @Test
    fun plainWordsAreNotModelNames() {
        ModelNames.check("Reads haiku, sonnets and magnum opus reviews", null)
    }

    @Test
    fun imagesOnlyComeFromAllowedHosts() {
        assertFailsWith<ToolError> { build(Disclosure.Edit(picture = "https://openai.com/logo.png")) }
        assertEquals("https://images.unsplash.com/photo-1", build(Disclosure.Edit(picture = "https://images.unsplash.com/photo-1")).text("picture"))
    }

    @Test
    fun aPictureFromAnotherHostIsDropped() {
        assertFalse("picture" in build(previous = """{"name":"x","picture":"https://evil.test/logo.png"}"""))
    }

    @Test
    fun theAboutLimitCountsTheDisclosure() {
        assertFailsWith<ToolError> { build(Disclosure.Edit(about = "x".repeat(150))) }
    }

    @Test
    fun aProfileWithoutBotIsNotDisclosed() {
        assertFalse(Disclosure.isDisclosed("""{"about":"${Disclosure.ABOUT_LINE}"}"""))
        assertFalse(Disclosure.isDisclosed("""{"bot":true,"about":"hello"}"""))
        assertFalse(Disclosure.isDisclosed("not json"))
    }
}
