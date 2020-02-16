package io.kvarto.utils

import com.fasterxml.jackson.module.kotlin.readValue
import io.kvarto.http.common.*
import io.vertx.core.*
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.core.streams.ReadStream
import io.vertx.core.streams.WriteStream
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.http.client.utils.URIBuilder
import java.net.URI
import java.net.URL
import java.nio.MappedByteBuffer
import java.nio.charset.Charset
import java.time.Duration


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
        suspendCancellableCoroutine<Unit> { cont ->
            drainHandler { cont.resumeWith(Result.success(Unit)) }
            exceptionHandler { cont.resumeWith(Result.failure(it)) }
        }
    }
}

//internal fun <T> ReadStream<T>.toFlow(vertx: Vertx): Flow<T> = toChannel(vertx).consumeAsFlow()

internal fun <T> ReadStream<T>.toFlow(vertx: Vertx): Flow<T> {
    val ch = toChannel(vertx)
    return flow {
        for (elem in ch) {
            emit(elem)
        }
    }
}

//internal suspend fun <T> Flow<T>.toReadStream(): ReadStream<T> = ReadWriteStream<T>().also { writeTo(it) }


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


class ReadWriteStream<T> : ReadStream<T>, WriteStream<T> {
    private var dataHandler: Handler<T>? = null
    private var endHandler: Handler<Void>? = null
    private var drainHandler: Handler<Void>? = null
    private var demand = 0L

    override fun handler(handler: Handler<T>?): ReadWriteStream<T> {
        dataHandler = handler
        return this
    }

    override fun pause(): ReadWriteStream<T> {
        demand = 0
        return this
    }

    override fun resume(): ReadWriteStream<T> {
        demand = Long.MAX_VALUE
        drainHandler?.handle(null)
        return this
    }

    override fun write(data: T): ReadWriteStream<T> = write(data, null)

    override fun write(data: T, handler: Handler<AsyncResult<Void>>?): ReadWriteStream<T> {
        println("ReadWriteStream.write: $data dataHandler: $dataHandler")
        dataHandler?.handle(data)
        if (demand > 0) {
            demand--
        }
        handler?.handle(Future.succeededFuture())
        return this
    }

    override fun end() {
        end(null as Handler<AsyncResult<Void>>?)
    }

    override fun end(handler: Handler<AsyncResult<Void>>?) {
        endHandler?.handle(null)
        handler?.handle(Future.succeededFuture())
    }

    override fun writeQueueFull(): Boolean {
        return demand <= 0L
    }

    override fun endHandler(handler: Handler<Void>?): ReadWriteStream<T> {
        endHandler = handler
        return this
    }

    override fun drainHandler(handler: Handler<Void>?): ReadWriteStream<T> {
        drainHandler = handler
        return this
    }

    override fun setWriteQueueMaxSize(maxSize: Int): ReadWriteStream<T> = this

    override fun exceptionHandler(handler: Handler<Throwable?>?): ReadWriteStream<T> = this

    override fun fetch(amount: Long): ReadWriteStream<T> {
        println("ReadWriteStream.fetch amount: $amount demand: $demand")
        demand += amount
        if (demand < 0) {
            demand = Long.MAX_VALUE
        }
        if (demand > 0) {
            drainHandler?.handle(null)
        }
        return this
    }
}
