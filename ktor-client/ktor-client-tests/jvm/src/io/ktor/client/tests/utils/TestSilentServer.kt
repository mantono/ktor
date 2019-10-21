/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests.utils

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import java.net.*

internal class TestSilentServer(val port: Int) {

    private val selector = ActorSelectorManager(Dispatchers.IO)

    private val server = aSocket(selector).tcp().bind(InetSocketAddress(port))

    fun close() {
        server.close()
        selector.close()
    }
}
