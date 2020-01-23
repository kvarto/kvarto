package io.kvarto.http.client.impl

import io.kvarto.http.client.HttpClient
import io.kvarto.http.common.*
import io.kvarto.http.server.*
import io.kvarto.utils.asString
import io.vertx.core.Vertx
import org.junit.jupiter.api.*
import java.net.URL
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VertxHttpClientTest {
    val vertx = Vertx.vertx()
    val port = getFreePort()

    val client = HttpClient.create(vertx)
    val req = HttpRequest(URL("http://localhost:$port"))

    val api = httpApi(vertx) {
        it.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR.code) { ctx ->
            ctx.failure()?.printStackTrace()
            ctx.response().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.code).end()
        }
        it.get("/").handle { req -> response("get ${req.parameters["name"]}") }
        it.get("/error").handle { HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR) }
        it.get("/exception").handle { throw TheDamnTable }
        it.post("/foo").handle { req -> response("post ${req.body.asString()}").withStatus(HttpStatus.ACCEPTED) }
        it.patch("/").handle { req -> response("patch ${req.headers["header1"]}") }
    }


    @BeforeAll
    fun setup() = testBlocking() {
        vertx.startHttpServer(port, api)
    }

    @Test
    fun `GET server error`() = testBlocking {
        val response = client.send(req.withPath("/error").withSuccessStatuses(ALL_STATUSES))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.status)
        assertEquals("", response.body.asString())
    }

    @Test
    fun `GET server exception`() = testBlocking {
        val response = client.send(req.withPath("/exception").withSuccessStatuses(ALL_STATUSES))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.status)
        assertEquals("", response.body.asString())
    }

    @Test
    fun `GET success`() = testBlocking {
        repeat(2) {
            val response = client.send(req.addParameter("name", "kvarto"))
            assertEquals(HttpStatus.OK, response.status)
            assertEquals("get kvarto", response.body.asString())
        }
    }

    @Test
    fun `POST with body success`() = testBlocking {
        val response = client.send(req.withMethod(HttpMethod.POST).withPath("/foo").withBody(Body("kotlin")))
        assertEquals(HttpStatus.ACCEPTED, response.status)
        assertEquals("post kotlin", response.body.asString())
    }

    @Test
    fun `PATCH with header success`() = testBlocking {
        val response = client.send(req.withMethod(HttpMethod.PATCH).addHeader("header1", "value1"))
        assertEquals(HttpStatus.OK, response.status)
        assertEquals("patch value1", response.body.asString())
    }
}