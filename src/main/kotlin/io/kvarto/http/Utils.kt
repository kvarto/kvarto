package io.kvarto.http

import java.net.URL


fun URL.withPath(path: String): URL = toURI().resolve(path).toURL()
