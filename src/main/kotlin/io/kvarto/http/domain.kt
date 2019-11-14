package io.kvarto.http

import kotlinx.coroutines.flow.*
import java.net.URL


data class HttpRequest(
    val url: URL,
    val method: HttpMethod = HttpMethod.GET,
    val headers: Headers = Headers.EMPTY,
    val parameters: Parameters = Parameters.EMPTY,
    val body: Body? = null,
    val operationId: String? = null,
    val successStatuses: Set<HttpStatus> = SUCCESS_STATUSES
) {
    fun addParameter(name: String, value: String) = copy(parameters = parameters.add(name, value))

    fun addHeader(name: String, value: String) = copy(headers = headers.add(name, value))

    fun withPath(path: String) = copy(url = url.withPath(path))
}


data class HttpResponse(
    val status: HttpStatus = HttpStatus.OK,
    val headers: Headers = Headers.EMPTY,
    val body: Body
)

class Headers(values: Map<String, String>) {
    val data: Map<String, String> = values.toCaseInsensitiveMap()

    operator fun get(name: String): String? = data[name]

    fun add(name: String, value: String): Headers =
        Headers(data.toCaseInsensitiveMap().apply { put(name, value) })

    companion object {
        val EMPTY = Headers(emptyMap())
    }
}

class Parameters(val data: Map<String, List<String>>) {
    operator fun get(name: String): String? = data[name]?.last()

    fun add(name: String, value: String): Parameters =
        Parameters(data + (name to (data[name].orEmpty() + value)))

    companion object {
        val EMPTY = Parameters(emptyMap())
        fun of(values: Map<String, String>): Parameters = Parameters(values.mapValues { listOf(it.value) })
    }
}

interface Body {
    fun content(): Flow<ByteArray>

    companion object {
        operator fun invoke(string: String): Body = object : Body {
            override fun content(): Flow<ByteArray> = flowOf(string.toByteArray())
        }
        operator fun invoke(bytes: ByteArray): Body = object : Body {
            override fun content(): Flow<ByteArray> = flowOf(bytes)
        }
        val EMPTY = object : Body {
            override fun content(): Flow<ByteArray> = emptyFlow()
        }
    }
}


//class Headers(values: Map<String, String>) : Map<String, String> by values.toCaseInsensitiveMap()

//interface CaseInsensitiveMap : Map<String, String>

//interface MultiMap : Map<String, List<String>>



