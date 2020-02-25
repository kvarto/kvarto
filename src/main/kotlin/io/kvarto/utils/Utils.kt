package io.kvarto.utils

import io.kvarto.http.common.StringMultiMap
import org.apache.http.client.utils.URIBuilder
import java.net.URI
import java.net.URL
import java.time.Duration


fun URL.resolve(path: String): URL = toURI().resolve(path).toURL()

fun buildUri(url: URL, parameters: StringMultiMap): URI {
    val builder = URIBuilder(url.toURI())
    for ((name, value) in parameters.entries()) {
        builder.addParameter(name, value)
    }
    return builder.build()
}

val Int.millis: Duration get() = toLong().millis
val Long.millis: Duration get() = Duration.ofMillis(this)

val Int.seconds: Duration get() = toLong().seconds
val Long.seconds: Duration get() = Duration.ofSeconds(this)

val Int.minutes: Duration get() = toLong().minutes
val Long.minutes: Duration get() = Duration.ofMinutes(this)

val Int.hours: Duration get() = toLong().hours
val Long.hours: Duration get() = Duration.ofHours(this)

val Int.days: Duration get() = toLong().days
val Long.days: Duration get() = Duration.ofDays(this)
