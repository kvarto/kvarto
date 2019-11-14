import io.kvarto.http.HttpClient
import io.kvarto.http.HttpRequest
import java.net.URL

class Example {

}

suspend fun main() {
    val client: HttpClient = createClient()
    val request = HttpRequest(URL("https://httpbin.org/get"))
        .addParameter("foo", "bar")
        .addHeader("header1", "value1")
    
    val response = client.send(request)
    println(response)
}

private fun createClient(): HttpClient {
    TODO()
}