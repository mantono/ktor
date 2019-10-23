/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util.logging

import kotlinx.coroutines.*
import kotlin.coroutines.*

fun Logger.trace(message: String, cause: Throwable? = null) {
    log(Level.TRACE) {
        this.text = message
        this.exception = cause
    }
}

fun Logger.debug(message: String, cause: Throwable? = null) {
    log(Level.DEBUG) {
        this.text = message
        this.exception = cause
    }
}

fun Logger.info(message: String, cause: Throwable? = null) {
    log {
        this.text = message
        this.exception = cause
    }
}

fun Logger.error(message: String, cause: Throwable? = null) {
    log(Level.ERROR) {
        this.text = message
        this.exception = cause
    }
}

fun Logger.warning(message: String, cause: Throwable? = null) {
    log(Level.WARNING) {
        this.text = message
        this.exception = cause
    }
}

inline fun Logger.log(level: Level = Level.INFO, block: LogRecord.() -> Unit) {
    val event = begin(level) ?: return
    try {
        block(event)
        commit(event)
    } finally {
        event.release()
    }
}
