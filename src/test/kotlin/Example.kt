import io.kvarto.http.client.HttpClient
import io.kvarto.http.client.RequestMetadata
import io.kvarto.http.client.impl.create
import io.kvarto.http.common.*
import io.kvarto.http.server.AuthScope
import io.kvarto.http.server.HttpApi
import io.kvarto.utils.*
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.URL


suspend fun main() {
    val vertx = Vertx.vertx()
    testServer(vertx)
    testHttpBin(vertx)
    Thread.sleep(1000)
    vertx.close()
}

private suspend fun testServer(vertx: Vertx) {
    val api: HttpApi = object : HttpApi(vertx) {
        override fun Router.setup() {
            get("/").operationId("op1").secure(AuthScope("my_scope.read")).handle { req ->
                HttpResponse(body = Body("hello ${req.params["name"]}"))
            }
        }
    }
    vertx.startHttpServer(8080, api)

    val client = HttpClient.create(vertx)
    val response = client.send(HttpRequest(URL("http://localhost:8080")).addParameter("name", "Misha"))
    println(response)
    println(response.body.asString())
}

private suspend fun testHttpBin(vertx: Vertx) {
    val client = HttpClient.create(vertx)
    val request = HttpRequest(URL("https://httpbin.org/get?baz=bro"))
        .addParameter("foo", "bar")
        .addHeader("header1", "value1")
        .withMetadata(RequestMetadata(timeout = 10.seconds))

    println("about to send $request")
    val response = client.send(request)
    val responseBytes = response.body.asBytes()
//    val body = response.body.asString()
    val body = String(responseBytes)
    println("done: $response")
    println(body)
    println("Content-Length: " + response.headers["cOntent-length"])
    println("Body size: ${responseBytes.size}")
//    val url = URL("https://ya.ru/foo/bar/")
//    println(url.resolve("/baz/dor?a=b"))
}


fun testOperationId(vertx: Vertx) {
    val scope = CoroutineScope(vertx.dispatcher() + OperationId("my_op"))
    scope.launch {
        println(operationId()?.value)
    }
}

