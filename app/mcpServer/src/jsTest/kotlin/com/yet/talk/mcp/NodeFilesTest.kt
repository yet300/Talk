package com.yet.talk.mcp

import com.yet.talk.mcp.agent.ToolError
import com.yet.talk.mcp.config.ConfigError
import com.yet.talk.mcp.node.FileStateStore
import com.yet.talk.mcp.node.existsSync
import com.yet.talk.mcp.node.isInsideGitRepository
import com.yet.talk.mcp.node.mkdirSync
import com.yet.talk.mcp.node.readKeyFile
import com.yet.talk.mcp.node.unlinkSync
import com.yet.talk.mcp.node.writeFileSync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class NodeFilesTest {
    private val directory: String = js("require('fs').mkdtempSync(require('path').join(require('os').tmpdir(), 'talk-mcp-'))") as String

    private fun file(name: String, mode: Int, content: String = "secret"): String {
        val path = "$directory/$name"
        writeFileSync(path, content, js("({ mode: mode })"))
        return path
    }

    @Test
    fun aPrivateKeyFileIsRead() {
        assertEquals("secret", readKeyFile(file("key", 384)))
    }

    @Test
    fun aKeyFileOthersCanReadIsRefused() {
        assertFailsWith<ConfigError> { readKeyFile(file("key", 420)) }
    }

    @Test
    fun aMissingKeyFileIsRefused() {
        assertFailsWith<ConfigError> { readKeyFile("$directory/none") }
    }

    @Test
    fun aKeyFileInsideARepositoryIsRefused() {
        mkdirSync("$directory/repo/.git", js("({ recursive: true })"))
        mkdirSync("$directory/repo/nested", js("({ recursive: true })"))
        assertTrue(isInsideGitRepository("$directory/repo/nested/key"))
        assertFalse(isInsideGitRepository("$directory/key"))
        val path = "$directory/repo/nested/key"
        writeFileSync(path, "secret", js("({ mode: 384 })"))
        assertFailsWith<ConfigError> { readKeyFile(path) }
    }

    private val pubkey = "b".repeat(64)
    private val lockPath get() = "$directory/state/${pubkey.take(16)}.json.lock"
    private fun stateStore() = FileStateStore("$directory/state", pubkey)

    @Test
    fun aHeldLockIsReleasedAfterItsBlock() = runTest {
        val store = stateStore()
        store.exclusive { assertTrue(existsSync(lockPath)) }
        assertFalse(existsSync(lockPath))
    }

    @Test
    fun theLockIsReleasedWhenItsBlockFails() = runTest {
        val store = stateStore()
        assertFailsWith<IllegalStateException> { store.exclusive { error("failed") } }
        assertFalse(existsSync(lockPath))
    }

    @Test
    fun aLockHeldByAnotherServerRefusesAndRunsNothing() = runTest {
        val store = stateStore()
        writeFileSync(lockPath, "", js("({ mode: 384 })"))
        var ran = false
        assertFailsWith<ToolError> { store.exclusive { ran = true } }
        assertFalse(ran)
        assertTrue(existsSync(lockPath), "someone else's lock stays")
    }

    @Test
    fun waitsForALockThatIsReleased() = runTest {
        val store = stateStore()
        writeFileSync(lockPath, "", js("({ mode: 384 })"))
        launch {
            delay(100)
            unlinkSync(lockPath)
        }
        var ran = false
        store.exclusive { ran = true }
        assertTrue(ran)
    }

    @Test
    fun aLockLeftByADeadProcessIsTakenOver() = runTest {
        val store = stateStore()
        val path = lockPath
        writeFileSync(path, "", js("({ mode: 384 })"))
        js("require('fs').utimesSync(path, 1, 1)")
        var ran = false
        store.exclusive { ran = true }
        assertTrue(ran)
        assertFalse(existsSync(lockPath))
    }

    @Test
    fun aLeftoverTemporaryFileDoesNotBlockSaving() {
        val store = stateStore()
        // The old shared name: as a directory, writing to it would fail.
        mkdirSync("$directory/state/${pubkey.take(16)}.json.tmp", js("({ recursive: true })"))
        store.save("{}")
        assertEquals("{}", store.load())
    }

    @Test
    fun stateSurvivesInAPrivateFile() {
        val state = "$directory/state"
        val store = FileStateStore(state, "a".repeat(64))
        assertNull(store.load())
        store.save("""{"posts":[1]}""")
        assertEquals("""{"posts":[1]}""", FileStateStore(state, "a".repeat(64)).load())
        val mode = com.yet.talk.mcp.node.statSync("$state/${"a".repeat(16)}.json").mode
        assertEquals(0, mode and 63)
    }
}
