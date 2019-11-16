package io.kvarto.http

data class HttpResponse(
    val status: HttpStatus = HttpStatus.OK,
    val headers: StringMultiMap = StringMultiMap.EMPTY,
    val body: Body = Body.EMPTY
)