package io.kvarto.http.client.impl

import io.kvarto.http.common.Body
import kotlinx.coroutines.flow.Flow


internal class FlowBodyImpl(val contextLength: Int?, val flow: Flow<ByteArray>) : Body {
    override fun contextLength(): Int? = contextLength
    override fun content(): Flow<ByteArray> = flow
    override fun toString(): String = "FlowBodyImpl(...)"
}




