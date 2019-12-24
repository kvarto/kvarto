package io.kvarto.http.client

import io.kvarto.http.common.HttpRequest
import io.kvarto.http.common.HttpResponse


interface HttpClient {
    suspend fun send(request: HttpRequest): HttpResponse

    companion object
}
