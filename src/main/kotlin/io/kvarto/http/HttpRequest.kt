package io.kvarto.http

import java.net.URL

data class HttpRequest(
    val url: URL,
    val method: HttpMethod = HttpMethod.GET,
    val headers: StringMultiMap = StringMultiMap.EMPTY,
    val params: StringMultiMap = StringMultiMap.EMPTY,
    val body: Body = Body.EMPTY
) {
    fun withPath(path: String) = copy(url = url.withPath(path))

    fun withMethod(method: HttpMethod) = copy(method = method)

    fun addQueryParam(name: String, value: String) = copy(params = params.add(name, value))

    fun addHeader(name: String, value: String) = copy(headers = headers.add(name, value))
}

//TODO: add all RequestContext to HttpClient
//val operationId: String? = null,
//val successStatuses: Set<HttpStatus> = SUCCESS_STATUSES
//val retry: RetryConfig = RetryConfig.No_RETRY
//timeout
//circuit breaker
