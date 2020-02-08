package io.kvarto.utils

import com.fasterxml.jackson.module.kotlin.readValue
import io.kvarto.http.common.*
import io.vertx.core.*
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
    stream.end()
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

internal fun <T> Flow<T>.toChannel(scope: CoroutineScope): ReceiveChannel<T> {
    val ch = Channel<T>()
    scope.launch {
        val cause = runCatching {
            collect {
                ch.send(it)
            }
        }.exceptionOrNull()
        ch.close(cause)
    }
    return ch
}

internal class FlowWriteStream<T>(vertx: Vertx, capacity: Int) : WriteStream<T> {
    private val context = vertx.orCreateContext
    private val scope = CoroutineScope(context.dispatcher())
    private val buffer = InboundBuffer<T>(context, capacity.toLong())
    private var exceptionHandler: Handler<Throwable>? = null
    private var endHandler: Handler<AsyncResult<Void>>? = null

    override fun setWriteQueueMaxSize(maxSize: Int): WriteStream<T> {
        return this
    }

    override fun writeQueueFull(): Boolean = !buffer.isWritable

    override fun write(data: T): WriteStream<T> = write(data, null)

    override fun write(data: T, handler: Handler<AsyncResult<Void>>?): WriteStream<T> {
        buffer.write(data)
        handler?.handle(Future.succeededFuture())
        return this
    }

    override fun end() {
        end(null as Handler<AsyncResult<Void>>?)
    }

    override fun end(handler: Handler<AsyncResult<Void>>?) {
//        channel.close()
        endHandler = handler
    }

    override fun drainHandler(handler: Handler<Void>?): WriteStream<T> {
        buffer.drainHandler(handler)
        return this
    }

    override fun exceptionHandler(handler: Handler<Throwable>?): WriteStream<T> {
        exceptionHandler = handler
        buffer.exceptionHandler(handler)
        return this
    }

    fun asFlow(): Flow<T> {
        TODO()
//        buffer
//        channel.consumeAsFlow()
    }
}

internal fun <T> Flow<T>.toReadStream(vertx: Vertx): ReadStream<T> {
    val context = vertx.orCreateContext
    val scope = CoroutineScope(context.dispatcher())
    val channel = toChannel(scope)
    val buffer = InboundBuffer<T>(context)
    val readStream = BufferReadStream(buffer)

    fun pumpNextPortion() {
        scope.launch {
            try {
                var ended = false
                while (buffer.isWritable) {
                    val elem = channel.receiveOrNull()
                    if (elem == null) {
                        ended = true
                        break
                    }
                    buffer.write(elem)
                }
                if (ended) {
                    readStream.endHandler?.handle(null)
                } else {
                    buffer.drainHandler { pumpNextPortion() }
                }
            } catch (e: Throwable) {
                readStream.exceptionHandler?.handle(e)
            }
        }
    }
    pumpNextPortion()
    return readStream
}

internal class BufferReadStream<T>(val buffer: InboundBuffer<T>): ReadStream<T> {
    internal var endHandler: Handler<Void>? = null
    internal var exceptionHandler: Handler<Throwable>? = null

    override fun fetch(amount: Long): ReadStream<T> {
        buffer.fetch(amount)
        return this
    }

    override fun pause(): ReadStream<T> {
        buffer.pause()
        return this
    }

    override fun resume(): ReadStream<T> {
        buffer.resume()
        return this
    }

    override fun handler(handler: Handler<T>?): ReadStream<T> {
        buffer.handler(handler)
        return this
    }

    override fun exceptionHandler(handler: Handler<Throwable>?): ReadStream<T> {
        exceptionHandler = handler
        buffer.exceptionHandler(handler)
        return this
    }

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
