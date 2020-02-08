package io.kvarto.http.common

import io.kvarto.http.client.RequestMetadata
import io.kvarto.utils.resolve
import java.net.URL
import java.time.Duration

data class HttpRequest(
    val url: URL,
    val method: HttpMethod = HttpMethod.GET,
    val headers: StringMultiMap = StringMultiMap.EMPTY,
    val parameters: StringMultiMap = StringMultiMap.EMPTY,
    val body: Body = Body.EMPTY,
    val metadata: RequestMetadata = RequestMetadata.EMPTY
) {
    fun withPath(path: String) = copy(url = url.resolve(path))

    fun withMethod(method: HttpMethod) = copy(method = method)

    fun addParameter(name: String, value: String) = copy(parameters = parameters.add(name, value))

    fun addHeader(name: String, value: String) = copy(headers = headers.add(name, value))

    fun withBody(body: Body) = copy(body = body)

    fun withMetadata(metadata: RequestMetadata) = copy(metadata = metadata)

    fun withSuccessStatuses(successStatuses: Set<HttpStatus>) =
        copy(metadata = metadata.copy(successStatuses = successStatuses))

    fun withTimeout(timeout: Duration) = copy(metadata = metadata.copy(timeout = timeout))
}

