package io.kvarto.http.server

import io.kvarto.http.client.impl.toStringMultiMap
import io.kvarto.http.common.*
import io.kvarto.utils.toFlow
import io.opentelemetry.metrics.MeterFactory
import io.opentelemetry.trace.Span
import io.opentelemetry.trace.Tracer
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.*
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import java.net.URL


abstract class HttpApi(
    val vertx: Vertx,
    val securityManager: SecurityManager? = null,
    val tracer: Tracer? = null,
    val meterFactory: MeterFactory? = null
) {

    abstract fun Router.setup()

    fun Route.operationId(id: String): Route = handler { ctx ->
        ctx.put(CTX_PARAM_OPERATION_ID, id)
        if (tracer != null) {
//            tracer.extractSpanFromHeaders
            val span = tracer.spanBuilder(id).setSpanKind(Span.Kind.SERVER).startSpan() //add more params
            ctx.put(CTX_PARAM_TRACE, span)
            ctx.addBodyEndHandler {
                span.end()
            }
        }
        if (meterFactory != null) {
            val startTime = System.currentTimeMillis()
            ctx.addBodyEndHandler {
                meterFactory.get("http.server")
                    .measureLongBuilder("responseTime")
                    .setLabelKeys(listOf(ctx.request().method().name, id))
                    .build()
                    .defaultHandle.record(System.currentTimeMillis() - startTime)
            }
        }   
        ctx.next()
    }

    fun Route.secure(scope: AuthScope): Route = handler { ctx ->
        if (securityManager != null) {
            GlobalScope.launch(vertx.dispatcher()) {
                try {
                    val authorized = securityManager.hasScope(ctx, scope)
                    if (authorized) {
                        ctx.next()
                    } else {
                        ctx.fail(HttpStatus.UNAUTHORIZED)
                    }
                } catch (e: Exception) {
                    ctx.fail(e)
                }
            }
        } else {
            ctx.next()
        }
    }

    fun Route.handle(requestHandler: suspend (HttpRequest) -> HttpResponse): Route = handler { ctx ->
        CoroutineScope(vertx.dispatcher()).launch {
            val response = requestHandler(ctx.toHttpRequest())
            ctx.response().end(response)
        }
    }

    suspend fun HttpServerResponse.end(response: HttpResponse) {
        val ch = toChannel(vertx)
        statusCode = response.status.code
        response.headers.values().forEach { (name, value) ->
            putHeader(name, value)
        }
        val length = response.body.contextLength()
        if (length != null) {
            putHeader("Content-Length", length.toString())
        } else {
            putHeader("Transfer-Encoding", "chunked")
        }
        response.body.content().collect {
            ch.send(Buffer.buffer(it))
        }
    }

    fun RoutingContext.toHttpRequest(): HttpRequest {
        val req = request()
        val method = HttpMethod.valueOf(req.method().name)
        val headers = req.headers().toStringMultiMap()
        val params = req.params().toStringMultiMap()
        val body = req.toFlow(vertx).map { it.bytes }
        return HttpRequest(URL(req.absoluteURI()), method, headers, params, Body(body))
    }
}


//OpenTelemetry.getMeterFactory().get("http.client").gaugeLongBuilder("latency").build().getHandle(emptyList()).set(11)
//OpenTelemetry.getMeterFactory().get("http.client").counterLongBuilder("requests.count").build().getHandle(emptyList()).add(3)
//OpenTelemetry.getMeterFactory().get("http.client").measureLongBuilder("requests.count").build().getHandle(emptyList()).record(7)
//OpenTelemetry.getTracerFactory().get("my.tracer").currentSpan

//path parameters?
//routing for HttpRequest without Router?
//goal: HttpApi as function (HttpRequest) -> HttpResponse

val CTX_PARAM_OPERATION_ID = "io.kvarto.OperationId"
val CTX_PARAM_TRACE = "io.kvarto.trace"