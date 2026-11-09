package com.yet.talk.mcp

import com.yet.talk.api.di.relayHttpClient
import com.yet.talk.mcp.config.ConfigError
import com.yet.talk.mcp.config.EnvVars
import com.yet.talk.mcp.config.McpConfig
import com.yet.talk.mcp.config.parseConfig
import com.yet.talk.mcp.di.McpGraph
import com.yet.talk.mcp.node.FileStateStore
import com.yet.talk.mcp.node.env
import com.yet.talk.mcp.node.forgetEnv
import com.yet.talk.mcp.node.homeDir
import com.yet.talk.mcp.node.logError
import com.yet.talk.mcp.node.process
import com.yet.talk.mcp.node.readKeyFile
import com.yet.talk.mcp.protocol.runStdio
import com.yet.talk.nostr.signer.LocalKeySigner
import dev.zacsweers.metro.createGraphFactory
import io.ktor.client.engine.js.Js
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() {
    // stdout belongs to the protocol: anything a library prints must not end up in it.
    console.asDynamic().log = console.asDynamic().error
    console.asDynamic().info = console.asDynamic().error

    val config = loadConfig() ?: return process.exit(1) as Unit
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val clock = Clock.System
    val graph = createGraphFactory<McpGraph.Factory>().create(
        relayHttpClient(Js),
        scope,
        AgentAuthenticator(LocalKeySigner(config.secretKey), clock),
        ConfiguredRelays(config.relays),
    )
    val store = FileStateStore(config.stateDir, config.secretKey.publicKey)
    runStdio(createServer(config, graph, store, clock, ::logError), scope)
    logError("ready, publishing ${if (config.publishEnabled) "ON" else "off (dry run)"}")
}

private fun loadConfig(): McpConfig? = try {
    parseConfig(::env, ::readKeyFile, ::homeDir).also { forgetEnv(EnvVars.SECRET_KEY) }
} catch (failure: ConfigError) {
    logError(failure.message.orEmpty())
    null
}
