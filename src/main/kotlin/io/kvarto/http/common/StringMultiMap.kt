package io.kvarto.http.common

import io.kvarto.http.client.impl.StringMultiMapImpl

interface StringMultiMap {
    operator fun get(name: String): String?

    fun getAll(name: String): List<String>

    fun values(): List<Pair<String, String>>

    fun add(name: String, value: String): StringMultiMap

    fun addAll(params: List<Pair<String, String>>): StringMultiMap

    companion object {
        val EMPTY: StringMultiMap = StringMultiMapImpl()

        fun of(values: List<Pair<String, String>>) = EMPTY.addAll(values)
    }
}

fun Map<String, String>.toMultiMap(): StringMultiMap = StringMultiMap.EMPTY.addAll(this)

fun StringMultiMap.addAll(params: Map<String, String>): StringMultiMap = addAll(values())

fun StringMultiMap.addAll(params: StringMultiMap): StringMultiMap = addAll(values())

operator fun StringMultiMap.contains(key: String): Boolean = get(key) != null

