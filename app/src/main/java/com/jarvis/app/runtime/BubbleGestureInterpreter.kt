package com.jarvis.app.runtime

import android.os.Handler

/** Defers a single tap briefly so double and triple taps can take precedence. */
internal class BubbleGestureInterpreter(private val intervalMs: Long = 280) {
    private var count = 0
    private var lastTap = 0L
    private var pending: Runnable? = null

    fun tap(now: Long, handler: Handler, action: (Int) -> Unit) {
        if (now - lastTap > intervalMs) count = 0
        lastTap = now
        count++
        pending?.let(handler::removeCallbacks)
        if (count >= 3) {
            count = 0
            pending = null
            action(3)
            return
        }
        val task = Runnable {
            val taps = count
            count = 0
            pending = null
            if (taps in 1..2) action(taps)
        }
        pending = task
        handler.postDelayed(task, intervalMs)
    }

    fun reset(handler: Handler) {
        pending?.let(handler::removeCallbacks)
        count = 0
        pending = null
    }
}
