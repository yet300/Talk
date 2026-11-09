package com.yet.talk.mcp.protocol

import com.yet.talk.mcp.agent.ToolError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Builds a tool's JSON Schema. */
internal class SchemaBuilder {
    private val properties = linkedMapOf<String, JsonObject>()
    private val required = mutableListOf<String>()

    private fun add(name: String, isRequired: Boolean, schema: JsonObject) {
        properties[name] = schema
        if (isRequired) required += name
    }

    fun string(name: String, description: String, isRequired: Boolean = false, choices: List<String>? = null) = add(
        name,
        isRequired,
        buildJsonObject {
            put("type", "string")
            put("description", description)
            choices?.let { put("enum", JsonArray(it.map(::JsonPrimitive))) }
        },
    )

    fun integer(name: String, description: String) = add(
        name,
        false,
        buildJsonObject {
            put("type", "integer")
            put("description", description)
        },
    )

    fun strings(name: String, description: String, isRequired: Boolean = false) = add(
        name,
        isRequired,
        buildJsonObject {
            put("type", "array")
            put("items", buildJsonObject { put("type", "string") })
            put("description", description)
        },
    )

    fun build() = buildJsonObject {
        put("type", "object")
        put("properties", JsonObject(properties))
        put("required", JsonArray(required.map(::JsonPrimitive)))
        put("additionalProperties", false)
    }
}

internal fun schema(block: SchemaBuilder.() -> Unit = {}): JsonObject = SchemaBuilder().apply(block).build()

internal fun JsonObject.stringOrNull(name: String): String? = (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

internal fun JsonObject.string(name: String): String = stringOrNull(name) ?: throw ToolError("Missing text argument: $name")

internal fun JsonObject.longOrNull(name: String): Long? = (this[name] as? JsonPrimitive)?.longOrNull

internal fun JsonObject.limit(name: String = "limit", default: Int = 20, max: Int = 50): Int =
    (longOrNull(name)?.toInt() ?: default).coerceIn(1, max)

internal fun JsonObject.stringsOrNull(name: String): List<String>? =
    (this[name] as? JsonArray)?.map { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.contentOrNull ?: throw ToolError("$name must be a list of text values") }
