package io.kvarto.http.client.impl

import io.kvarto.http.client.HttpClient
import io.kvarto.http.common.*
import io.kvarto.utils.*
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.RequestOptions
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.ext.web.client.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.vertx.ext.web.client.HttpRequest as VxRequest
import io.vertx.ext.web.client.HttpResponse as VxResponse

internal class VertxHttpClient(val vertx: Vertx, options: WebClientOptions) : HttpClient {
    val client = WebClient.create(vertx, options)

    override suspend fun send(request: HttpRequest): HttpResponse = withContext(vertx.dispatcher()) {
        retry(request.metadata.retry) {
            val rws = ReadWriteStream<Buffer>()
            launch {
                rws.toFlow(vertx).collect {
                    println("collecting body: $it")
                }
            }

            val vertxRequest = createVertxRequest(request).`as`(BodyCodec.pipe(rws, true))
//            val vertxRequest = createVertxRequest(request).`as`(object : BodyCodec<Void> {
//                override fun create(handler: Handler<AsyncResult<BodyStream<Void>>>?) {
//                    println("My bodycodec.create $handler")
//                }
//            })
//            val responseBody = rws.toFlow(vertx).map { it.bytes }
            val responseBody = emptyFlow<ByteArray>()
            val vertxResponse = vertxRequest.sendRequest(request.body)
            println("received vertxResponse $vertxResponse")
            
            val status = HttpStatus.fromCode(vertxResponse.statusCode())
            val headers = StringMultiMap.of(vertxResponse.headers().map { (name, value) -> name to value })

            val response = HttpResponse(status, headers, Body(responseBody))
            if (response.status !in request.metadata.successStatuses) {
                throw UnexpectedHttpStatusException(response)
            }
            response
        }
    }

    private suspend fun <T> VxRequest<T>.sendRequest(body: Body): VxResponse<T> =
        when (body) {
            is EmptyBody -> sendAwait()
            is ByteArrayBody -> sendBufferAwait(Buffer.buffer(body.value))
            is JsonBody -> sendJsonAwait(body.value)
            is FlowBody -> sendStreamAwait(body.value.map { Buffer.buffer(it) }.toReadStream())
        }

    private suspend fun createVertxRequest(request: HttpRequest): VxRequest<Buffer> {
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


