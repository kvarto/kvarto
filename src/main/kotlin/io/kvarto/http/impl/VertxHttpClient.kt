package io.kvarto.http.impl

import io.kvarto.http.*
import io.kvarto.http.HttpClient
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.*
import io.vertx.core.http.HttpMethod
import io.vertx.core.streams.WriteStream
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.flow.*
import org.apache.http.client.utils.URIBuilder
import kotlin.coroutines.suspendCoroutine

//TODO: add open tracing and metrics
internal class VertxHttpClient(val vertx: Vertx, val options: HttpClientOptions) : HttpClient {
    override suspend fun send(request: HttpRequest): HttpResponse {
        val client = vertx.createHttpClient(options)
        val method = HttpMethod.valueOf(request.method.name)
        val options = RequestOptions().apply {
            host = request.url.host
            port = request.url.port.takeIf { it != -1 } ?: request.url.defaultPort
            isSsl = request.url.protocol == "https"
            request.headers.values().forEach { (name, value) ->
                addHeader(name, value)
            }
            uri = URIBuilder(request.url.toURI()).apply {
                for ((name, value) in request.params.values()) {
                    addParameter(name, value)
                }
            }.build().toASCIIString()
        }
        val vertxReq = client.request(method, options)
        val byteArrayReqBodyFlow = request.body.content().map { Buffer.buffer(it) }
        vertxReq.writeAwait(byteArrayReqBodyFlow)

        val vertxResponse = suspendCoroutine<HttpClientResponse> { cont ->
            vertxReq.handler {
                cont.resumeWith(Result.success(it))
            }
            vertxReq.exceptionHandler {
                cont.resumeWith(Result.failure(it))
            }
            vertxReq.end()
        }
        val status = HttpStatus.fromCode(vertxResponse.statusCode())
        val headers = StringMultiMap.of(vertxResponse.headers().map { (name, value) -> name to value })

        val responseBodyChannel = vertxResponse.toChannel(vertx)
        val responseBodyFlow = flow {
            for (elem in responseBodyChannel) {
                emit(elem.bytes)
            }
        }
        return HttpResponse(status, headers, Body(responseBodyFlow))
    }
}


private val DEFAULT_OPTIONS = HttpClientOptions()

fun HttpClient.Companion.create(vertx: Vertx, options: HttpClientOptions = DEFAULT_OPTIONS): HttpClient =
    VertxHttpClient(vertx, options)

private suspend fun <T> WriteStream<T>.writeAwait(flow: Flow<T>) {
    flow.collect { elem ->
        waitTillWritable()
        write(elem)
    }
}

private suspend fun <T> WriteStream<T>.waitTillWritable() {
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

//private fun <T> ReadStream<T>.toChannel(): ReceiveChannel<T> {
//    var shouldRun = true
//    val stream = this
//    stream.endHandler {
//        shouldRun = false
//    }
//    val ch = Channel<T>()
//    stream.pause()
//    stream.fetch(1)
//    stream.handler { elem ->
//        if ()
//    }
//
//    return ch
//}
//
