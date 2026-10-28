package com.jarvis.app.runtime

import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

data class FloatingAssistantSnapshot(
    val taskLabel: String = "",
    val operation: String = "",
    val completedOperations: Int = 0,
    val attemptedOperations: Int = 0,
    val busy: Boolean = false,
    val result: String = "",
    val returnPackage: String? = null,
    val showReturnChoice: Boolean = false,
    val hadAccessibilityOperation: Boolean = false
)

/** In-process progress/events shared by the task coordinator, device bridge and overlay. */
object FloatingAssistantController {
    @Volatile
    var snapshot: FloatingAssistantSnapshot = FloatingAssistantSnapshot()
        private set

    private val listeners = CopyOnWriteArrayList<(FloatingAssistantSnapshot) -> Unit>()
    @Volatile private var pendingVoiceOrigin: String? = null
    fun observe(listener: (FloatingAssistantSnapshot) -> Unit) {
        listeners += listener
        listener(snapshot)
    }

    fun removeObserver(listener: (FloatingAssistantSnapshot) -> Unit) {
        listeners -= listener
    }

    fun taskStarted(label: String) {
        val origin = if (label == "悬浮窗对话") pendingVoiceOrigin ?: ForegroundAppTracker.currentPackage()
            else ForegroundAppTracker.currentPackage()
        pendingVoiceOrigin = null
        publish(FloatingAssistantSnapshot(taskLabel = label, busy = true, returnPackage = origin))
    }

    fun rememberVoiceOrigin() {
        pendingVoiceOrigin = ForegroundAppTracker.currentPackage()
    }

    fun clearVoiceOrigin() {
        pendingVoiceOrigin = null
    }

    fun operationStarted(label: String) {
        val old = snapshot
        if (!old.busy) return
        val accessibilityOperation = label.startsWith("accessibility.") || label == "app.launch"
        publish(old.copy(operation = label, result = "", hadAccessibilityOperation = old.hadAccessibilityOperation || accessibilityOperation))
    }

    fun operationFinished(success: Boolean) {
        val old = snapshot
        if (!old.busy) return
        publish(old.copy(
            operation = "",
            attemptedOperations = old.attemptedOperations + 1,
            completedOperations = old.completedOperations + if (success) 1 else 0
        ))
    }

    fun taskFinished(result: String) {
        val old = snapshot
        publish(old.copy(
            operation = "",
            busy = false,
            result = result.take(220),
            showReturnChoice = old.hadAccessibilityOperation && old.returnPackage != null
        ))
    }

    fun dismissReturnChoice() = publish(snapshot.copy(showReturnChoice = false, returnPackage = null, hadAccessibilityOperation = false))

    fun reportLaunchFailure(message: String) {
        Log.w("JarvisFloating", message)
        val old = snapshot
        publish(old.copy(result = message.take(220), showReturnChoice = false))
    }

    fun resetForServiceStart() {
        if (!snapshot.busy && snapshot.showReturnChoice) dismissReturnChoice()
    }

    private fun publish(next: FloatingAssistantSnapshot) {
        snapshot = next
        listeners.forEach { listener -> runCatching { listener(next) } }
    }
}

enum class FloatingLaunchAction { VOICE_TASK, OPEN_MAIN_CHAT }
/** Remembers the last user-facing app seen by the enabled accessibility service. */
object ForegroundAppTracker {
    private const val MAX_AGE_MS = 20 * 60 * 1000L
    @Volatile private var lastPackage: String? = null
    @Volatile private var lastSeenAt: Long = 0L

    fun record(packageName: String?) {
        val name = packageName?.trim().orEmpty()
        if (name.isBlank() || isTransientSystemUi(name)) return
        lastPackage = name
        lastSeenAt = System.currentTimeMillis()
    }

    fun currentPackage(): String? = lastPackage?.takeIf {
        System.currentTimeMillis() - lastSeenAt in 0..MAX_AGE_MS
    }

    fun launch(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun isTransientSystemUi(packageName: String): Boolean {
        val name = packageName.lowercase()
        return name in setOf(
            "com.android.systemui", "com.android.permissioncontroller", "com.google.android.googlequicksearchbox",
            "com.miui.securitycenter", "com.miui.voiceassist", "com.xiaomi.mibrain.speech"
        ) || name.contains("inputmethod") || name.contains("keyboard") || name.contains("speech") || name.contains("voice")
    }
}
