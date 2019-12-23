package io.kvarto.http.client.impl

import io.kvarto.http.client.HttpClient
import io.kvarto.http.client.RequestMetadata
import io.kvarto.http.common.*
import io.kvarto.utils.toFlow
import io.kvarto.utils.writeTo
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.*
import io.vertx.core.http.HttpMethod
import kotlinx.coroutines.flow.map
import org.apache.http.client.utils.URIBuilder
import kotlin.coroutines.suspendCoroutine

//TODO: add open tracing and metrics
internal class VertxHttpClient(val vertx: Vertx, options: HttpClientOptions) : HttpClient {
    val client = vertx.createHttpClient(options)

    override suspend fun send(request: HttpRequest, metadata: RequestMetadata): HttpResponse {
        val method = HttpMethod.valueOf(request.method.name)
        val options = RequestOptions().apply {
            host = request.url.host
            port = request.url.port.takeIf { it != -1 } ?: request.url.defaultPort
            isSsl = request.url.protocol == "https"
            request.headers.values().forEach { (name, value) ->
                addHeader(name, value)
            }
            val length = request.body.contextLength()
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
        val vertxReq = client.request(method, options)
        vertxReq.setTimeout(metadata.timeout.toMillis())
        request.body.content().map { Buffer.buffer(it) }.writeTo(vertxReq)
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

        val responseBodyFlow = vertxResponse.toFlow(vertx).map { it.bytes }
        return HttpResponse(status, headers, Body(responseBodyFlow))
    }
}


private val DEFAULT_OPTIONS = HttpClientOptions()

fun HttpClient.Companion.create(vertx: Vertx, options: HttpClientOptions = DEFAULT_OPTIONS): HttpClient =
    VertxHttpClient(vertx, options)


