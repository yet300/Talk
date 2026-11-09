package com.yet.talk.mcp

import com.yet.talk.mcp.config.ConfigError
import com.yet.talk.mcp.config.EnvVars
import com.yet.talk.mcp.config.McpConfig
import com.yet.talk.mcp.config.parseConfig
import com.yet.talk.nostr.encoding.Nip19
import com.yet.talk.nostr.key.SecretKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigTest {
    private val key = SecretKey.generate()

    private fun config(vararg env: Pair<String, String>, keyFile: String = key.toHex()): McpConfig {
        val values = env.toMap()
        return parseConfig(values::get, { keyFile }, "/home/agent")
    }

    @Test
    fun publishingIsOffUnlessExplicitlyEnabled() {
        assertFalse(config(EnvVars.KEY_FILE to "/k").publishEnabled)
        assertFalse(config(EnvVars.KEY_FILE to "/k", EnvVars.PUBLISH to "false").publishEnabled)
        assertTrue(config(EnvVars.KEY_FILE to "/k", EnvVars.PUBLISH to "true", EnvVars.AGENT_NAME to "Scout").publishEnabled)
    }

    @Test
    fun aMistypedPublishSwitchStopsTheServer() {
        assertFailsWith<ConfigError> { config(EnvVars.KEY_FILE to "/k", EnvVars.PUBLISH to "yes") }
    }

    @Test
    fun realPublishingNeedsAnAgentName() {
        assertFailsWith<ConfigError> { config(EnvVars.KEY_FILE to "/k", EnvVars.PUBLISH to "true") }
    }

    @Test
    fun readsAKeyFromFileAsHexOrNsec() {
        assertEquals(key, config(EnvVars.KEY_FILE to "/k").secretKey)
        assertEquals(key, config(EnvVars.KEY_FILE to "/k", keyFile = Nip19.nsec(key) + "\n").secretKey)
        assertEquals(key, config(EnvVars.SECRET_KEY to Nip19.nsec(key)).secretKey)
    }

    @Test
    fun refusesAmbiguousOrMissingKeys() {
        assertFailsWith<ConfigError> { config() }
        assertFailsWith<ConfigError> { config(EnvVars.KEY_FILE to "/k", EnvVars.SECRET_KEY to key.toHex()) }
    }

    @Test
    fun aBadKeyIsNeverEchoed() {
        val secret = "not-a-key-but-maybe-private"
        val error = assertFailsWith<ConfigError> { config(EnvVars.SECRET_KEY to secret) }
        assertFalse(secret in error.message.orEmpty())
    }

    @Test
    fun theConfigurationPrintsNoKey() {
        assertFalse(key.toHex() in config(EnvVars.KEY_FILE to "/k").toString())
    }

    @Test
    fun relaysMustBeSecureOrLocal() {
        assertFailsWith<ConfigError> { config(EnvVars.KEY_FILE to "/k", EnvVars.RELAYS to "ws://example.com") }
        assertEquals(listOf("wss://a.test", "ws://localhost:7777"), config(EnvVars.KEY_FILE to "/k", EnvVars.RELAYS to "wss://a.test, ws://localhost:7777").relays)
    }

    @Test
    fun limitsMustBePositive() {
        assertFailsWith<ConfigError> { config(EnvVars.KEY_FILE to "/k", EnvVars.POSTS_PER_HOUR to "0") }
        assertEquals(9, config(EnvVars.KEY_FILE to "/k", EnvVars.POSTS_PER_HOUR to "9").posts.perHour)
    }
}
