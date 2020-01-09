package io.kvarto.http.client.impl

import io.kvarto.http.client.HttpClient
import io.kvarto.http.common.*
import io.kvarto.utils.toFlow
import io.kvarto.utils.writeTo
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.*
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import kotlinx.coroutines.flow.map
import org.apache.http.client.utils.URIBuilder
import kotlin.coroutines.suspendCoroutine

internal class VertxHttpClient(val vertx: Vertx, options: HttpClientOptions) : HttpClient {
    val client = vertx.createHttpClient(options)

    override suspend fun send(request: HttpRequest): HttpResponse =
        retry(request.metadata.retry) {
            val vertxRequest = createVertxRequest(request)
            val vertxResponse = sendRequest(vertxRequest, request.body)

            val status = HttpStatus.fromCode(vertxResponse.statusCode())
            val headers = StringMultiMap.of(vertxResponse.headers().map { (name, value) -> name to value })
            val responseBodyFlow = vertxResponse.toFlow(vertx).map { it.bytes }

            val response = HttpResponse(status, headers, Body(responseBodyFlow))
            if (response.status !in request.metadata.successStatuses) {
                throw UnexpectedHttpStatusException(response)
            }
            response
        }

    private suspend fun sendRequest(vertxRequest: HttpClientRequest, body: Body): HttpClientResponse =
        when (body) {
            is EmptyBody -> vertxRequest.send { end() }
            is ByteArrayBody -> {
                vertxRequest.putHeader("Content-Length", body.value.size.toString())
                vertxRequest.send { end(Buffer.buffer(body.value)) }
            }
            is JsonBody -> {
                val bytes = Json.mapper.writeValueAsBytes(body.value)
                vertxRequest.putHeader("Content-Type", "application/json")
                vertxRequest.putHeader("Content-Length", bytes.size.toString())
                vertxRequest.send { end(Buffer.buffer(bytes)) }
            }
            is FlowBody -> {
                vertxRequest.putHeader("Transfer-Encoding", "chunked")
                body.value.map { Buffer.buffer(it) }.writeTo(vertxRequest)
                vertxRequest.send { end() }
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
            request.headers.values().forEach { (name, value) ->
                addHeader(name, value)
            }
            correlationHeader()?.let { (name, value) ->
                if (name !in request.headers) {
                    addHeader(name, value)
                }
            }
            uri = URIBuilder(request.url.toURI()).apply {
                for ((name, value) in request.params.values()) {
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


private val DEFAULT_OPTIONS = HttpClientOptions()

fun HttpClient.Companion.create(vertx: Vertx, options: HttpClientOptions = DEFAULT_OPTIONS): HttpClient =
    VertxHttpClient(vertx, options)

class UnexpectedHttpStatusException(val response: HttpResponse):
    RuntimeException("Got ${response.status} ${response.status.code}")


