package io.kvarto.utils

import com.fasterxml.jackson.module.kotlin.readValue
import io.kvarto.http.common.*
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.core.streams.ReadStream
import io.vertx.core.streams.WriteStream
import io.vertx.core.streams.impl.InboundBuffer
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.apache.http.client.utils.URIBuilder
import java.net.URI
import java.net.URL
import java.nio.MappedByteBuffer
import java.nio.charset.Charset
import java.time.Duration
import kotlin.coroutines.suspendCoroutine


fun URL.resolve(path: String): URL = toURI().resolve(path).toURL()

fun buildUri(url: URL, parameters: StringMultiMap): URI {
    val builder = URIBuilder(url.toURI())
    for ((name, value) in parameters.entries()) {
        builder.addParameter(name, value)
    }
    return builder.build()
}

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

internal fun <T> Flow<T>.toReadStream(vertx: Vertx): ReadStream<T> = FlowReadStream(this, vertx)
internal fun <T> Flow<T>.toChannel(scope: CoroutineScope): ReceiveChannel<T> {
    val ch = Channel<T>()
    scope.launch {
        val cause = runCatching { pumpTo(ch) }.exceptionOrNull()
        ch.close(cause)
    }
    return ch
}

suspend fun <T> Flow<T>.pumpTo(ch: Channel<T>) {
    collect { ch.send(it) }
}

internal class FlowReadStream<T>(flow: Flow<T>, vertx: Vertx) : ReadStream<T> {
    private val context = vertx.orCreateContext
    private val scope = CoroutineScope(context.dispatcher())
    private val ch = flow.toChannel(scope)
    private val buffer = InboundBuffer<T>(context)
    private var exceptionHandler: Handler<Throwable>? = null
    private var endHandler: Handler<Void>? = null

    init {
        pumpNextPortion()
    }

    private fun pumpNextPortion() {
        scope.launch {
            try {
                var ended = false
                while (buffer.isWritable) {
                    val elem = ch.receiveOrNull()
                    if (elem == null) {
                        ended = true
                        break
                    }
                    buffer.write(elem)
                }
                if (ended) {
                    endHandler?.handle(null)
                } else {
                    buffer.drainHandler { pumpNextPortion() }
                }
            } catch (e: Throwable) {
                exceptionHandler?.handle(e)
            }
        }
    }

    @Synchronized
    override fun fetch(amount: Long): ReadStream<T> {
        buffer.fetch(amount)
        return this
    }

    @Synchronized
    override fun pause(): ReadStream<T> {
        buffer.pause()
        return this
    }

    @Synchronized
    override fun resume(): ReadStream<T> {
        buffer.resume()
        return this
    }

    @Synchronized
    override fun handler(handler: Handler<T>?): ReadStream<T> {
        buffer.handler(handler)
        return this
    }

    @Synchronized
    override fun exceptionHandler(handler: Handler<Throwable>?): ReadStream<T> {
        buffer.exceptionHandler(handler)
        exceptionHandler = handler
        return this
    }

    @Synchronized
    override fun endHandler(endHandler: Handler<Void>?): ReadStream<T> {
        this.endHandler = endHandler
        return this
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
