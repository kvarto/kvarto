package io.kvarto.http.common

import kotlinx.coroutines.flow.Flow

sealed class Body {
    companion object {
        val EMPTY: Body = EmptyBody

        fun json(content: Any): Body = JsonBody(content)

        operator fun invoke(string: String): Body = invoke(string.toByteArray())

        operator fun invoke(bytes: ByteArray): Body = ByteArrayBody(bytes)

        operator fun invoke(flow: Flow<ByteArray>): Body = FlowBody(flow)
    }
}

internal data class FlowBody(val value: Flow<ByteArray>) : Body()

internal data class ByteArrayBody(val value: ByteArray) : Body()

internal class JsonBody(val value: Any) : Body()

internal object EmptyBody : Body()
