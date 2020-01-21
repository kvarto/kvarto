package io.kvarto.http.client.impl

import io.kvarto.http.client.HttpClient
import io.kvarto.http.common.*
import io.kvarto.utils.writeTo
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.*
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.core.streams.ReadStream
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import org.apache.http.client.utils.URIBuilder
import kotlin.coroutines.suspendCoroutine

internal class VertxHttpClient(val vertx: Vertx, options: HttpClientOptions) : HttpClient {
    val client = vertx.createHttpClient(options)

    override suspend fun send(request: HttpRequest): HttpResponse =
        retry(request.metadata.retry) {
            val vertxRequest = createVertxRequest(request)
            val responseFlow = (vertxRequest as ReadStream<HttpClientResponse>).toChannel(vertx)
            vertxRequest.sendRequest(request.body)
            val vertxResponse = responseFlow.receive()

            val responseBodyFlow = vertxResponse.toChannel(vertx).consumeAsFlow().map { it.bytes }
//            val responseBodyFlow = vertxResponse.toChannel(vertx).toList().asFlow().map { it.bytes }

            val status = HttpStatus.fromCode(vertxResponse.statusCode())
            val headers = StringMultiMap.of(vertxResponse.headers().map { (name, value) -> name to value })

//            val body = awaitEvent<Buffer> { vertxResponse.bodyHandler(it) }
//            val responseBodyFlow = flowOf(body.bytes)

            val response = HttpResponse(status, headers, Body(responseBodyFlow))
            if (response.status !in request.metadata.successStatuses) {
                throw UnexpectedHttpStatusException(response)
            }
            response
        }

    private suspend fun HttpClientRequest.sendRequest(body: Body): Unit =
        when (body) {
            is EmptyBody -> end()
            is ByteArrayBody -> {
                putHeader("Content-Length", body.value.size.toString())
                end(Buffer.buffer(body.value))
            }
            is JsonBody -> {
                val bytes = DatabindCodec.mapper().writeValueAsBytes(body.value)
                putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                putHeader(HttpHeaders.CONTENT_LENGTH, bytes.size.toString())
                end(Buffer.buffer(bytes))
            }
            is FlowBody -> {
                isChunked = true
                body.value.map { Buffer.buffer(it) }.writeTo(this)
                end()
            }
        }

    private suspend fun HttpClientRequest.send(f: HttpClientRequest.() -> Unit): HttpClientResponse =
        suspendCoroutine { cont ->
            var waitingForResult = true
            handler {
                if (waitingForResult) {
                    waitingForResult = false
                    cont.resumeWith(Result.success(it))
                }
            }
            exceptionHandler {
                if (waitingForResult) {
                    waitingForResult = false
                    cont.resumeWith(Result.failure(it))
                }
            }
            f()
        }

    private suspend fun createVertxRequest(request: HttpRequest): HttpClientRequest {
        val options = RequestOptions().apply {
            host = request.url.host
            port = request.url.port.takeIf { it != -1 } ?: request.url.defaultPort
            isSsl = request.url.protocol == "https"
            request.headers.entries().forEach { (name, value) ->
                addHeader(name, value)
            }
            correlationHeader()?.let { (name, value) ->
                if (name !in request.headers) {
                    addHeader(name, value)
                }
            }
            uri = URIBuilder(request.url.toURI()).apply {
                for ((name, value) in request.parameters.entries()) {
                    addParameter(name, value)
                }
            }.build().toASCIIString()
        }
        val method = HttpMethod.valueOf(request.method.name)
        return client.request(method, options).apply {
            setTimeout(request.metadata.timeout.toMillis())
        }
    }
}


val DEFAULT_OPTIONS = HttpClientOptions().apply {
    isTryUseCompression = true
    maxPoolSize = 10
}

fun HttpClient.Companion.create(vertx: Vertx, options: HttpClientOptions = DEFAULT_OPTIONS): HttpClient =
    VertxHttpClient(vertx, options)

class UnexpectedHttpStatusException(val response: HttpResponse):
    RuntimeException("Got ${response.status} ${response.status.code}")


