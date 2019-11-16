import io.kvarto.http.HttpClient
import io.kvarto.http.HttpRequest
import io.kvarto.http.impl.create
import io.vertx.core.Vertx
import java.net.URL

class Example {

}

suspend fun main() {
    val vertx = Vertx.vertx()
    val client: HttpClient = HttpClient.create(vertx)
    val request = HttpRequest(URL("https://httpbin.org/get"))
        .addQueryParam("foo", "bar")
        .addHeader("header1", "value1")
    
    val response = client.send(request)
    println(response)
}

private fun createClient(): HttpClient {
    TODO()
}