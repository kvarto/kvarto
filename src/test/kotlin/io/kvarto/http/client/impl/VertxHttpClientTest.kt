package io.kvarto.http.client.impl

import io.kvarto.*
import io.kvarto.http.client.HttpClient
import io.kvarto.http.common.*
import io.kvarto.http.server.httpApi
import io.kvarto.http.server.response
import io.kvarto.utils.seconds
import io.vertx.core.Vertx
import io.vertx.core.VertxException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.*
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

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
        it.get("/close").handler { it.response().close() }
        it.post("/foo").handle { req -> response("post ${req.body.asString()}").withStatus(HttpStatus.ACCEPTED) }
        it.patch("/").handle { req -> response("patch ${req.headers["header1"]}") }
        it.put("/stream").handle {
            val body = flow {
                repeat(5) {
                    val value = (it + 75).toByte()
                    emit(byteArrayOf(value))
                    delay(50)
                }
            }
            response("").withStatus(HttpStatus.ACCEPTED).copy(body = Body(body))
        }
    }

    @Test
    fun `PUT with stream body success`() = testBlocking {
        val request = req.withMethod(HttpMethod.PUT).withPath("/stream").withBody(Body("ABC")).withTimeout(10.seconds)
        val response = client.send(request)
        assertEquals(HttpStatus.ACCEPTED, response.status)
        val body = response.body.asFlow().toList().map { String(it) }
        assertEquals(listOf("K", "L", "M", "N", "O"), body)
    }

    @ExperimentalTime
    @BeforeAll
    fun setup() = testBlocking {
        println("starting")
        val duration = measureTime {
            vertx.startHttpServer(port, api)
        }
        println("server started $duration")
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
    fun `GET server close connection`() = testBlocking {
        val e = assertFailsWith<VertxException> {
            client.send(req.withPath("/close").withSuccessStatuses(ALL_STATUSES))
        }
        assertEquals("Connection was closed", e.message)
    }

    @Test
    fun `GET success`() = testBlocking {
        repeat(10) {
            println("try #${it + 1}")
            val response = client.send(req.addParameter("name", "kvarto"))
            assertEquals(HttpStatus.OK, response.status)
            assertEquals("get kvarto", response.body.asString())
//            delay(1000)
            client.send(req.addParameter("name", "kvarto"))
        }
    }

    @Test
    fun `POST with body success`() = testBlocking {
        repeat(10) {
            println("try #${it + 1}")
            val response = client.send(req.withMethod(HttpMethod.POST).withPath("/foo").withBody(Body("kotlin")))
            assertEquals(HttpStatus.ACCEPTED, response.status)
            assertEquals("post kotlin", response.body.asString())
        }
    }

    @Test
    fun `PATCH with header success`() = testBlocking {
        val response = client.send(req.withMethod(HttpMethod.PATCH).addHeader("header1", "value1"))
        assertEquals(HttpStatus.OK, response.status)
        assertEquals("patch value1", response.body.asString())
    }

    @Test
    fun `POST with stream body success`() = testBlocking {
        repeat(10) {
            println("try #${it + 1}")
            val body = flow {
                repeat(5) {
                    delay(10)
                    emit(byteArrayOf((it + 65).toByte()))
                }
            }
            val response = client.send(req.withMethod(HttpMethod.POST).withPath("/foo").withBody(Body(body)))
            assertEquals(HttpStatus.ACCEPTED, response.status)
            assertEquals("post ABCDE", response.body.asString())
        }
    }
}