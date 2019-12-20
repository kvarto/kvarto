package io.kvarto.http.common

import io.kvarto.http.client.impl.FlowBodyImpl
import kotlinx.coroutines.flow.*

interface Body {
    fun contextLength(): Int?

    fun content(): Flow<ByteArray>

    companion object {
        val EMPTY: Body = FlowBodyImpl(0, emptyFlow())

        operator fun invoke(string: String): Body = invoke(string.toByteArray())

        operator fun invoke(bytes: ByteArray): Body = FlowBodyImpl(bytes.size, flowOf(bytes))

        operator fun invoke(flow: Flow<ByteArray>): Body = FlowBodyImpl(null, flow)
    }
}