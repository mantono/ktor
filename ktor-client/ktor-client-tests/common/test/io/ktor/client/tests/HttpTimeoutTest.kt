/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.tests.utils.*
import io.ktor.utils.io.core.*
import kotlin.test.*

class HttpTimeoutTest : ClientLoader() {
    @Test
    fun requestTest(): Unit = clientTest {
        config {
            install(HttpTimeout) { requestTimeout = 100 }
        }

        test { client ->
            val response = client.get<String>("$TEST_SERVER/timeout/with-delay?delay=10")
            assertEquals("Text", response)
        }
    }

    @Test
    fun requestTimeoutTest(): Unit = clientTest {
        config {
            install(HttpTimeout) { requestTimeout = 100 }
        }

        test { client ->
            val e = assertFails {
                client.get<String>("$TEST_SERVER/timeout/with-delay?delay=200")
            }

            assertTrue { e is HttpTimeoutCancellationException }
        }
    }

    @Test
    fun requestStreamTest(): Unit = clientTest {
        config {
            install(HttpTimeout) { requestTimeout = 100 }
        }

        test { client ->
            val response = client.get<ByteArray>("$TEST_SERVER/timeout/with-stream?delay=10")

            assertEquals("Text", String(response))
        }
    }

    @Test
    fun requestTimeoutStreamTest(): Unit = clientTest {
        config {
            install(HttpTimeout) { requestTimeout = 100 }
        }

        test { client ->
            val e = assertFails {
                client.get<ByteArray>("$TEST_SERVER/timeout/with-stream?delay=50")
            }

            assertTrue { e is HttpTimeoutCancellationException }
        }
    }
}
