package io.kvarto.http

import kotlinx.coroutines.flow.collect
import java.net.URL
import java.nio.MappedByteBuffer
import java.nio.charset.Charset


fun URL.withPath(path: String): URL = toURI().resolve(path).toURL()

suspend fun Body.asBytes(): ByteArray {
    val buf = MappedByteBuffer.allocate(4096)
    content().collect {
        buf.put(it)
    }
    return buf.array().copyOf(buf.position())
}

suspend fun Body.asString(charset: Charset = Charsets.UTF_8): String = String(asBytes(), charset)
