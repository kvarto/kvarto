package io.kvarto.http.server

import io.kvarto.http.common.HttpRequest
import io.kvarto.http.common.HttpResponse
import io.opentelemetry.metrics.MeterFactory
import io.opentelemetry.trace.Tracer
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.*
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


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
}

private fun HttpServerResponse.end(response: HttpResponse) {
    throw UnsupportedOperationException("not implemented")
}

private fun RoutingContext.toHttpRequest(): HttpRequest {
    throw UnsupportedOperationException("not implemented")
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
