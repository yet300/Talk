package com.yet.talk.mcp.protocol

import com.yet.talk.mcp.node.process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Newline-delimited JSON-RPC, the MCP stdio transport. Once the input has ended it calls [exit], as soon as every
 * request has been answered and every answer flushed: relay sockets would otherwise keep Node running.
 */
internal class StdioPump(
    private val handle: suspend (String) -> String?,
    private val scope: CoroutineScope,
    private val write: (text: String, flushed: () -> Unit) -> Unit,
    private val exit: () -> Unit,
) {
    private var buffer = ""
    private var ended = false
    private var exited = false

    /** Requests being served and answers being written. */
    private var busy = 0

    fun onData(chunk: String) {
        buffer += chunk
        val lines = buffer.split('\n')
        buffer = lines.last()
        lines.dropLast(1).forEach(::serve)
    }

    fun onEnd() {
        serve(buffer)
        buffer = ""
        ended = true
        exitIfDone()
    }

    private fun serve(line: String) {
        val request = line.trim()
        if (request.isEmpty()) return
        busy++
        scope.launch {
            try {
                handle(request)?.let { answer ->
                    busy++
                    write(answer + "\n") { finished() }
                }
            } finally {
                finished()
            }
        }
    }

    private fun finished() {
        busy--
        exitIfDone()
    }

    private fun exitIfDone() {
        if (ended && busy == 0 && !exited) {
            exited = true
            exit()
        }
    }
}

internal fun runStdio(server: McpServer, scope: CoroutineScope) {
    val pump = StdioPump(
        handle = server::handle,
        scope = scope,
        write = { text, flushed -> process.stdout.write(text, { flushed() }) },
        exit = { process.exit(0) },
    )
    process.stdin.setEncoding("utf8")
    process.stdin.on("data") { chunk: String -> pump.onData(chunk) }
    process.stdin.on("end") { pump.onEnd() }
    // The client closed its end: no answer can reach it any more.
    process.stdout.on("error") { process.exit(0) }
}
