package io.kvarto.http.client.impl

import io.kvarto.http.client.HttpClient
import io.kvarto.http.common.*
import io.kvarto.utils.*
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.*
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class VertxHttpClient(val vertx: Vertx, options: HttpClientOptions) : HttpClient {
    val client = vertx.createHttpClient(options)

    override suspend fun send(request: HttpRequest): HttpResponse = withContext(vertx.dispatcher()) {
        retry(request.metadata.retry) {
            val options = RequestOptions().apply {
                host = request.url.host
                port = request.url.port.takeIf { it != -1 } ?: request.url.defaultPort
                isSsl = request.url.protocol == "https"
                uri = buildUri(request.url, request.parameters).toASCIIString()
                request.headers.entries().forEach { (name, value) ->
                    addHeader(name, value)
                }
                correlationHeader()?.takeIf { it.first !in request.headers }?.let { addHeader(it.first, it.second) }
            }
            val method = HttpMethod.valueOf(request.method.name)
            val ch = Channel<HttpResponse>()
            val vertxRequest = client.request(method, options) { vertxResponse ->
                val status = HttpStatus.fromCode(vertxResponse.statusCode())
                val headers = StringMultiMap.of(vertxResponse.headers().map { (name, value) -> name to value })
                val responseBodyFlow = vertxResponse.toFlow(vertx).map { it.bytes }
                val response = HttpResponse(status, headers, Body(responseBodyFlow))
                launch { ch.send(response) }
            }
            vertxRequest.setTimeout(request.metadata.timeout.toMillis())
            vertxRequest.sendRequest(request.body)
            val response = ch.receive()
            if (response.status !in request.metadata.successStatuses) {
                throw UnexpectedHttpStatusException(response)
            }
            response
        }
    }

    private suspend fun HttpClientRequest.sendRequest(body: Body) {
        when (body) {
            is ByteArrayBody -> {
                putHeader("Content-Length", body.value.size.toString())
                write(Buffer.buffer(body.value))
            }
            is JsonBody -> {
                val bytes = DatabindCodec.mapper().writeValueAsBytes(body.value)
                putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                putHeader(HttpHeaders.CONTENT_LENGTH, bytes.size.toString())
                write(Buffer.buffer(bytes))
            }
            is FlowBody -> {
                isChunked = true
                body.value.map { Buffer.buffer(it) }.writeTo(this)
            }
        }
        end()
    }
}


val DEFAULT_OPTIONS = HttpClientOptions().apply {
    isTryUseCompression = true
    maxPoolSize = 10
    isKeepAlive = true
}

fun HttpClient.Companion.create(vertx: Vertx, options: HttpClientOptions = DEFAULT_OPTIONS): HttpClient =
    VertxHttpClient(vertx, options)

class UnexpectedHttpStatusException(response: HttpResponse) :
    RuntimeException("Got ${response.status} ${response.status.code}")


