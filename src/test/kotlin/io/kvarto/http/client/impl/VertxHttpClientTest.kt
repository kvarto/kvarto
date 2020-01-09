package io.kvarto.http.client.impl

import io.kvarto.http.client.HttpClient
import io.kvarto.http.common.*
import io.kvarto.http.server.*
import io.kvarto.utils.asString
import io.vertx.core.Vertx
import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.test.assertEquals


internal class VertxHttpClientTest {
    val vertx = Vertx.vertx()
    val port = getFreePort()

    @Test
    fun `get success`() = testBlocking {
        println("starting server")
        val api = httpApi(vertx) {
            it.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR.code) { ctx ->
                ctx.failure().printStackTrace()
            }
            it.get("/").handle { req -> response("hello") }
        }
        vertx.startHttpServer(port, api)
        println("server started")

        val client = HttpClient.create(vertx)
        val req = HttpRequest(URL("http://localhost:$port"))
        val response = client.send(req)

        assertEquals(HttpStatus.OK, response.status)
        assertEquals("hello", response.body.asString())
    }
}