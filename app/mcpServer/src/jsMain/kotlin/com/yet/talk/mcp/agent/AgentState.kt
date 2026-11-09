package com.yet.talk.mcp.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/** Where [AgentState] lives between runs. */
internal interface StateStore {
    fun load(): String?

    fun save(text: String)
}

/** A tool call failed in a way the agent should be told about. The message reaches the agent as it is. */
internal class ToolError(message: String) : Exception(message)

internal enum class Budget(val key: String) { Posts("posts"), Actions("actions") }

/**
 * What must survive a restart: MCP clients start a new server per session, so limits kept in memory would reset each
 * time. Read from the store on every use, so that two servers running on one key see each other. A file that cannot
 * be read stops publishing rather than resetting the limits.
 */
internal class AgentState(private val store: StateStore) {

    fun times(budget: Budget): List<Long> = read()[budget.key]?.jsonArray?.map { it.jsonPrimitive.long }.orEmpty()

    /** Adds [now] to [budget], dropping times at or before [forgetBefore]. */
    fun record(budget: Budget, now: Long, forgetBefore: Long) {
        val kept = times(budget).filter { it > forgetBefore } + now
        write(budget.key to JsonArray(kept.map(::JsonPrimitive)))
    }

    /** True once this key is known to have a profile on relays: finding none later means relays are incomplete. */
    var profileKnown: Boolean
        get() = flag(PROFILE)
        set(value) = write(PROFILE to JsonPrimitive(value))

    /** The same for the follow list, which a new version replaces whole. */
    var contactsKnown: Boolean
        get() = flag(CONTACTS)
        set(value) = write(CONTACTS to JsonPrimitive(value))

    private fun flag(key: String) = read()[key]?.jsonPrimitive?.booleanOrNull == true

    private fun read(): JsonObject {
        val text = store.load() ?: return JsonObject(emptyMap())
        return runCatching { Json.parseToJsonElement(text).jsonObject }
            .getOrElse { throw ToolError("The server's state file is damaged. Its operator must repair or remove it.") }
    }

    private fun write(entry: Pair<String, kotlinx.serialization.json.JsonElement>) =
        store.save(JsonObject(read() + entry).toString())

    private companion object {
        const val PROFILE = "profile_known"
        const val CONTACTS = "contacts_known"
    }
}
