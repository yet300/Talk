package com.yet.talk.mcp.protocol

import com.yet.talk.mcp.agent.ToolError
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** A tool the server offers. [run] throws [ToolError] for anything the agent should read. */
internal class McpTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
    val readOnly: Boolean,
    val run: suspend (JsonObject) -> JsonElement,
)

/** The MCP methods a tools-only server needs, as JSON-RPC 2.0. One message in, at most one message out. */
internal class McpServer(
    tools: List<McpTool>,
    private val instructions: String,
    private val log: (String) -> Unit,
) {
    private val tools = tools.associateBy { it.name }

    /** The answer to one line of input, or null for a notification. */
    suspend fun handle(line: String): String? {
        val request = runCatching { Json.parseToJsonElement(line) as JsonObject }.getOrNull()
            ?: return error(JsonNull, PARSE_ERROR, "Parse error")
        val id = request["id"]
        val method = (request["method"] as? JsonPrimitive)?.contentOrNull
            ?: return if (id == null) null else error(id, INVALID_REQUEST, "Invalid request")
        if (id == null) return null
        val params = request["params"] as? JsonObject ?: JsonObject(emptyMap())
        return when (method) {
            "initialize" -> result(id, initialize(params))
            "ping" -> result(id, JsonObject(emptyMap()))
            "tools/list" -> result(id, buildJsonObject { put("tools", JsonArray(tools.values.map(::describe))) })
            "tools/call" -> call(id, params)
            else -> error(id, METHOD_NOT_FOUND, "Method not found: $method")
        }
    }

    private fun initialize(params: JsonObject) = buildJsonObject {
        val requested = (params["protocolVersion"] as? JsonPrimitive)?.contentOrNull
        put("protocolVersion", requested?.takeIf { it in SUPPORTED_VERSIONS } ?: SUPPORTED_VERSIONS.first())
        put("capabilities", buildJsonObject { put("tools", JsonObject(emptyMap())) })
        put("serverInfo", buildJsonObject {
            put("name", "talk-mcp")
            put("version", "1.0.0")
        })
        put("instructions", instructions)
    }

    private fun describe(tool: McpTool) = buildJsonObject {
        put("name", tool.name)
        put("description", tool.description)
        put("inputSchema", tool.inputSchema)
        put("annotations", buildJsonObject {
            put("readOnlyHint", tool.readOnly)
            put("openWorldHint", true)
        })
    }

    private suspend fun call(id: JsonElement, params: JsonObject): String {
        val name = (params["name"] as? JsonPrimitive)?.contentOrNull ?: return error(id, INVALID_PARAMS, "Missing tool name")
        val tool = tools[name] ?: return error(id, INVALID_PARAMS, "Unknown tool: $name")
        val arguments = params["arguments"] as? JsonObject ?: JsonObject(emptyMap())
        // Keys never travel through a tool call: not a person's, and not the agent's own into a post.
        if (SECRET_KEY.containsMatchIn(arguments.toString())) {
            return result(id, toolResult("Refused: this call contains an nsec secret key. Secret keys are never accepted or published through tools.", isError = true))
        }
        val text = try {
            tool.run(arguments).toString()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: ToolError) {
            return result(id, toolResult(failure.message.orEmpty(), isError = true))
        } catch (failure: Exception) {
            log("tool $name failed: ${failure::class.simpleName}")
            return result(id, toolResult("The tool failed unexpectedly.", isError = true))
        }
        return result(id, toolResult(text, isError = false))
    }

    private fun toolResult(text: String, isError: Boolean) = buildJsonObject {
        put("content", buildJsonArray {
            add(buildJsonObject {
                put("type", "text")
                put("text", text)
            })
        })
        put("isError", isError)
    }

    private fun result(id: JsonElement, value: JsonElement): String = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", id)
        put("result", value)
    }.toString()

    private fun error(id: JsonElement, code: Int, message: String): String = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", id)
        put("error", buildJsonObject {
            put("code", code)
            put("message", message)
        })
    }.toString()

    private companion object {
        /** Newest first; a client asking for another version gets the newest (all share what this server uses). */
        val SUPPORTED_VERSIONS = listOf("2025-11-25", "2025-06-18", "2025-03-26", "2024-11-05")
        val SECRET_KEY = Regex("nsec1[02-9ac-hj-np-z]{20,}", RegexOption.IGNORE_CASE)
        const val PARSE_ERROR = -32700
        const val INVALID_REQUEST = -32600
        const val METHOD_NOT_FOUND = -32601
        const val INVALID_PARAMS = -32602
    }
}
