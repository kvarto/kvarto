package io.kvarto.http.server

import io.kvarto.http.client.impl.toStringMultiMap
import io.kvarto.http.common.*
import io.kvarto.utils.toFlow
import io.opentelemetry.metrics.MeterFactory
import io.opentelemetry.trace.Tracer
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.*
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.net.URL


abstract class HttpApi(
    val vertx: Vertx,
    val securityManager: SecurityManager? = null,
    val tracer: Tracer? = null,
    val meterFactory: MeterFactory? = null
) {

    abstract fun Router.setup()

    fun Route.operationId(name: String): Route = this

    fun Route.secure(scope: String): Route = this

    fun Route.handle(requestHandler: suspend (HttpRequest) -> HttpResponse): Route = handler { ctx ->
        val scope = CoroutineScope(vertx.dispatcher())
        val request: HttpRequest = ctx.toHttpRequest()
        scope.launch {
            val response = requestHandler(request)
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
        val req = this.request()
        val method = HttpMethod.valueOf(req.method().name)
        val headers = req.headers().toStringMultiMap()
        val params = req.params().toStringMultiMap()
        val body = req.toFlow(vertx).map { it.bytes }
        return HttpRequest(URL(req.absoluteURI()), method, headers, params, Body(body))
    }
}



interface SecurityManager {
    
}


suspend fun Vertx.startHttpServer(port: Int, vararg apis: HttpApi) {
    val router = Router.router(this)
    apis.forEach {
        with(it) {
            router.setup()
        }
    }
    awaitResult<HttpServer> { createHttpServer().requestHandler(router).listen(port, it) }
}

//OpenTelemetry.getMeterFactory().get("http.client").gaugeLongBuilder("latency").build().getHandle(emptyList()).set(11)
//OpenTelemetry.getMeterFactory().get("http.client").counterLongBuilder("requests.count").build().getHandle(emptyList()).add(3)
//OpenTelemetry.getMeterFactory().get("http.client").measureLongBuilder("requests.count").build().getHandle(emptyList()).record(7)
//OpenTelemetry.getTracerFactory().get("my.tracer").currentSpan

//path parameters?
//routing for HttpRequest without Router?
//goal: HttpApi as function (HttpRequest) -> HttpResponse