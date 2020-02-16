package io.kvarto.http.server

import io.kvarto.http.client.impl.toStringMultiMap
import io.kvarto.http.common.*
import io.kvarto.utils.toFlow
import io.kvarto.utils.writeTo
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.opentracing.Tracer
import io.opentracing.tag.Tags
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.*
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext


abstract class HttpApi(
    val vertx: Vertx,
    val securityManager: SecurityManager? = null,
    val tracer: Tracer? = null,
    val registry: MeterRegistry? = null
) {

    abstract fun setup(router: Router)

    fun Route.operationId(id: String): Route = handler { ctx ->
        ctx.put(CTX_PARAM_OPERATION_ID, OperationId(id))
        tracer?.handleTracing(ctx, id)
        registry?.handleMetrics(ctx, id)
        ctx.next()
    }


    fun Route.secure(scope: AuthScope): Route = handler { ctx ->
        if (securityManager != null) {
            securityManager.handleSecurity(ctx, scope)
        } else {
            ctx.next()
        }
    }

    fun Route.handle(requestHandler: suspend (HttpRequest) -> HttpResponse): Route = handler { ctx ->
        createScope(ctx).launch {
            try {
                val response = requestHandler(ctx.toHttpRequest())
                ctx.response().end(response)
            } catch (e: Exception) {
                ctx.fail(e)
            }
        }
    }

    private fun RoutingContext.toHttpRequest(): HttpRequest {
        val req = request()
        val method = HttpMethod.valueOf(req.method().name)
        val headers = req.headers().toStringMultiMap()
        val params = req.params().toStringMultiMap()
        val body = Body(req.toFlow(vertx).map { it.bytes })
        return HttpRequest(URL(req.absoluteURI()), method, headers, params, body)
    }

    private fun SecurityManager.handleSecurity(ctx: RoutingContext, authScope: AuthScope) {
        createScope(ctx).launch {
            try {
                val authorized = hasScope(ctx, authScope)
                if (authorized) {
                    ctx.next()
                } else {
                    ctx.fail(HttpStatus.UNAUTHORIZED)
                }
            } catch (e: Exception) {
                ctx.fail(e)
            }
        }
    }

    private fun createScope(ctx: RoutingContext): CoroutineScope {
        val operationId = ctx.get<OperationId?>(CTX_PARAM_OPERATION_ID) ?: EmptyCoroutineContext
        val currentSpan = ctx.get<CurrentSpan?>(CTX_PARAM_SPAN) ?: EmptyCoroutineContext
        val correlationHeader = ctx.get<CorrelationHeader?>(CTX_PARAM_CORRELATION_HEADER) ?: EmptyCoroutineContext
        return CoroutineScope(vertx.dispatcher() + operationId + currentSpan + correlationHeader)
    }
}

private fun MeterRegistry.handleMetrics(ctx: RoutingContext, operationId: String) {
    val startTime = System.currentTimeMillis()
    ctx.addBodyEndHandler {
        val duration = System.currentTimeMillis() - startTime
        val tags = listOf(
            Tag.of("operationId", operationId),
            Tag.of("method", ctx.request().method().name)
        )
        timer("http.server.responseTime", tags).record(duration, TimeUnit.MILLISECONDS)
    }
}

private fun Tracer.handleTracing(ctx: RoutingContext, operationId: String) {
    val context = extract(ctx)
    val span = buildSpan(operationId)
        .withTag(Tags.SPAN_KIND, Tags.SPAN_KIND_SERVER)
        .asChildOf(context)
        .start() //TODO add more params
    ctx.put(CTX_PARAM_SPAN, CurrentSpan(span))
    ctx.addBodyEndHandler {
        span.finish()
    }
}

private suspend fun HttpServerResponse.end(response: HttpResponse) {
    statusCode = response.status.code
    response.headers.entries().forEach { (name, value) ->
        putHeader(name, value)
    }
    return when (response.body) {
        is EmptyBody -> end()
        is ByteArrayBody -> {
            putHeader("Content-Length", response.body.value.size.toString())
            end(Buffer.buffer(response.body.value))
        }
        is JsonBody -> {
            val bytes = DatabindCodec.mapper().writeValueAsBytes(response.body.value)
            putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            putHeader(HttpHeaders.CONTENT_LENGTH, bytes.size.toString())
            end(Buffer.buffer(bytes))
        }
        is FlowBody -> {
            isChunked = true
            response.body.value.map { Buffer.buffer(it) }.writeTo(this)
            end()
        }
    }
}

fun httpApi(
    vertx: Vertx,
    securityManager: SecurityManager? = null,
    tracer: Tracer? = null,
    registry: MeterRegistry? = null,
    f: HttpApi.(Router) -> Unit
): HttpApi =
    object : HttpApi(vertx, securityManager, tracer, registry) {
        override fun setup(router: Router) {
            f(router)
        }
    }

fun response(body: String): HttpResponse = HttpResponse(body = Body(body))

fun HttpResponse.addHeader(name: String, value: String) = copy(headers = headers.add(name, value))

fun HttpResponse.withStatus(status: HttpStatus) = copy(status = status)
