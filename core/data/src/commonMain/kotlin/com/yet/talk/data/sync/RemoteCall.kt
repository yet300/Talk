package com.yet.talk.data.sync

import com.yet.talk.domain.model.error.DomainError
import kotlin.coroutines.cancellation.CancellationException

/** Runs a network call; anything but a [DomainError] or cancellation becomes [DomainError.Network]. */
internal suspend fun <T> remote(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: DomainError) {
        Result.failure(error)
    } catch (error: Exception) {
        Result.failure(DomainError.Network(error))
    }
