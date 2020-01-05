package io.kvarto.http.client.impl

import io.kvarto.http.common.Body
import kotlinx.coroutines.flow.Flow


internal class FlowBodyImpl(val length: Int?, val flow: Flow<ByteArray>) : Body {
    override fun length(): Int? = length
    override fun content(): Flow<ByteArray> = flow
    override fun toString(): String = "FlowBodyImpl(...)"
}




