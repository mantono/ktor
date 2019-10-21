/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.features

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.coroutines.*

/**
 * Client HTTP feature that sets up [HttpRequestBuilder.executionContext] and completes it when the pipeline is fully
 * processed.
 */
class HttpRequestLifecycle {
    /**
     * Companion object for feature installation.
     */
    companion object Feature : HttpClientFeature<Unit, HttpRequestLifecycle> {

        override val key: AttributeKey<HttpRequestLifecycle> = AttributeKey("RequestLifecycle")

        override fun prepare(block: Unit.() -> Unit): HttpRequestLifecycle = HttpRequestLifecycle()

        override fun install(feature: HttpRequestLifecycle, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Before) {
                val executionContext = Job(context.executionContext)

                try {
                    context.executionContext = executionContext

                    proceed()
                }
                catch(cause: Throwable) {
                    if (executionContext.isActive) {
                        executionContext.completeExceptionally(cause)
                    }

                    throw cause
                }

                if (executionContext.isActive) {
                    executionContext.complete()
                }
            }
        }
    }
}
