package io.kvarto.http

import io.vertx.core.streams.WriteStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import java.net.URL
import java.nio.MappedByteBuffer
import java.nio.charset.Charset
import java.time.Duration
import kotlin.coroutines.suspendCoroutine


fun URL.resolve(path: String): URL = toURI().resolve(path).toURL()

suspend fun Body.asBytes(): ByteArray {
    val buf = MappedByteBuffer.allocate(4096)
    content().collect {
        buf.put(it)
    }
    return buf.array().copyOf(buf.position())
}

suspend fun Body.asString(charset: Charset = Charsets.UTF_8): String = String(asBytes(), charset)

internal suspend fun <T> WriteStream<T>.writeAwait(flow: Flow<T>) {
    flow.collect { elem ->
        waitTillWritable()
        write(elem)
    }
}

internal suspend fun <T> WriteStream<T>.waitTillWritable() {
    if (writeQueueFull()) {
        suspendCoroutine<Unit> { cont ->
            drainHandler {
                cont.resumeWith(Result.success(Unit))
            }
            exceptionHandler {
                cont.resumeWith(Result.failure(it))
            }
        }
    }
}

val Int.millis: Duration get() = Duration.ofMillis(toLong())

val Int.seconds: Duration get() = Duration.ofSeconds(toLong())

val Int.minutes: Duration get() = Duration.ofMinutes(toLong())

val Int.hours: Duration get() = Duration.ofHours(toLong())

val Int.days: Duration get() = Duration.ofDays(toLong())
