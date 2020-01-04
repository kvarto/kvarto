package io.kvarto.http.client

import io.kvarto.http.common.*
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

