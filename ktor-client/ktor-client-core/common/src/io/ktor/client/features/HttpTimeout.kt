/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.features

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.coroutines.*

/**
 * Client HTTP timeout feature.
 * [requestTimeout] - request timeout in milliseconds.
 */
class HttpTimeout(private val requestTimeout: Long) {
    /**
     * [HttpTimeout] configuration that is used during installation
     */
    class Configuration {
        /**
         * Request timeout in milliseconds.
         *
         * Default value is 10000 (10 seconds).
         */
        var requestTimeout: Long = 10_000

        internal fun build(): HttpTimeout = HttpTimeout(requestTimeout)
    }

    /**
     * Companion object for feature installation
     */
    companion object Feature : HttpClientFeature<Configuration, HttpTimeout> {

        override val key: AttributeKey<HttpTimeout> = AttributeKey("Timeout")

        override fun prepare(block: Configuration.() -> Unit): HttpTimeout = Configuration().apply(block).build()

        override fun install(feature: HttpTimeout, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Before) {
                val executionContext = Job(context.executionContext)

                try {
                    context.executionContext = executionContext

                    val killer = launch {
                        delay(feature.requestTimeout)
                        executionContext.cancel(
                            HttpTimeoutCancellationException(
                                "Request timeout has been expired [${feature.requestTimeout} ms]"
                            )
                        )
                    }

                    executionContext.invokeOnCompletion {
                        killer.cancel()
                    }

                    proceed()
                }
                catch (e: Throwable) {
                    if (executionContext.isActive)
                        executionContext.completeExceptionally(e)

                    throw e
                }

                if (executionContext.isActive)
                    executionContext.complete()
            }
        }
    }
}

/**
 * This exception is thrown by [HttpTimeout] to indicate timeout.
 */
class HttpTimeoutCancellationException(message: String? = null) : CancellationException(message)
