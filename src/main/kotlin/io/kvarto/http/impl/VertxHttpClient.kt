package io.kvarto.http.impl

import io.kvarto.http.*
import io.kvarto.http.HttpClient
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.*
import io.vertx.core.http.HttpMethod
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.apache.http.client.utils.URIBuilder
import kotlin.coroutines.suspendCoroutine

//TODO: add open tracing and metrics
internal class VertxHttpClient(val vertx: Vertx, options: HttpClientOptions) : HttpClient {
    val client = vertx.createHttpClient(options)

    override suspend fun send(request: HttpRequest, context: RequestContext): HttpResponse {
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
        vertxReq.setTimeout(context.timeout.toMillis())
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


