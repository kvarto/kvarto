package io.kvarto.http.client.impl

import io.kvarto.http.client.HttpClient
import io.kvarto.http.common.*
import io.kvarto.utils.toFlow
import io.kvarto.utils.writeTo
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.*
import io.vertx.core.http.HttpMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.apache.http.client.utils.URIBuilder
import kotlin.coroutines.suspendCoroutine

internal class VertxHttpClient(val vertx: Vertx, options: HttpClientOptions) : HttpClient {
    val client = vertx.createHttpClient(options)

    override suspend fun send(request: HttpRequest): HttpResponse =
        retry(request.metadata.retry) {
            val vertxRequest = createVertxRequest(request)
            val vertxResponse = sendRequest(vertxRequest, request.body.content())

            val status = HttpStatus.fromCode(vertxResponse.statusCode())
            val headers = StringMultiMap.of(vertxResponse.headers().map { (name, value) -> name to value })
            val responseBodyFlow = vertxResponse.toFlow(vertx).map { it.bytes }

            HttpResponse(status, headers, Body(responseBodyFlow))
        }

    private suspend fun sendRequest(vertxRequest: HttpClientRequest, body: Flow<ByteArray>): HttpClientResponse {
        body.map { Buffer.buffer(it) }.writeTo(vertxRequest)
        return suspendCoroutine { cont ->
            var waitingForResult = true
            vertxRequest.handler {
                if (waitingForResult) {
                    waitingForResult = false
                    cont.resumeWith(Result.success(it))
                }
            }
            vertxRequest.exceptionHandler {
                if (waitingForResult) {
                    waitingForResult = false
                    cont.resumeWith(Result.failure(it))
                }
            }
            vertxRequest.end()
        }
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
            val length = request.body.length()
            if (length != null) {
                addHeader("Content-Length", length.toString())
            } else {
                addHeader("Transfer-Encoding", "chunked")
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


