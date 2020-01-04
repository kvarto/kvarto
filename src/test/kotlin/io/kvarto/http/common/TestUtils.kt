package io.kvarto.http.common

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.DefaultAsserter.fail


fun testBlocking(f: suspend () -> Unit) = runBlocking<Unit> {
    withTimeout(20_000) { f() }
}

inline fun <reified T: Throwable> assertThrows(f: () -> Unit): T {
    try {
        f()
        fail("Expected ${T::class.qualifiedName} to be thrown but there were no exception")
    } catch (e: Exception) {
        if (e !is T) {
            fail("Expected ${T::class.qualifiedName} to be thrown but $e was thrown instead")
        }
        return e
    }
}

object TheDamnTable : Exception("(╯°□°)╯︵ ┻━┻")