package io.kvarto.http.common

import io.kvarto.http.StringMultiMap

data class HttpResponse(
    val status: HttpStatus = HttpStatus.OK,
    val headers: StringMultiMap = StringMultiMap.EMPTY,
    val body: Body = Body.EMPTY
)