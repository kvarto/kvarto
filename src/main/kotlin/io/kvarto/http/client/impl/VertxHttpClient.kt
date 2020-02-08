package io.kvarto.http.client.impl

import io.kvarto.http.client.HttpClient
import io.kvarto.http.common.*
import io.kvarto.utils.buildUri
import io.kvarto.utils.toReadStream
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.RequestOptions
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.kotlin.ext.web.client.*
import kotlinx.coroutines.flow.map

typealias VxRequest = io.vertx.ext.web.client.HttpRequest<Buffer>
typealias VxResponse = io.vertx.ext.web.client.HttpResponse<Buffer>

internal class VertxHttpClient(val vertx: Vertx, options: WebClientOptions) : HttpClient {
    val client = WebClient.create(vertx, options)

    override suspend fun send(request: HttpRequest): HttpResponse =
        retry(request.metadata.retry) {
//            val writeStream = FlowWriteStream<Buffer>(vertx, 128)
            val vertxRequest = createVertxRequest(request)//.`as`(BodyCodec.pipe(writeStream, true))
            val vertxResponse = vertxRequest.sendRequest(request.body)

            val responseBody = vertxResponse.body()?.bytes ?: byteArrayOf()
//            val responseBody = writeStream.asFlow().map { it.bytes }

            val status = HttpStatus.fromCode(vertxResponse.statusCode())
            val headers = StringMultiMap.of(vertxResponse.headers().map { (name, value) -> name to value })

            val response = HttpResponse(status, headers, Body(responseBody))
            if (response.status !in request.metadata.successStatuses) {
                throw UnexpectedHttpStatusException(response)
            }
            response
        }

    private suspend fun VxRequest.sendRequest(body: Body): VxResponse =
        when (body) {
            is EmptyBody -> sendAwait()
            is ByteArrayBody -> sendBufferAwait(Buffer.buffer(body.value))
            is JsonBody -> sendJsonAwait(body.value)
            is FlowBody -> sendStreamAwait(body.value.map { Buffer.buffer(it) }.toReadStream(vertx))
        }

    private suspend fun createVertxRequest(request: HttpRequest): VxRequest {
        val options = RequestOptions().apply {
            host = request.url.host
            port = request.url.port.takeIf { it != -1 } ?: request.url.defaultPort
            isSsl = request.url.protocol == "https"
            uri = buildUri(request.url, request.parameters).toASCIIString()
        }
        val method = HttpMethod.valueOf(request.method.name)
        return client.request(method, options).apply {
            timeout(request.metadata.timeout.toMillis())
            request.headers.entries().forEach { (name, value) ->
                putHeader(name, value)
            }
            correlationHeader()?.let { (name, value) ->
                if (name !in request.headers) {
                    putHeader(name, value)
                }
            }
        }
    }
}


val DEFAULT_OPTIONS = WebClientOptions().apply {
    isTryUseCompression = true
    maxPoolSize = 10
}

fun HttpClient.Companion.create(vertx: Vertx, options: WebClientOptions = DEFAULT_OPTIONS): HttpClient =
    VertxHttpClient(vertx, options)

class UnexpectedHttpStatusException(response: HttpResponse) :
    RuntimeException("Got ${response.status} ${response.status.code}")


