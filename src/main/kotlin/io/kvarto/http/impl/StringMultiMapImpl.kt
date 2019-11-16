package io.kvarto.http.impl

import io.kvarto.http.StringMultiMap
import io.vertx.core.MultiMap


internal class StringMultiMapImpl : StringMultiMap { //TODO: replace with more efficient impl
    private val impl: MultiMap = MultiMap.caseInsensitiveMultiMap()

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

    override fun addAll(params: Map<String, String>): StringMultiMap =
        StringMultiMapImpl().also {
            it.impl.addAll(impl)
            it.impl.addAll(params)
        }

    override fun addAll(params: StringMultiMap): StringMultiMap = addAll(params.values())
}