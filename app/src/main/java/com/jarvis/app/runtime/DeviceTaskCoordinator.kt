package com.jarvis.app.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes agent runs that may drive the single Android accessibility session. */
object DeviceTaskCoordinator {
    private val mutex = Mutex()
    @Volatile private var activeLabel: String? = null

    fun activeTask(): String? = activeLabel

    suspend fun <T> run(label: String, block: suspend () -> T): T = mutex.withLock {
        activeLabel = label
        try {
            block()
        } finally {
            activeLabel = null
        }
    }
}

/** A scoped marker lets the capability bridge enforce the separate remote-device permission. */
object RemoteExecutionGuard {
    @Volatile var active: Boolean = false
        private set
    @Volatile var deviceControlAllowed: Boolean = false
        private set

    suspend fun <T> run(allowDeviceControl: Boolean, block: suspend () -> T): T {
        active = true
        deviceControlAllowed = allowDeviceControl
        try {
            return block()
        } finally {
            deviceControlAllowed = false
            active = false
        }
    }
}
