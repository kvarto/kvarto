package io.kvarto.utils

import com.fasterxml.jackson.module.kotlin.readValue
import io.kvarto.http.common.*
import io.vertx.core.Vertx
import io.vertx.core.json.jackson.DatabindCodec
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

suspend fun Body.asBytes(): ByteArray =
    when (this) {
        is EmptyBody -> byteArrayOf()
        is ByteArrayBody -> value
        is JsonBody -> DatabindCodec.mapper().writeValueAsBytes(value)
        is FlowBody -> {
            val buf = MappedByteBuffer.allocate(4096)
            value.collect {
                buf.put(it)
            }
            buf.array().copyOf(buf.position())
        }
    }

fun Body.asFlow(): Flow<ByteArray> =
    when (this) {
        is EmptyBody -> emptyFlow()
        is ByteArrayBody -> flowOf(value)
        is JsonBody -> flow { asBytes() }
        is FlowBody -> value
    }

inline suspend fun <reified T> Body.parse(): T = DatabindCodec.mapper().readValue<T>(asBytes())

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

val Int.millis: Duration get() = toLong().millis
val Long.millis: Duration get() = Duration.ofMillis(this)

val Int.seconds: Duration get() = toLong().seconds
val Long.seconds: Duration get() = Duration.ofSeconds(this)

val Int.minutes: Duration get() = toLong().minutes
val Long.minutes: Duration get() = Duration.ofMinutes(this)

val Int.hours: Duration get() = toLong().hours
val Long.hours: Duration get() = Duration.ofHours(this)

val Int.days: Duration get() = toLong().days
val Long.days: Duration get() = Duration.ofDays(this)
