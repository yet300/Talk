package com.yet.talk.mcp.node

import com.yet.talk.mcp.agent.StateStore
import com.yet.talk.mcp.agent.ToolError
import com.yet.talk.mcp.config.ConfigError
import kotlin.js.Date
import kotlin.random.Random
import kotlinx.coroutines.delay

/** The Node global. */
internal external val process: dynamic

internal fun env(name: String): String? = process.env[name] as String?

/** Keeps a key that came through the environment out of anything that later reads it (crash reports, child processes). */
internal fun forgetEnv(name: String) {
    js("delete process.env[name]")
}

internal fun homeDir(): String = (process.env["HOME"] as String?) ?: throw ConfigError("HOME is not set")

/** Diagnostics go to stderr: stdout carries only the MCP protocol. */
internal fun logError(message: String) {
    process.stderr.write("talk-mcp: $message\n")
}

/**
 * The agent's key file. Like an SSH key it must be private to its owner, and it must live outside any git
 * repository so that it cannot be committed by accident.
 */
internal fun readKeyFile(path: String): String {
    val real = runCatching { realpathSync(path) }.getOrElse { throw ConfigError("The key file does not exist or cannot be read") }
    val stats = statSync(real)
    if (!stats.isFile()) throw ConfigError("The key file is not a regular file")
    if (stats.mode and GROUP_OR_OTHER_ACCESS != 0) throw ConfigError("The key file must be readable by its owner only (chmod 600)")
    if (isInsideGitRepository(real)) throw ConfigError("The key file must be outside any git repository")
    return readFileSync(real, "utf8")
}

internal fun isInsideGitRepository(path: String): Boolean {
    var directory = path.substringBeforeLast('/', "")
    while (directory.isNotEmpty()) {
        if (existsSync("$directory/.git")) return true
        directory = directory.substringBeforeLast('/', "")
    }
    return existsSync("/.git")
}

/**
 * One JSON file per agent key, replaced atomically, private to its owner. Every MCP session starts its own server, so
 * writers on one key are serialised by a lockfile: creating it exclusively is the lock. It is held only for a synchronous
 * read and write, so a lockfile that has aged past [LOCK_STALE_MS] was left by a process that died.
 */
internal class FileStateStore(directory: String, pubkey: String) : StateStore {
    private val file = "$directory/${pubkey.take(16)}.json"
    private val lock = "$file.lock"

    init {
        mkdirSync(directory, js("({ recursive: true, mode: 448 })"))
    }

    override fun load(): String? = if (existsSync(file)) readFileSync(file, "utf8") else null

    override fun save(text: String) {
        val temporary = uniquePath(file)
        writeFileSync(temporary, text, js("({ mode: 384 })"))
        renameSync(temporary, file)
    }

    override suspend fun <T> exclusive(block: () -> T): T {
        acquireLock()
        try {
            return block()
        } finally {
            runCatching { unlinkSync(lock) }
        }
    }

    private suspend fun acquireLock() {
        repeat(LOCK_ATTEMPTS) {
            if (tryCreateLock()) return
            removeLockIfStale()
            delay(LOCK_RETRY_MS)
        }
        throw ToolError("Nothing was published: another server on this key is holding its state file. Try again shortly.")
    }

    private fun tryCreateLock(): Boolean = try {
        writeFileSync(lock, "", js("({ flag: 'wx', mode: 384 })"))
        true
    } catch (failure: Throwable) {
        if (failure.asDynamic().code != "EEXIST") throw failure
        false
    }

    // Renaming first means that of several servers finding the same stale lock, only one removes it.
    private fun removeLockIfStale() {
        runCatching {
            if (Date.now() - statSync(lock).mtimeMs > LOCK_STALE_MS) {
                val taken = uniquePath(lock)
                renameSync(lock, taken)
                unlinkSync(taken)
            }
        }
    }

    private fun uniquePath(path: String) = "$path.${process.pid}.${Random.nextLong().toULong().toString(16)}"
}

private const val GROUP_OR_OTHER_ACCESS = 63 // 0o077
private const val LOCK_ATTEMPTS = 100
private const val LOCK_RETRY_MS = 20L
private const val LOCK_STALE_MS = 10_000.0
