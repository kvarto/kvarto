package io.kvarto.http.common

import io.kvarto.http.client.impl.FlowBodyImpl
import kotlinx.coroutines.flow.*

interface Body {
    fun content(): Flow<ByteArray>

    companion object {
        val EMPTY: Body = FlowBodyImpl(emptyFlow())

        operator fun invoke(string: String): Body = FlowBodyImpl(flowOf(string.toByteArray()))

        operator fun invoke(bytes: ByteArray): Body = FlowBodyImpl(flowOf(bytes))

        operator fun invoke(flow: Flow<ByteArray>): Body = FlowBodyImpl(flow)
    }
}