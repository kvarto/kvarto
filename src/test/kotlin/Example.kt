import io.kvarto.http.resolve
import java.net.URL

class Example {

}

suspend fun main() {
//    val vertx = Vertx.vertx()
//    val client: HttpClient = HttpClient.create(vertx)
//    val request = HttpRequest(URL("https://httpbin.org/get?baz=bro"))
//        .addQueryParam("foo", "bar")
//        .addHeader("header1", "value1")
//
//    println("about to send $request")
//    val response = client.send(request)
//    val responseBytes = response.body.asBytes()
////    val body = response.body.asString()
//    val body = String(responseBytes)
//    println("done: $response")
//    println(body)
//    println("Content-Length: " + response.headers["cOntent-length"])
//    println("Body size: ${responseBytes.size}")
    val url = URL("https://ya.ru/foo/bar/")
    println(url.resolve("/baz/dor?a=b"))
}

