package com.yet.talk.mcp

import com.yet.talk.mcp.protocol.StdioPump
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class StdioPumpTest {
    private val written = mutableListOf<String>()
    private val unflushed = mutableListOf<() -> Unit>()
    private var exits = 0

    private fun TestScope.pump(handle: suspend (String) -> String?) = StdioPump(
        handle = handle,
        scope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
        write = { text, flushed ->
            written += text
            unflushed += flushed
        },
        exit = { exits++ },
    )

    private operator fun kotlinx.coroutines.CoroutineScope.plus(context: kotlin.coroutines.CoroutineContext) =
        kotlinx.coroutines.CoroutineScope(coroutineContext + context)

    private fun flushAll() {
        unflushed.toList().forEach { it() }
        unflushed.clear()
    }

    @Test
    fun linesSplitAcrossChunksAreJoined() = runTest {
        val pump = pump { "got $it" }
        pump.onData("one\ntw")
        pump.onData("o\n")
        assertEquals(listOf("got one\n", "got two\n"), written)
    }

    @Test
    fun exitsAtOnceWhenNothingIsPending() = runTest {
        pump { it }.onEnd()
        assertEquals(1, exits)
    }

    @Test
    fun aCallInFlightFinishesBeforeTheProcessExits() = runTest {
        val result = CompletableDeferred<String>()
        val pump = pump { result.await() }
        pump.onData("call\n")
        pump.onEnd()
        assertEquals(0, exits)

        result.complete("answer")
        assertEquals(listOf("answer\n"), written)
        assertEquals(0, exits, "the answer has not been flushed yet")

        flushAll()
        assertEquals(1, exits)
    }

    @Test
    fun aLastLineWithoutNewlineIsStillServed() = runTest {
        val pump = pump { "got $it" }
        pump.onData("tail")
        pump.onEnd()
        assertEquals(listOf("got tail\n"), written)
        assertEquals(0, exits)
        flushAll()
        assertEquals(1, exits)
    }

    @Test
    fun exitsOnlyOnce() = runTest {
        val pump = pump { it }
        pump.onData("a\nb\n")
        pump.onEnd()
        flushAll()
        assertEquals(1, exits)
        assertTrue(written.isNotEmpty())
        assertFalse(exits > 1)
    }
}
