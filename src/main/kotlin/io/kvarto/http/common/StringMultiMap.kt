package io.kvarto.http.common

import io.kvarto.http.client.impl.StringMultiMapImpl

interface StringMultiMap {
    operator fun get(name: String): String?

    fun getAll(name: String): List<String>

    fun entries(): List<Pair<String, String>>

    fun add(name: String, value: String): StringMultiMap

    operator fun plus(params: List<Pair<String, String>>): StringMultiMap

    companion object {
        val EMPTY: StringMultiMap = StringMultiMapImpl()

        fun of(values: List<Pair<String, String>>) = EMPTY + values
    }
}

fun Map<String, String>.toMultiMap(): StringMultiMap = StringMultiMap.EMPTY + this

operator fun StringMultiMap.plus(other: Map<String, String>): StringMultiMap = plus(other.toList())

operator fun StringMultiMap.plus(other: StringMultiMap): StringMultiMap = plus(other.entries())

operator fun StringMultiMap.contains(key: String): Boolean = get(key) != null

