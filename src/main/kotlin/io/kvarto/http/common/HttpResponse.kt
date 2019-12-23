package io.kvarto.http.common

data class HttpResponse(
    val status: HttpStatus = HttpStatus.OK,
    val headers: StringMultiMap = StringMultiMap.EMPTY,
    val body: Body = Body.EMPTY
)