package com.yet.talk.mcp.config

import com.yet.talk.api.source.relay.DefaultRelays
import com.yet.talk.nostr.encoding.Nip19
import com.yet.talk.nostr.key.SecretKey

/** The server cannot start as configured. The message names variables only, never their values. */
internal class ConfigError(message: String) : Exception(message)

internal data class RateLimit(val perHour: Int, val minGapSeconds: Long)

/** Set by whoever runs the server, never by the agent: no tool can change any of it. */
internal class McpConfig(
    val secretKey: SecretKey,
    val publishEnabled: Boolean,
    val relays: List<String>,
    val agentName: String?,
    val model: String?,
    val posts: RateLimit,
    val actions: RateLimit,
    val stateDir: String,
) {
    override fun toString(): String = "McpConfig(redacted)"
}

internal object EnvVars {
    const val KEY_FILE = "TALK_MCP_KEY_FILE"
    const val SECRET_KEY = "TALK_MCP_SECRET_KEY"
    const val PUBLISH = "TALK_MCP_PUBLISH"
    const val AGENT_NAME = "TALK_MCP_AGENT_NAME"
    const val MODEL = "TALK_MCP_MODEL"
    const val RELAYS = "TALK_MCP_RELAYS"
    const val POSTS_PER_HOUR = "TALK_MCP_POSTS_PER_HOUR"
    const val POST_GAP_SECONDS = "TALK_MCP_POST_GAP_SECONDS"
    const val ACTIONS_PER_HOUR = "TALK_MCP_ACTIONS_PER_HOUR"
    const val ACTION_GAP_SECONDS = "TALK_MCP_ACTION_GAP_SECONDS"
    const val STATE_DIR = "TALK_MCP_STATE_DIR"
}

/** [env] reads a variable; [readKeyFile] returns the contents of the key file at a path, or throws [ConfigError]; [homeDir] is asked only for the default state directory. */
internal fun parseConfig(env: (String) -> String?, readKeyFile: (String) -> String, homeDir: () -> String): McpConfig {
    fun value(name: String) = env(name)?.trim()?.takeIf(String::isNotEmpty)
    fun positive(name: String, default: Int): Int {
        val raw = value(name) ?: return default
        return raw.toIntOrNull()?.takeIf { it > 0 } ?: throw ConfigError("$name must be a positive whole number")
    }

    val publishEnabled = when (value(EnvVars.PUBLISH)?.lowercase()) {
        null, "false" -> false
        "true" -> true
        else -> throw ConfigError("${EnvVars.PUBLISH} must be true or false")
    }
    val agentName = value(EnvVars.AGENT_NAME)
    if (publishEnabled && agentName == null) throw ConfigError("${EnvVars.AGENT_NAME} is required when ${EnvVars.PUBLISH} is true")
    val relays = value(EnvVars.RELAYS)?.split(',')?.map(String::trim)?.filter(String::isNotEmpty) ?: DefaultRelays.urls
    if (relays.isEmpty() || !relays.all(::isRelayUrl)) throw ConfigError("${EnvVars.RELAYS} must be a comma-separated list of wss:// addresses")
    return McpConfig(
        secretKey = loadKey(value(EnvVars.KEY_FILE), value(EnvVars.SECRET_KEY), readKeyFile),
        publishEnabled = publishEnabled,
        relays = relays,
        agentName = agentName,
        model = value(EnvVars.MODEL),
        posts = RateLimit(positive(EnvVars.POSTS_PER_HOUR, 4), positive(EnvVars.POST_GAP_SECONDS, 300).toLong()),
        actions = RateLimit(positive(EnvVars.ACTIONS_PER_HOUR, 60), positive(EnvVars.ACTION_GAP_SECONDS, 5).toLong()),
        stateDir = value(EnvVars.STATE_DIR) ?: "${homeDir()}/.config/talk-mcp",
    )
}

private fun loadKey(file: String?, inline: String?, readKeyFile: (String) -> String): SecretKey {
    if (file != null && inline != null) throw ConfigError("Set only one of ${EnvVars.KEY_FILE} and ${EnvVars.SECRET_KEY}")
    val source = file ?: EnvVars.SECRET_KEY.takeIf { inline != null }
        ?: throw ConfigError("Set ${EnvVars.KEY_FILE} (recommended) or ${EnvVars.SECRET_KEY} to the agent's own key")
    val raw = if (file != null) readKeyFile(file) else inline.orEmpty()
    return parseSecretKey(raw) ?: throw ConfigError("$source does not hold a valid nsec or 64-character hex secret key")
}

internal fun parseSecretKey(raw: String): SecretKey? {
    val text = raw.trim()
    return (Nip19.decode(text) as? Nip19.Entity.PrivateKey)?.secretKey ?: SecretKey.fromHex(text)
}

// ws:// is for a relay on this machine only, such as a local test relay.
private val LOCAL_RELAY = Regex("""ws://(localhost|127\.0\.0\.1)(:\d+)?(/.*)?""")

private fun isRelayUrl(url: String) = url.startsWith("wss://") || LOCAL_RELAY.matches(url)
