package io.kvarto.http

import java.net.URL
import java.util.TreeMap


fun Map<String, String>.toCaseInsensitiveMap() =
    TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER).also { it.putAll(this) }

fun URL.withPath(path: String): URL = toURI().resolve(path).toURL()
