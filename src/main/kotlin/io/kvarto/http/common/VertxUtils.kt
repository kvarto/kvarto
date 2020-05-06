package io.kvarto.http.common

import com.fasterxml.jackson.module.kotlin.readValue
import io.kvarto.http.server.HttpApi
import io.opentracing.*
import io.opentracing.propagation.Format
import io.opentracing.propagation.TextMapAdapter
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.core.streams.ReadStream
import io.vertx.core.streams.WriteStream
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.MappedByteBuffer
import java.nio.charset.Charset
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext


suspend fun Vertx.startHttpServer(
    port: Int,
    vararg apis: HttpApi,
    options: HttpServerOptions = DEFAULT_SERVER_OPTIONS
) {
    val router = Router.router(this)
    apis.forEach { it.setup(router) }
    createHttpServer(options).requestHandler(router).listenAwait(port)
}

fun RoutingContext.fail(status: HttpStatus) {
    fail(status.code)
}

class OperationId(val value: String) : CoroutineContext.Element {
    override val key = Key

    object Key : CoroutineContext.Key<OperationId>
}


suspend fun operationId(): String? = coroutineContext[OperationId.Key]?.value


class CurrentSpan(val value: Span) : CoroutineContext.Element {
    override val key = Key

    object Key : CoroutineContext.Key<CurrentSpan>
}

suspend fun currentSpan(): Span? = coroutineContext[CurrentSpan.Key]?.value


class CorrelationHeader(val name: String, val value: String) : CoroutineContext.Element {
    override val key = Key

    object Key : CoroutineContext.Key<CorrelationHeader>
}

suspend fun correlationHeader(): Pair<String, String>? =
    coroutineContext[CorrelationHeader.Key]?.let { it.name to it.value }

val CTX_PARAM_OPERATION_ID = "io.kvarto.OperationId"

val CTX_PARAM_SPAN = "io.kvarto.CurrentSpan"

val CTX_PARAM_CORRELATION_HEADER = "io.kvarto.CorrelationHeader"

fun Tracer.extract(ctx: RoutingContext): SpanContext? {
    val headers = ctx.request().headers().map { it.key to it.value }.toMap()
    return extract(Format.Builtin.HTTP_HEADERS, TextMapAdapter(headers))
}

fun Router.setCorrelationHeader(name: String) {
    route().handler { ctx ->
        val value: String? = ctx.request().getHeader(name)
        if (value != null) {
            ctx.put(CTX_PARAM_CORRELATION_HEADER, CorrelationHeader(name, value))
            ctx.addHeadersEndHandler {
                ctx.response().putHeader(name, value)
            }
        }
        ctx.next()
    }
}

val DEFAULT_SERVER_OPTIONS = HttpServerOptions()
    .setCompressionSupported(true)
    .setDecompressionSupported(true)


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
