package com.yet.talk.mcp

import com.yet.talk.mcp.agent.StateStore
import com.yet.talk.mcp.protocol.McpServer
import com.yet.talk.mcp.protocol.McpTool
import com.yet.talk.mcp.protocol.schema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class ProtocolTest {
    private val agent = Agent()

    @Test
    fun initializeAnswersWithTheClientsVersionAndTools() = runTest {
        val result = agent.rpc("initialize", buildJsonObject { put("protocolVersion", "2025-06-18") }).getValue("result").jsonObject
        assertEquals("2025-06-18", result.getValue("protocolVersion").jsonPrimitive.content)
        assertTrue("tools" in result.getValue("capabilities").jsonObject)
        assertTrue("untrusted" in result.getValue("instructions").jsonPrimitive.content)
    }

    @Test
    fun anUnknownVersionGetsTheNewest() = runTest {
        val result = agent.rpc("initialize", buildJsonObject { put("protocolVersion", "1999-01-01") }).getValue("result").jsonObject
        assertEquals("2025-11-25", result.getValue("protocolVersion").jsonPrimitive.content)
    }

    @Test
    fun listsEveryToolWithASchema() = runTest {
        val tools = agent.rpc("tools/list").getValue("result").jsonObject.getValue("tools").jsonArray.map { it.jsonObject }
        assertEquals(
            setOf(
                "talk_whoami", "talk_read_feed", "talk_read_post", "talk_read_profile", "talk_search", "talk_read_activity",
                "talk_publish_post", "talk_reply", "talk_quote", "talk_like", "talk_follow", "talk_set_profile", "talk_send_chat", "talk_read_chats",
            ),
            tools.map { it.getValue("name").jsonPrimitive.content }.toSet(),
        )
        assertTrue(tools.all { it.getValue("inputSchema").jsonObject.getValue("type").jsonPrimitive.content == "object" })
    }

    @Test
    fun notificationsGetNoAnswer() = runTest {
        assertNull(agent.server.handle("""{"jsonrpc":"2.0","method":"notifications/initialized"}"""))
    }

    @Test
    fun malformedInputAndUnknownMethodsAreErrors() = runTest {
        val parse = Json.parseToJsonElement(agent.server.handle("{oops")!!).jsonObject
        assertEquals(-32700, parse.getValue("error").jsonObject.getValue("code").jsonPrimitive.int)
        assertEquals(-32601, agent.rpc("nope").getValue("error").jsonObject.getValue("code").jsonPrimitive.int)
    }

    @Test
    fun aSecretKeyInAnyArgumentIsRefused() = runTest {
        val nsec = com.yet.talk.nostr.encoding.Nip19.nsec(com.yet.talk.nostr.key.SecretKey.generate())
        val outcome = agent.call("talk_publish_post", "markdown" to JsonPrimitive("my key is $nsec"))
        assertTrue(outcome.isError)
        assertFalse(nsec in outcome.text)
        assertTrue(agent.relays.published.isEmpty())
    }

    private fun serverWith(run: suspend (JsonObject) -> JsonElement) = McpServer(
        listOf(McpTool("boom", "Fails", schema(), readOnly = true, run)),
        "",
        {},
    )

    private suspend fun McpServer.callBoom(): JsonObject =
        Json.parseToJsonElement(handle("""{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"boom"}}""")!!).jsonObject

    @Test
    fun aNativeJsErrorInAToolStillGetsAnAnswer() = runTest {
        val answer = serverWith { js("throw new Error('EACCES: permission denied')") }.callBoom()
        val result = answer.getValue("result").jsonObject
        assertEquals("true", result.getValue("isError").jsonPrimitive.content)
        assertFalse("EACCES" in result.toString(), "Node error text stays out of the answer")
    }

    @Test
    fun aTimeoutInsideAToolIsAnErrorNotASilentCancellation() = runTest {
        val answer = serverWith { withTimeout(1) { delay(1_000); JsonNull } }.callBoom()
        assertEquals("true", answer.getValue("result").jsonObject.getValue("isError").jsonPrimitive.content)
    }

    @Test
    fun aDamagedStateStoreThatThrowsNativelyIsAToolError() = runTest {
        val store = object : StateStore {
            override fun load(): String? = js("throw new Error('EIO')")
            override fun save(text: String) = Unit
        }
        val outcome = Agent(store = store).call("talk_publish_post", "markdown" to JsonPrimitive("Hello"))
        assertTrue(outcome.isError)
    }

    @Test
    fun whoamiShowsThePublicSideOnly() = runTest {
        val outcome = agent.call("talk_whoami")
        assertEquals(agent.pubkey, outcome.json.getValue("pubkey").jsonPrimitive.content)
        assertFalse(agent.key.toHex() in outcome.text)
    }
}
