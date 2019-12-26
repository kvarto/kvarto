package io.kvarto.http.common

import io.kvarto.http.server.HttpApi
import io.opentracing.*
import io.opentracing.propagation.Format
import io.opentracing.propagation.TextMapAdapter
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.*
import io.vertx.ext.web.handler.impl.BodyHandlerImpl
import io.vertx.kotlin.coroutines.awaitResult
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext


suspend fun Vertx.startHttpServer(port: Int, vararg apis: HttpApi) {
    val router = Router.router(this)
    apis.forEach {
        with(it) {
            router.setup()
        }
    }
    awaitResult<HttpServer> { createHttpServer().requestHandler(router).listen(port, it) }
}

fun Router.postWithBody(path: String): Route {
    post(path).handler(BodyHandlerImpl())
    return post(path)
}

fun Router.putWithBody(path: String): Route {
    put(path).handler(BodyHandlerImpl())
    return put(path)
}

fun Router.patchWithBody(path: String): Route {
    patch(path).handler(BodyHandlerImpl())
    return patch(path)
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

suspend fun correlationHeader(): Pair<String, String>? = coroutineContext[CorrelationHeader.Key]?.let { it.name to it.value }

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
