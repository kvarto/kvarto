package io.kvarto.http.common

import io.kvarto.utils.resolve
import java.net.URL

data class HttpRequest(
    val url: URL,
    val method: HttpMethod = HttpMethod.GET,
    val headers: StringMultiMap = StringMultiMap.EMPTY,
    val params: StringMultiMap = StringMultiMap.EMPTY,
    val body: Body = Body.EMPTY
) {
    fun withPath(path: String) = copy(url = url.resolve(path))

    fun withMethod(method: HttpMethod) = copy(method = method)

    fun addParameter(name: String, value: String) = copy(params = params.add(name, value))

    fun addHeader(name: String, value: String) = copy(headers = headers.add(name, value))

    fun withBody(body: Body) = copy(body = body)
}

