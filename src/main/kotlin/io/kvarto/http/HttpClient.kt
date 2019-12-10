package io.kvarto.http


interface HttpClient {
    suspend fun send(request: HttpRequest, context: RequestContext = RequestContext.EMPTY): HttpResponse

    companion object
}
