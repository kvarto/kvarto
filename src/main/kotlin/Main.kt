import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING as d

fun main() {
    println("hello")
    val userId = UserId("foo")
    val mapper = ObjectMapper().registerModule(KotlinModule())
    val str = mapper.writeValueAsString(userId)
    println(str)
    println(mapper.readValue<UserId>(str))
}

data class UserId @JsonCreator(mode = d) constructor(@JsonValue val value: String)