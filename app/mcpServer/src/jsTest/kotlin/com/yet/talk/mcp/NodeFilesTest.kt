package com.yet.talk.mcp

import com.yet.talk.mcp.config.ConfigError
import com.yet.talk.mcp.node.FileStateStore
import com.yet.talk.mcp.node.isInsideGitRepository
import com.yet.talk.mcp.node.mkdirSync
import com.yet.talk.mcp.node.readKeyFile
import com.yet.talk.mcp.node.writeFileSync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
