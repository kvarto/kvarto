package io.kvarto.http.common

data class HttpResponse(
    val status: HttpStatus = HttpStatus.OK,
    val headers: StringMultiMap = StringMultiMap.EMPTY,
    val body: Body = Body.EMPTY
) {
    fun addHeader(name: String, value: String) = copy(headers = headers.add(name, value))

    fun withStatus(status: HttpStatus) = copy(status = status)
}