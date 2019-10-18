/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests.utils.tests

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*

internal fun Application.timeoutTest() {
    routing {
        route("/timeout") {
            get("/with-delay") {
                val delay = call.parameters["delay"]!!.toLong()
                delay(delay)
                call.respondText { "Text" }
            }

            get("/with-stream") {
                val delay = call.parameters["delay"]!!.toLong()
                val response = "Text".toByteArray()
                call.respond(object : OutgoingContent.WriteChannelContent() {
                    override val contentType = ContentType.Application.OctetStream
                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        for (offset in 0..response.size) {
                            delay(delay)
                            channel.writeFully(response, offset, 1)
                            channel.flush()
                        }
                    }
                })
            }
        }
    }
}
