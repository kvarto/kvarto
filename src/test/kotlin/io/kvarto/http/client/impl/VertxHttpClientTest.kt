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
            it.get("/").handle { req -> response("get ${req.parameters["name"]}") }
            it.post("/foo").handle { req -> response("post ${req.body.asString()}").withStatus(HttpStatus.ACCEPTED) }
            it.patch("/").handle { req -> response("path ${req.headers["header1"]}") }
        }
        vertx.startHttpServer(port, api)
        println("server started")

        val client = HttpClient.create(vertx)
        val req = HttpRequest(URL("http://localhost:$port"))

        run {
            val response = client.send(req.addParameter("name", "kvarto"))
            assertEquals(HttpStatus.OK, response.status)
            assertEquals("get kvarto", response.body.asString())
        }
        run {
            val response = client.send(req.withMethod(HttpMethod.POST).withPath("/foo").withBody(Body("kotlin")))
            assertEquals(HttpStatus.ACCEPTED, response.status)
            assertEquals("post kotlin", response.body.asString())
        }
        run {
            val response = client.send(req.withMethod(HttpMethod.PATCH).addHeader("header1", "value1"))
            assertEquals(HttpStatus.OK, response.status)
            assertEquals("patch value1", response.body.asString())
        }
    }
}