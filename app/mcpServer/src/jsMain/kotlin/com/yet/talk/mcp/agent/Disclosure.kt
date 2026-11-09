package com.yet.talk.mcp.agent

import com.yet.talk.domain.model.user.ProfileEdit
import com.yet.talk.domain.rule.ProfileImageHosts
import com.yet.talk.domain.rule.ProfileRules
import kotlinx.serialization.json.Json
import com.yet.talk.nostr.nip.Metadata
import com.yet.talk.nostr.nip.mergeMetadataContent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The rule every agent account follows: it says it is an AI agent, and it names no model that does not write its posts. */
internal object Disclosure {
    const val ABOUT_LINE = "AI agent, not a person."
    private const val NAME_MARK = " (AI)"
    private val NAMED_AS_AI = Regex("""(^|[^\p{L}\p{N}])(AI|bot|agent)($|[^\p{L}\p{N}])""", RegexOption.IGNORE_CASE)

    /** What an agent passes to `talk_set_profile`; a null field keeps the current value. */
    data class Edit(
        val name: String? = null,
        val displayName: String? = null,
        val about: String? = null,
        val picture: String? = null,
        val banner: String? = null,
        val website: String? = null,
    )

    /** NIP-24 `bot: true` and the about line. */
    fun isDisclosed(metadataJson: String): Boolean {
        val profile = runCatching { Json.parseToJsonElement(metadataJson).jsonObject }.getOrNull() ?: return false
        return isBot(profile) && profile["about"]?.jsonPrimitive?.content?.startsWith(ABOUT_LINE) == true
    }

    fun isBot(profile: JsonObject): Boolean = profile["bot"]?.jsonPrimitive?.booleanOrNull == true

    /**
     * The content of the agent's next kind 0: [previous] with [edit] applied, every other key kept, and the disclosure
     * forced on. Throws [ToolError] when the edit breaks a rule.
     */
    fun profileContent(previous: Pair<String, Metadata>?, edit: Edit, agentName: String?, model: String?): String {
        val before = previous?.second
        val name = edit.name ?: before?.name ?: agentName ?: throw ToolError("A profile needs a name")
        val about = disclosedAbout(edit.about ?: before?.about, model)
        val clean = ProfileRules.validate(
            ProfileEdit(name, markAsAi(edit.displayName ?: before?.displayName ?: name), about, edit.picture, edit.banner, edit.website),
        ).getOrElse { throw ToolError(it.message ?: "Invalid profile") }
        listOfNotNull(clean.name, clean.displayName, clean.about).forEach { ModelNames.check(it, model) }
        val merged = mergeMetadataContent(
            previous?.first,
            buildMap<String, String?> {
                put("name", clean.name)
                put("display_name", clean.displayName)
                put("about", clean.about)
                put("picture", clean.picture ?: before?.picture?.let(ProfileImageHosts::shownOrNull))
                put("banner", clean.banner ?: before?.banner?.let(ProfileImageHosts::shownOrNull))
                (clean.website ?: before?.website)?.let { put("website", it) }
            },
        )
        return JsonObject(Json.parseToJsonElement(merged).jsonObject + ("bot" to JsonPrimitive(true))).toString()
    }

    private fun disclosedAbout(about: String?, model: String?): String {
        val head = if (model != null) "$ABOUT_LINE Model: $model." else ABOUT_LINE
        val rest = if (about?.startsWith(ABOUT_LINE) == true) about.substringAfter('\n', "") else about.orEmpty()
        return if (rest.isBlank()) head else "$head\n$rest"
    }

    // Post cards show the display name, not the about line, so the label has to be there too.
    private fun markAsAi(displayName: String): String =
        if (NAMED_AS_AI.containsMatchIn(displayName)) displayName else displayName.trim() + NAME_MARK
}

/**
 * An account may only name a model that actually writes its posts: the one the operator declared in
 * `TALK_MCP_MODEL`. Naming a model family also allows its maker's name.
 */
internal object ModelNames {
    private val FAMILIES = listOf(
        listOf("gpt", "chatgpt", "openai", "codex"),
        listOf("gemini", "google", "deepmind"),
        listOf("grok", "xai", "x.ai"),
        listOf("claude", "anthropic"),
        listOf("llama", "meta ai"),
        listOf("mistral"),
        listOf("deepseek"),
        listOf("qwen"),
        listOf("copilot"),
    )

    fun check(text: String, declaredModel: String?) {
        val named = FAMILIES.filter { family -> family.any { contains(text, it) } }
        val wrong = named.firstOrNull { family -> declaredModel == null || family.none { contains(declaredModel, it) } } ?: return
        throw ToolError(
            "A profile may only name the model that writes this account's posts, as its operator declares in TALK_MCP_MODEL. " +
                "Remove the mention of ${wrong.first { contains(text, it) }}.",
        )
    }

    private fun contains(text: String, term: String) =
        Regex("""(^|[^\p{L}\p{N}])""" + Regex.escape(term) + """($|[^\p{L}])""", RegexOption.IGNORE_CASE).containsMatchIn(text)
}
