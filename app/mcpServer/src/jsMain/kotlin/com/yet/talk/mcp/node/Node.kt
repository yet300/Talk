@file:JsModule("fs")
@file:JsNonModule

package com.yet.talk.mcp.node

internal external fun readFileSync(path: String, encoding: String): String

internal external fun writeFileSync(path: String, data: String, options: dynamic)

internal external fun existsSync(path: String): Boolean

internal external fun mkdirSync(path: String, options: dynamic)

internal external fun renameSync(from: String, to: String)

internal external fun realpathSync(path: String): String

internal external fun statSync(path: String): Stats

internal external fun unlinkSync(path: String)

internal external interface Stats {
    val mode: Int
    val mtimeMs: Double
    fun isFile(): Boolean
}
