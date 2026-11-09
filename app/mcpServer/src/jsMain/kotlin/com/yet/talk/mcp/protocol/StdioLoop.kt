package com.yet.talk.mcp.protocol

import com.yet.talk.mcp.node.process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Newline-delimited JSON-RPC on stdin and stdout, the MCP stdio transport. Ends the process when the client goes away. */
internal fun runStdio(server: McpServer, scope: CoroutineScope) {
    var buffer = ""
    process.stdin.setEncoding("utf8")
    process.stdin.on("data") { chunk: String ->
        buffer += chunk
        val lines = buffer.split('\n')
        buffer = lines.last()
        lines.dropLast(1).map(String::trim).filter(String::isNotEmpty).forEach { line ->
            scope.launch {
                server.handle(line)?.let { process.stdout.write(it + "\n") }
            }
        }
    }
    // Relay sockets would otherwise keep Node running after the client has left.
    process.stdin.on("end") { process.exit(0) }
}
