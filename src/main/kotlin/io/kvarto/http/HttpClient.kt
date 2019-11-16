package io.kvarto.http


interface HttpClient {
    suspend fun send(request: HttpRequest): HttpResponse

    companion object
}
