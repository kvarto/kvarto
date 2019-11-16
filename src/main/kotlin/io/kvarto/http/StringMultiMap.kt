package io.kvarto.http

import io.kvarto.http.impl.StringMultiMapImpl

interface StringMultiMap {
    operator fun get(name: String): String?

    fun getAll(name: String): List<String>

    fun values(): List<Pair<String, String>>

    fun add(name: String, value: String): StringMultiMap

    fun addAll(params: List<Pair<String, String>>): StringMultiMap

    fun addAll(params: Map<String, String>): StringMultiMap

    fun addAll(params: StringMultiMap): StringMultiMap

    companion object {
        val EMPTY: StringMultiMap = StringMultiMapImpl()
    }
}

fun Map<String, String>.toMultiMap(): StringMultiMap = StringMultiMap.EMPTY.addAll(this)
