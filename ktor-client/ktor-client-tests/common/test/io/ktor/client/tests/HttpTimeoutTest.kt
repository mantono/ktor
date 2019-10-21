/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.tests.utils.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlin.reflect.*
import kotlin.test.*

private fun assertContainsCause(expected: KClass<out Throwable>, cause: Throwable?) {
    var currCause = cause
    while (currCause != null) {
        if (currCause::class == expected) {
            return
        }

        currCause = currCause.cause
    }

    fail("Exception expected to have $expected cause, but it doesn't.")
}

class HttpTimeoutTest : ClientLoader() {
    @Test
    fun getTest(): Unit = clientTest {
        config {
            install(HttpTimeout) { requestTimeout = 500 }
        }

        test { client ->
            val response = client.get<String>("$TEST_SERVER/timeout/with-delay?delay=10")
            assertEquals("Text", response)
        }
    }

    @Test
    fun getRequestTimeoutTest(): Unit = clientTest {
        config {
            install(HttpTimeout) { requestTimeout = 10 }
        }

        test { client ->
            val e = assertFails {
                client.get<String>("$TEST_SERVER/timeout/with-delay?delay=500")
            }

            assertContainsCause(HttpTimeoutCancellationException::class, e)
        }
    }

    @Test
    fun getWithSeparateReceive(): Unit = clientTests {
        config {
            install(HttpTimeout) { requestTimeout = 200 }
        }

        test { client ->
            val call = client.call("$TEST_SERVER/timeout/with-delay?delay=10") { method = HttpMethod.Get }
            val res: String = call.receive()

            assertEquals("Text", res)
        }
    }

    @Test
    fun getRequestTimeoutWithSeparateReceive(): Unit = clientTests {
        config {
            install(HttpTimeout) { requestTimeout = 200 }
        }

        test { client ->
            val call = client.call("$TEST_SERVER/timeout/with-stream?delay=100") { method = HttpMethod.Get }
            val e = assertFails { call.receive<String>() }

            assertContainsCause(HttpTimeoutCancellationException::class, e)
        }
    }

    @Test
    fun getStreamTest(): Unit = clientTest {
        config {
            install(HttpTimeout) { requestTimeout = 500 }
        }

        test { client ->
            val response = client.get<ByteArray>("$TEST_SERVER/timeout/with-stream?delay=10")

            assertEquals("Text", String(response))
        }
    }

    @Test
    fun getStreamRequestTimeoutTest(): Unit = clientTest {
        config {
            install(HttpTimeout) { requestTimeout = 500 }
        }

        test { client ->
            val e = assertFails {
                client.get<ByteArray>("$TEST_SERVER/timeout/with-stream?delay=200")
            }

            assertContainsCause(HttpTimeoutCancellationException::class, e)
        }
    }

    @Test
    fun redirectTest(): Unit = clientTests {
        config {
            install(HttpTimeout) { requestTimeout = 500 }
        }

        test { client ->
            val response = client.get<String>("$TEST_SERVER/timeout/with-redirect?delay=10&count=2")
            assertEquals("Text", response)
        }
    }

    @Test
    fun redirectRequestTimeoutOnFirstStepTest(): Unit = clientTest {
        config {
            install(HttpTimeout) { requestTimeout = 10 }
        }

        test { client ->
            val e = assertFails {
                client.get<String>("$TEST_SERVER/timeout/with-redirect?delay=500&count=5")
            }

            assertEquals(HttpTimeoutCancellationException::class, e::class)
        }
    }

    @Test
    fun redirectRequestTimeoutOnSecondStepTest(): Unit = clientTest {
        config {
            install(HttpTimeout) { requestTimeout = 500 }
        }

        test { client ->
            val e = assertFails {
                client.get<String>("$TEST_SERVER/timeout/with-redirect?delay=250&count=5")
            }

            assertContainsCause(HttpTimeoutCancellationException::class, e)
        }
    }
}
