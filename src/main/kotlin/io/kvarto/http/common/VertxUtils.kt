package io.kvarto.http.common

import io.kvarto.http.server.HttpApi
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
    override val key = OperationIdKey
}

object OperationIdKey : CoroutineContext.Key<OperationId>

suspend fun operationId(): OperationId? = coroutineContext[OperationIdKey]
