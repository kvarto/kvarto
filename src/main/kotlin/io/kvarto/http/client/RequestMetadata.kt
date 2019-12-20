package io.kvarto.http.client

import io.kvarto.http.common.HttpStatus
import io.kvarto.http.common.SUCCESS_STATUSES
import io.kvarto.utils.seconds
import java.time.Duration

// add circuit breaker
data class RequestMetadata(
    val operationId: String? = null,
    val successStatuses: Set<HttpStatus> = SUCCESS_STATUSES,
    val timeout: Duration = 2.seconds,
    val retry: RetryConfig = NO_RETRY
) {
    companion object {
        val EMPTY = RequestMetadata()
    }
}

data class RetryConfig(
    val retries: Int,
    val backoffStrategy: BackoffStrategy,
    val totalTimeout: Duration,
    val isRetryable: (Throwable) -> Boolean
)

interface BackoffStrategy {
    fun delayBeforeNextRetry(round: Int): Duration
}

val NO_BACKOFF = object : BackoffStrategy {
    override fun delayBeforeNextRetry(round: Int): Duration = Duration.ZERO
}

val NO_RETRY = RetryConfig(0, NO_BACKOFF, Duration.ofMillis(Long.MAX_VALUE), isRetryable = { false })
