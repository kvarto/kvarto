package io.kvarto.http.common

import io.kvarto.utils.days
import io.kvarto.utils.millis
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import kotlin.test.*

class RetryUtilsTest {
    @Test
    fun `success case`() = testBlocking {
        var count = 0
        val expected = "foo"
        val actual = retry(RetryConfig(10)) {
            count++
            expected
        }

        assertEquals(expected, actual)
        assertEquals(1, count)
    }
    
    @Test
    fun `retries on retryable exception`() = testBlocking {
        var count = 0
        val expected = 100500
        val actual = retry(RetryConfig(4, isRetryable = { it == TheDamnTable })) {
            count++
            if (count <= 2) {
                throw TheDamnTable
            }
            expected
        }

        assertEquals(expected, actual)
        assertEquals(3, count)
    }

    @Test
    fun `retry happen after specified timeout`() = testBlocking {
        val retryNumbers = mutableListOf<Int>()
        val invocationTimes = mutableListOf<Long>()

        val strategy = object : BackoffStrategy {
            override fun getDelayBeforeRetry(retryNumber: Int): Duration {
                retryNumbers += retryNumber
                return (25 * retryNumber).millis
            }
        }
        val config = RetryConfig(10, strategy, isRetryable = { true })
        val startTime = System.currentTimeMillis()

        retry(config) {
            invocationTimes += System.currentTimeMillis()
            if (invocationTimes.size <= 3) throw TheDamnTable
            0
        }

        assertEquals(listOf(1, 2, 3), retryNumbers)
        assertThat(invocationTimes).hasSize(4)
        val (t0, t1, t2, t3) = invocationTimes
        assertTrue(t0 - startTime < 100)
        assertTrue(t1 - t0 >= 25)
        assertTrue(t2 - t1 >= 50)
        assertTrue(t3 - t2 >= 75)
    }


    @Test
    fun `retry on non-retryable exception`() = testBlocking {
        var count = 0
        val expected = Exception("(╯°□°)╯︵ ┻━┻")
        val actual = assertThrows<Exception> {
            retry<Unit>(RetryConfig(10)) {
                count++
                throw expected
            }
        }
        assertSame(expected, actual)
        assertEquals(1, count)
    }

    @Test
    fun `no retry executes code exactly once`() = testBlocking {
        var count = 0
        retry(NO_RETRY) {
            count++
        }
        assertEquals(1, count)
    }

    @Test
    fun `no retry does not retry and throws original exception`() = testBlocking {
        var count = 0
        assertThrows<IOException> {
            retry<Unit>(NO_RETRY) {
                count++
                throw IOException("test")
            }
        }
        assertEquals(1, count)
    }

    @Test
    fun `constant backoff strategy`() {
        val expected = 12.days
        val unit = ConstantBackoffStrategy(expected)
        repeat(10) {
            assertEquals(expected, unit.getDelayBeforeRetry(it))
        }
    }

    @Test
    fun `exponential backoff strategy`() {
        val unit = ExponentialBackoffStrategy(10.millis, 3)
        assertEquals(10.millis, unit.getDelayBeforeRetry(1))
        assertEquals(30.millis, unit.getDelayBeforeRetry(2))
        assertEquals(90.millis, unit.getDelayBeforeRetry(3))
        assertEquals(270.millis, unit.getDelayBeforeRetry(4))
    }

    @Test
    fun `fibonacci backoff strategy`() {
        val unit = FibonacciBackoffStrategy(10.millis)
        val numbers = listOf(1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987)
        numbers.forEachIndexed { i, number ->
            assertEquals(number * 10L, unit.getDelayBeforeRetry(i + 1).toMillis(), "$i")
        }
    }
}