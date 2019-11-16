package io.kvarto.http.impl

import io.kvarto.http.*
import io.kvarto.http.HttpClient
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.*
import io.vertx.core.http.HttpMethod
import io.vertx.core.streams.ReadStream
import io.vertx.core.streams.WriteStream
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import org.apache.http.client.utils.URIBuilder

internal class VertxHttpClient(val vertx: Vertx, val options: HttpClientOptions) : HttpClient {
    override suspend fun send(request: HttpRequest): HttpResponse {
        val client = vertx.createHttpClient(options)
        val method = HttpMethod.valueOf(request.method.name)
        val options = RequestOptions().apply {
            host = request.url.host
            port = request.url.port.takeIf { it != -1 } ?: request.url.defaultPort
            isSsl = request.url.protocol == "https"
            request.headers.values.forEach { (name, value) ->
                addHeader(name, value)
            }
            uri = URIBuilder(request.url.toURI()).apply {
                request.params.values.forEach { (name, values) ->
                    values.forEach {
                        addParameter(name, it)
                    }
                }
            }.build().toASCIIString()
        }
        val vertxReq = client.request(method, options)
        val writeChannel = (vertxReq as WriteStream<Buffer>).toChannel(vertx)
        request.body.content().collect { elem ->
            writeChannel.send(Buffer.buffer(elem))
        }
        writeChannel.close()
        val responseChannel = (vertxReq as ReadStream<HttpClientResponse>).toChannel(vertx)
        val vertxResponse = responseChannel.receive()
        val status = HttpStatus.fromCode(vertxResponse.statusCode())
        val headers = vertxResponse.headers().entries().map { (name, value) -> name to value }.let(::Headers)

        val responseBodyFlow = flow {
            val ch = vertxResponse.toChannel(vertx)
            for (e in ch) {
                emit(e.bytes)
            }
        }
        return HttpResponse(status, headers, Body(responseBodyFlow))
    }
}


private val DEFAULT_OPTIONS = HttpClientOptions()

fun HttpClient.Companion.create(vertx: Vertx, options: HttpClientOptions = DEFAULT_OPTIONS): HttpClient =
    VertxHttpClient(vertx, options)