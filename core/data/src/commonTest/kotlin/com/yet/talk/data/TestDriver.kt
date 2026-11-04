package com.yet.talk.data

import com.yet.talk.database.driver.SqlDriverFactory

/** An empty in-memory database per call; null where no driver runs without a bundler (JS). */
internal expect fun testDriverFactory(): SqlDriverFactory?
