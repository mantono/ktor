/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine.jetty

import io.ktor.client.engine.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import org.eclipse.jetty.http2.client.*
import org.eclipse.jetty.util.thread.*
import java.time.*
import java.util.LinkedHashMap

internal class JettyHttp2Engine(
    override val config: JettyEngineConfig
) : HttpClientJvmEngine("ktor-jetty") {

    private val clientCache = ThreadLocal.withInitial {object : LinkedHashMap<HttpTimeoutAttributes, HTTP2Client>() {
        override fun removeEldestEntry(eldest: Map.Entry<HttpTimeoutAttributes, HTTP2Client>): Boolean {
            return size > 10
        }
    }}

    private fun getJettyClient(data: HttpRequestData): HTTP2Client {
        return if (data.attributes.contains(HttpTimeoutAttributes.key)) {
            val httpTimeoutAttributes = data.attributes[HttpTimeoutAttributes.key]
            clientCache.get().computeIfAbsent(httpTimeoutAttributes) {
                HTTP2Client().apply {
                    addBean(config.sslContextFactory)
                    check(config.proxy == null) { "Proxy unsupported in Jetty engine." }

                    executor = QueuedThreadPool().apply {
                        name = "ktor-jetty-client-qtp"
                    }

                    httpTimeoutAttributes.connectTimeout?.let { connectTimeout = it }
                    httpTimeoutAttributes.socketTimeout?.let { idleTimeout = it }

                    start()
                }
            }
        }
        else {
            return HTTP2Client().apply {
                addBean(config.sslContextFactory)
                check(config.proxy == null) { "Proxy unsupported in Jetty engine." }

                executor = QueuedThreadPool().apply {
                    name = "ktor-jetty-client-qtp"
                }

                start()
            }
        }
    }

    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val callContext = createCallContext(data.executionContext)
        return try {
            val jettyClient = getJettyClient(data)
            withContext(callContext) {
                data.executeRequest(jettyClient, config, callContext)
            }
        } catch (cause: Throwable) {
            (callContext[Job] as? CompletableJob)?.completeExceptionally(cause)
            throw cause
        }
    }

    override fun close() {
        super.close()
        coroutineContext[Job]?.invokeOnCompletion {
            clientCache.get().forEach { (_, client) -> client.stop() }
        }
    }
}
