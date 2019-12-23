package io.kvarto.http.client.impl

import io.kvarto.http.common.StringMultiMap
import io.vertx.core.MultiMap


internal class StringMultiMapImpl(
    private val impl: MultiMap = MultiMap.caseInsensitiveMultiMap()
): StringMultiMap { //TODO: replace with more efficient impl

    override fun get(name: String): String? = impl[name]

    override fun getAll(name: String): List<String> = impl.getAll(name)

    override fun values(): List<Pair<String, String>> = impl.map { (name, value) -> name to value }

    override fun add(name: String, value: String): StringMultiMap =
        StringMultiMapImpl().also {
            it.impl.addAll(impl)
            it.impl.add(name, value)
        }

    override fun addAll(params: List<Pair<String, String>>): StringMultiMap =
        StringMultiMapImpl().also {
            it.impl.addAll(impl)
            for ((name, value) in params) {
                it.impl.add(name, value)
            }
        }

    override fun toString(): String = buildString {
        append('{')
        val values = values()
        values.forEachIndexed { i, (name, value) ->
            append('\"')
            append(name)
            append("\": \"")
            append(value)
            append('\"')
            if (i < values.size - 1) {
                append(", ")
            }
        }
        append('}')
    }
}

fun MultiMap.toStringMultiMap(): StringMultiMap = StringMultiMapImpl(this)