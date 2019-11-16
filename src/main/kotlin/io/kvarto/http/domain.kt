package io.kvarto.http

import kotlinx.coroutines.flow.*
import java.net.URL


data class HttpRequest(
    val url: URL,
    val method: HttpMethod = HttpMethod.GET,
    val headers: Headers = Headers.EMPTY,
    val params: Params = Params.EMPTY,
    val body: Body = Body.EMPTY,
    val operationId: String? = null,
    val successStatuses: Set<HttpStatus> = SUCCESS_STATUSES
) {
    fun withPath(path: String) = copy(url = url.withPath(path))

    fun withMethod(method: HttpMethod) = copy(method = method)

    fun addQueryParam(name: String, value: String) = copy(params = params.add(name, value))

    fun addHeader(name: String, value: String) = copy(headers = headers.add(name, value))
}


data class HttpResponse(
    val status: HttpStatus = HttpStatus.OK,
    val headers: Headers = Headers.EMPTY,
    val body: Body
)

class Headers(values: Map<String, String>) {
    val values: Map<String, String> = values.toCaseInsensitiveMap()

    operator fun get(name: String): String? = values[name]

    fun add(name: String, value: String): Headers =
        Headers(values.toCaseInsensitiveMap().apply { put(name, value) })

    companion object {
        val EMPTY = Headers(emptyMap())
    }
}

class Params(val values: Map<String, List<String>>) {
    operator fun get(name: String): String? = values[name]?.last()

    fun add(name: String, value: String): Params =
        Params(values + (name to (values[name].orEmpty() + value)))

    companion object {
        val EMPTY = Params(emptyMap())

        fun of(values: Map<String, String>): Params = Params(values.mapValues { listOf(it.value) })
    }
}

interface Body {
    fun content(): Flow<ByteArray>

    companion object {
        operator fun invoke(string: String): Body = flowOf(string.toByteArray()).toBody()

        operator fun invoke(bytes: ByteArray): Body = flowOf(bytes).toBody()

        operator fun invoke(flow: Flow<ByteArray>): Body = flow.toBody()

        val EMPTY = emptyFlow<ByteArray>().toBody()
    }
}

private fun Flow<ByteArray>.toBody() : Body = object : Body {
    override fun content(): Flow<ByteArray> = this@toBody
}


//class Headers(values: Map<String, String>) : Map<String, String> by values.toCaseInsensitiveMap()

//interface CaseInsensitiveMap : Map<String, String>

//interface MultiMap : Map<String, List<String>>



