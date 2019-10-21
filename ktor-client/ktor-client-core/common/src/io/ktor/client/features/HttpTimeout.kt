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
class HttpTimeout(private val requestTimeout: Long, private val connectTimeout: Long, private val socketTimeout: Long) {
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

        var connectTimeout: Long = 10_000

        var socketTimeout: Long = 10_000

        internal fun build(): HttpTimeout = HttpTimeout(requestTimeout, connectTimeout, socketTimeout)
    }

    /**
     * Companion object for feature installation.
     */
    companion object Feature : HttpClientFeature<Configuration, HttpTimeout> {

        override val key: AttributeKey<HttpTimeout> = AttributeKey("Timeout")

        override fun prepare(block: Configuration.() -> Unit): HttpTimeout = Configuration().apply(block).build()

        override fun install(feature: HttpTimeout, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Before) {
                if (!context.attributes.contains(httpTimeoutAttributesKey)) {
                    context.attributes.put(httpTimeoutAttributesKey, HttpTimeoutAttributes())
                }

                val httpTimeoutAttributes = context.attributes[httpTimeoutAttributesKey]

                if (httpTimeoutAttributes.requestTimeout == null) {
                    httpTimeoutAttributes.requestTimeout = feature.requestTimeout
                }

                if (httpTimeoutAttributes.connectTimeout == null) {
                    httpTimeoutAttributes.connectTimeout = feature.connectTimeout;
                }

                if (httpTimeoutAttributes.socketTimeout == null) {
                    httpTimeoutAttributes.socketTimeout = feature.socketTimeout;
                }

                val requestTimeout = httpTimeoutAttributes.requestTimeout!!
                val killer = launch {
                    delay(requestTimeout)
                    context.executionContext!!.cancel(
                        HttpTimeoutCancellationException(
                            "Request timeout has been expired [${feature.requestTimeout} ms]"
                        )
                    )
                }

                context.executionContext!!.invokeOnCompletion {
                    killer.cancel()
                }
            }
        }
    }
}

/**
 * This exception is thrown by [HttpTimeout] to indicate timeout.
 */
class HttpTimeoutCancellationException(message: String? = null) : CancellationException(message)

class HttpTimeoutAttributes(
    var requestTimeout: Long? = null,
    var connectTimeout: Long? = null,
    var socketTimeout: Long? = null
)

val httpTimeoutAttributesKey = AttributeKey<HttpTimeoutAttributes>("TimeoutAttributes")
