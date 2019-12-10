package io.kvarto.http

import io.kvarto.utils.resolve
import io.kvarto.utils.seconds
import java.net.URL
import java.time.Duration

data class HttpRequest(
    val url: URL,
    val method: HttpMethod = HttpMethod.GET,
    val headers: StringMultiMap = StringMultiMap.EMPTY,
    val params: StringMultiMap = StringMultiMap.EMPTY,
    val body: Body = Body.EMPTY
) {
    fun withPath(path: String) = copy(url = url.resolve(path))

    fun withMethod(method: HttpMethod) = copy(method = method)

    fun addQueryParam(name: String, value: String) = copy(params = params.add(name, value))

    fun addHeader(name: String, value: String) = copy(headers = headers.add(name, value))

    fun withBody(body: Body) = copy(body = body)
}

data class RequestContext(
    val operationId: String? = null,
    val successStatuses: Set<HttpStatus> = SUCCESS_STATUSES,
    val timeout: Duration = 2.seconds,
    val retry: RetryConfig = NO_RETRY
) {
    companion object {
        val EMPTY = RequestContext()
    }
}

data class RetryConfig(
    val retries: Int,
    val backoffStrategy: BackoffStrategy,
    val totalTimeout: Duration,
    val isRetriable: (Throwable) -> Boolean
)

val NO_BACKOFF = object : BackoffStrategy {
    override fun delayBeforeNextRetry(round: Int): Duration = Duration.ZERO
}
val NO_RETRY = RetryConfig(0, NO_BACKOFF, Duration.ofMillis(Long.MAX_VALUE), isRetriable = { false })

interface BackoffStrategy {
    fun delayBeforeNextRetry(round: Int): Duration
}
