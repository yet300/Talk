package com.yet.talk.database.dao

import kotlinx.coroutines.flow.Flow

/** Relays that still owe an event (see `Outbox.sq`). */
interface OutboxDao {
    data class Entry(val eventId: String, val relay: String, val eventJson: String, val attempts: Long)

    /** Owes [relay] the event, after [attempts] tries, next at [nextAt]. */
    suspend fun owe(eventId: String, relay: String, eventJson: String, attempts: Long, nextAt: Long)

    suspend fun settle(eventId: String, relay: String)

    /** The relays still owed [eventId], with their attempts. */
    suspend fun relaysFor(eventId: String): Map<String, Long>

    suspend fun due(now: Long): List<Entry>

    /** Events still waiting for at least one relay. */
    fun observePending(): Flow<Long>
}
