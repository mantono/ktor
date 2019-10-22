/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.tests.utils.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlin.reflect.*
import kotlin.test.*

/**
 * Util function that checks that the specified [exception] is the [expectedCause] itself or caused by it.
 */
private fun assertContainsCause(expectedCause: KClass<out Throwable>, exception: Throwable?) {
    var prevCause: Throwable? = null
    var currCause = exception
    while (currCause != null) {
        if (currCause::class == expectedCause) {
            return
        }

        prevCause = currCause
        currCause = currCause.cause
    }

    fail("Exception expected to have $expectedCause cause, but it doesn't (root cause is $prevCause).")
}

private fun TestClientBuilder<*>.testWithTimeout(timeout: Long, block: suspend (client: HttpClient) -> Unit) {
    test = { client ->
        withTimeout(timeout) {
            block(client)
        }
    }
}

class HttpTimeoutTest : ClientLoader() {
    @Test
    fun getTest() = clientTests {
        config {
            install(HttpTimeout) { requestTimeout = 500 }
        }

        testWithTimeout(5_000) { client ->
            val response = client.get<String>("$TEST_SERVER/timeout/with-delay?delay=10")
            assertEquals("Text", response)
        }
    }

    @Test
    fun getRequestTimeoutTest() = clientTests {
        config {
            install(HttpTimeout) { requestTimeout = 10 }
        }

        testWithTimeout(5_000) { client ->
            val exception = assertFails {
                client.get<String>("$TEST_SERVER/timeout/with-delay?delay=500")
            }

            assertContainsCause(HttpTimeoutCancellationException::class, exception)
        }
    }

    @Test
    fun getWithSeparateReceive() = clientTests {
        config {
            install(HttpTimeout) { requestTimeout = 200 }
        }

        testWithTimeout(5_000) { client ->
            val call = client.call("$TEST_SERVER/timeout/with-delay?delay=10") { method = HttpMethod.Get }
            val res: String = call.receive()

            assertEquals("Text", res)
        }
    }

    @Test
    fun getRequestTimeoutWithSeparateReceive() = clientTests {
        config {
            install(HttpTimeout) { requestTimeout = 200 }
        }

        testWithTimeout(5_000) { client ->
            val call = client.call("$TEST_SERVER/timeout/with-stream?delay=100") { method = HttpMethod.Get }
            val exception = assertFails { call.receive<String>() }

            assertContainsCause(HttpTimeoutCancellationException::class, exception)
        }
    }

    @Test
    fun getStreamTest() = clientTests {
        config {
            install(HttpTimeout) { requestTimeout = 500 }
        }

        testWithTimeout(5_000) { client ->
            val response = client.get<ByteArray>("$TEST_SERVER/timeout/with-stream?delay=10")

            assertEquals("Text", String(response))
        }
    }

    @Test
    fun getStreamRequestTimeoutTest() = clientTests {
        config {
            install(HttpTimeout) { requestTimeout = 500 }
        }

        testWithTimeout(5_000) { client ->
            val exception = assertFails {
                client.get<ByteArray>("$TEST_SERVER/timeout/with-stream?delay=200")
            }

            assertContainsCause(HttpTimeoutCancellationException::class, exception)
        }
    }

    @Test
    fun redirectTest() = clientTests {
        config {
            install(HttpTimeout) { requestTimeout = 500 }
        }

        testWithTimeout(5_000) { client ->
            val response = client.get<String>("$TEST_SERVER/timeout/with-redirect?delay=10&count=2")
            assertEquals("Text", response)
        }
    }

    @Test
    fun redirectRequestTimeoutOnFirstStepTest() = clientTests {
        config {
            install(HttpTimeout) { requestTimeout = 10 }
        }

        testWithTimeout(5_000) { client ->
            val exception = assertFails {
                client.get<String>("$TEST_SERVER/timeout/with-redirect?delay=500&count=5")
            }

            assertEquals(HttpTimeoutCancellationException::class, exception::class)
        }
    }

    @Test
    fun redirectRequestTimeoutOnSecondStepTest() = clientTests {
        config {
            install(HttpTimeout) { requestTimeout = 200 }
        }

        testWithTimeout(5_000) { client ->
            val exception = assertFails {
                client.get<String>("$TEST_SERVER/timeout/with-redirect?delay=250&count=5")
            }

            assertContainsCause(HttpTimeoutCancellationException::class, exception)
        }
    }

    @Test
    fun connectionTimeoutTest() = clientTests {
        config {
            install(HttpTimeout) { connectTimeout = 100 }
        }

        testWithTimeout(5_000) { client ->
            val exception = assertFails {
                client.get<String>("http://www.google.com:81")
            }

//            assertContainsCause(HttpTimeoutCancellationException::class, exception)
        }
    }

    @Test
    fun socketTimeoutTest() = clientTests {
        config {
            install(HttpTimeout) { socketTimeout = 100 }
        }

        testWithTimeout(5_000) { client ->
            val exception = assertFails {
                client.get<String>("$TEST_SERVER/timeout/with-stream?delay=1000")
            }

//            assertContainsCause(HttpTimeoutCancellationException::class, exception)
        }
    }
}
