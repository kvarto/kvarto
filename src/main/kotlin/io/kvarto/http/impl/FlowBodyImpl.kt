package io.kvarto.http.impl

import io.kvarto.http.Body
import kotlinx.coroutines.flow.Flow


internal class FlowBodyImpl(val flow: Flow<ByteArray>) : Body {
    override fun content(): Flow<ByteArray> = flow
    override fun toString(): String = "FlowBodyImpl(...)"
}




