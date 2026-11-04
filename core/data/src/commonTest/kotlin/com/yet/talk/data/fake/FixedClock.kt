package com.yet.talk.data.fake

import kotlin.time.Clock
import kotlin.time.Instant

class FixedClock(var seconds: Long) : Clock {
    override fun now(): Instant = Instant.fromEpochSeconds(seconds)
}
