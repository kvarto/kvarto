package io.kvarto.utils

import io.kvarto.http.common.Body
import io.vertx.core.Vertx
import io.vertx.core.streams.ReadStream
import io.vertx.core.streams.WriteStream
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.flow.*
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

internal suspend fun <T> Flow<T>.writeTo(stream: WriteStream<T>) {
    collect { elem ->
        stream.waitTillWritable()
        stream.write(elem)
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

internal fun <T> ReadStream<T>.toFlow(vertx: Vertx): Flow<T> {
    val ch = toChannel(vertx)
    return flow {
        for (elem in ch) {
            emit(elem)
        }
    }
}

val Int.millis: Duration get() = Duration.ofMillis(toLong())

val Int.seconds: Duration get() = Duration.ofSeconds(toLong())

val Int.minutes: Duration get() = Duration.ofMinutes(toLong())

val Int.hours: Duration get() = Duration.ofHours(toLong())

val Int.days: Duration get() = Duration.ofDays(toLong())
