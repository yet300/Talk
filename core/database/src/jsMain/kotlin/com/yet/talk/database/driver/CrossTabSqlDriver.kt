package com.yet.talk.database.driver

import app.cash.sqldelight.db.SqlDriver

/**
 * Every tab reads and writes the same database (the worker's owner tab runs the statements), but SQLDelight only
 * refreshes queries for writes made through its own driver. This tells the other tabs which tables changed, and
 * refreshes this tab's queries when they say so, so a post liked in one tab shows liked in the others.
 */
internal class CrossTabSqlDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    private val channel: dynamic = js("new BroadcastChannel('talk-sqlite-changes')")

    init {
        channel.onmessage = { event: dynamic ->
            val keys = (event.data as? Array<String>)
            if (keys != null) delegate.notifyListeners(*keys)
        }
    }

    override fun notifyListeners(vararg queryKeys: String) {
        delegate.notifyListeners(*queryKeys)
        channel.postMessage(queryKeys.toList().toTypedArray())
    }

    override fun close() {
        channel.close()
        delegate.close()
    }
}
