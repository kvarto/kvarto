package io.kvarto.http.server

import io.vertx.ext.web.RoutingContext

interface SecurityManager {
    suspend fun hasScope(ctx: RoutingContext, scope: AuthScope): Boolean
}

data class AuthScope(val value: String)