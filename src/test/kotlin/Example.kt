import io.kvarto.http.client.HttpClient
import io.kvarto.http.client.RequestMetadata
import io.kvarto.http.client.impl.create
import io.kvarto.http.common.*
import io.kvarto.http.server.HttpApi
import io.kvarto.http.server.startHttpServer
import io.kvarto.utils.*
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import java.net.URL

class Example {

}

suspend fun main() {
    val vertx = Vertx.vertx()
    val client = HttpClient.create(vertx)
//    testHttpBin(client)
    val api: HttpApi = object : HttpApi(vertx) {
        override fun Router.setup() {
            get("/").operationId("op1").secure("my_scope.read").handle { req: HttpRequest ->
                HttpResponse(body = Body("hello"))
            }
        }
    }
    vertx.startHttpServer(8080, api)

    val response = client.send(HttpRequest(URL("http://localhost:8080")))
    println(response)
    println(response.body.asString())
}

private suspend fun testHttpBin(client: HttpClient) {
    val request = HttpRequest(URL("https://httpbin.org/get?baz=bro"))
        .addQueryParam("foo", "bar")
        .addHeader("header1", "value1")

    println("about to send $request")
    val metadata = RequestMetadata(timeout = 10.seconds)
    val response = client.send(request, metadata)
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

