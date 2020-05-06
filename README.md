# Kvarto
**Kvarto** is set of utilities for writing REST services using Kotlin and Vertx. It makes extensive use of coroutines 
and aims at providing clean, minimal, intuitive interface for sending and receiving http requests.  

Kvarto includes http client, http server and various helpers. It supports metrics with `micrometer` and tracing with 
`open-tracing`.

## Http Client
`HttpClient` provides single method `send` that accepts `HttpRequest` and returns `HttpResponse`.
`HttpRequest` is descriptive data structure that represents all data needed to send http request like
url, method, parameters, headers, body and metadata like timeout, circuit breaker config, retries etc. 
`HttpResponse` is descriptive data structure that represents http server response, It contains response status, 
headers and body.
 
Both request and response have streaming support: theie bodies can be converted to `Flow<ByteArray>`. 

### Client example: 
```kotlin
val client = HttpClient.create(vertx)
val request = HttpRequest(URL("http://httpbin.org"))
    .addParameter("foo", "bar")
    .addHeader("Zig", "zag")
    .withTimeout(300.millis)
    .withSuccessStatuses(setOf(HttpStatus.OK, HttpStatus.CONFLICT))

val response = client.send(request)
println(response.body.asString())
```         

## Http Server

