package com.yet.talk.mcp.node

import com.yet.talk.mcp.agent.StateStore
import com.yet.talk.mcp.config.ConfigError

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

/** One JSON file per agent key, replaced atomically, private to its owner. */
internal class FileStateStore(directory: String, pubkey: String) : StateStore {
    private val file = "$directory/${pubkey.take(16)}.json"

    init {
        mkdirSync(directory, js("({ recursive: true, mode: 448 })"))
    }

    override fun load(): String? = if (existsSync(file)) readFileSync(file, "utf8") else null

    override fun save(text: String) {
        val temporary = "$file.tmp"
        writeFileSync(temporary, text, js("({ mode: 384 })"))
        renameSync(temporary, file)
    }
}

private const val GROUP_OR_OTHER_ACCESS = 63 // 0o077
