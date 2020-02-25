package io.kvarto.utils

import io.kvarto.http.common.Body
import io.kvarto.testBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtilsTest {
    @Test
    fun `Body asBytes`() = testBlocking {
        assertThat(Body.EMPTY.asBytes()).isEqualTo(byteArrayOf())

        val string = "test abc"
        assertThat(Body(string).asBytes()).isEqualTo(string.toByteArray())

        val bytes = byteArrayOf(1, -2, 3, 0, 127)
        assertThat(Body(bytes).asBytes()).isEqualTo(bytes)

        val flow = flow {
            repeat(5) {
                delay(2)
                emit(byteArrayOf((it + 65).toByte()))
            }
        }
        assertThat(Body(flow).asBytes()).isEqualTo(byteArrayOf(65, 66, 67, 68, 69))
    }
    
    @Test
    fun `Body asString`() = testBlocking {
        assertThat(Body.EMPTY.asString()).isEqualTo("")

        val string = "foobar"
        assertThat(Body(string).asString()).isEqualTo(string)

        val bytes = byteArrayOf(65, 66, 67)
        assertThat(Body(bytes).asString()).isEqualTo("ABC")

        val flow = flow {
            repeat(5) {
                delay(2)
                emit(byteArrayOf((it + 65).toByte()))
            }
        }
        assertThat(Body(flow).asString()).isEqualTo("ABCDE")
    }

    @Test
    fun `Body asFlow`() = testBlocking {
        assertThat(Body.EMPTY.asFlow().toList()).isEmpty()

        val string = "foobar"
        assertThat(Body(string).asFlow().toList()).containsExactly(string.toByteArray())

        val bytes = byteArrayOf(65, 66, 67)
        assertThat(Body(bytes).asFlow().toList()).containsExactly(bytes)

        val flow = flow {
            repeat(5) {
                delay(2)
                emit(byteArrayOf((it + 65).toByte()))
            }
        }
        assertThat(Body(flow).asFlow()).isSameAs(flow)
    }
}