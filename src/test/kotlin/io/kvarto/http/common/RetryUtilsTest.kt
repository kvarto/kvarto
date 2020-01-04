package io.kvarto.http.common

import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertSame

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
    fun `no retry does not retry`() = testBlocking {
        var count = 0
        assertThrows<RetriesLimitReachedException> {
            retry<Unit>(NO_RETRY) {
                count++
                throw IOException("test")
            }
        }
        assertEquals(1, count)
    }
}