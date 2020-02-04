package io.kvarto.http.common

import io.kvarto.utils.millis
import io.vertx.core.VertxException
import kotlinx.coroutines.time.delay
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeoutException


suspend fun <T> retry(config: RetryConfig, f: suspend () -> T): T =
    retryInner(0, config, f)

private tailrec suspend fun <T> retryInner(tryNumber: Int, config: RetryConfig, f: suspend () -> T): T {
    try {
        return f()
    } catch (e: Throwable) {
        if (config.retries == 0 || !config.isRetryable(e)) throw e
        if (tryNumber == config.retries) throw RetriesLimitReachedException("", e)
        delay(config.backoffStrategy.getDelayBeforeRetry(tryNumber + 1))
    }
    return retryInner(tryNumber + 1, config, f)
}


class RetriesLimitReachedException(message: String, cause: Throwable) : RuntimeException(message, cause)

fun isRetryable(t: Throwable): Boolean =
    when (t) {
        is IOException -> true
        is TimeoutException -> true
        is RetriesLimitReachedException -> true // for nested retries
        is VertxException -> "timeout" in t.message.orEmpty()
        else -> false
    }

data class RetryConfig(
    val retries: Int,
    val backoffStrategy: BackoffStrategy = NO_BACKOFF,
    val totalTimeout: Duration = Duration.ofMillis(Long.MAX_VALUE),
    val isRetryable: (Throwable) -> Boolean = ::isRetryable
)

interface BackoffStrategy {
    fun getDelayBeforeRetry(retryNumber: Int): Duration
}

class ConstantBackoffStrategy(val timeout: Duration) : BackoffStrategy {
    override fun getDelayBeforeRetry(retryNumber: Int): Duration = timeout
}

class ExponentialBackoffStrategy(val timeout: Duration, val multiplier: Int) : BackoffStrategy {
    override fun getDelayBeforeRetry(retryNumber: Int): Duration =
        (timeout.toMillis() * Math.pow(multiplier.toDouble(), retryNumber.toDouble() - 1)).toLong().millis
}

class FibonacciBackoffStrategy(val base: Duration) : BackoffStrategy {
    override fun getDelayBeforeRetry(retryNumber: Int): Duration = (base.toMillis() * fib(retryNumber)).millis

    private fun fib(n: Int): Int {
        if (n < 2) return 1
        var a = 1
        var b = 1
        repeat(n - 1) {
            val c = b
            b += a
            a = c
        }
        return b
    }
}

val NO_BACKOFF = object : BackoffStrategy {
    override fun getDelayBeforeRetry(retryNumber: Int): Duration = Duration.ZERO
}

val NO_RETRY = RetryConfig(0, NO_BACKOFF, Duration.ofMillis(Long.MAX_VALUE))
