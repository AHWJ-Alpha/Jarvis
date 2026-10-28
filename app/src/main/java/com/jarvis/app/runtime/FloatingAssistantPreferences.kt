package com.jarvis.app.runtime

import android.content.Context

enum class FloatingShape(val label: String) {
    Circle("圆形"), Rounded("圆角方形"), Square("方形")
}

enum class FloatingGestureAction(val label: String) {
    None("无"), Hide("暂时隐藏"), Rotate("旋转一圈"), Task("执行自定义任务")
}

data class FloatingAssistantPreferences(
    val shape: FloatingShape = FloatingShape.Circle,
    val sizeDp: Int = 56,
    val doubleTap: FloatingGestureAction = FloatingGestureAction.Rotate,
    val tripleTap: FloatingGestureAction = FloatingGestureAction.Hide,
    val hideSeconds: Int = 15,
    val doubleTapTask: String = "",
    val tripleTapTask: String = ""
) {
    companion object {
        private const val PREFS = "jarvis_floating_appearance"

        fun load(context: Context): FloatingAssistantPreferences {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return FloatingAssistantPreferences(
                shape = FloatingShape.entries.getOrElse(p.getInt("shape", 0)) { FloatingShape.Circle },
                sizeDp = p.getInt("sizeDp", 56).coerceIn(44, 88),
                doubleTap = FloatingGestureAction.entries.getOrElse(p.getInt("doubleTap", 2)) { FloatingGestureAction.Rotate },
                tripleTap = FloatingGestureAction.entries.getOrElse(p.getInt("tripleTap", 1)) { FloatingGestureAction.Hide },
                hideSeconds = p.getInt("hideSeconds", 15).coerceIn(3, 300),
                doubleTapTask = p.getString("doubleTapTask", "").orEmpty().take(4000),
                tripleTapTask = p.getString("tripleTapTask", "").orEmpty().take(4000)
            )
        }
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("shape", shape.ordinal)
            .putInt("sizeDp", sizeDp.coerceIn(44, 88))
            .putInt("doubleTap", doubleTap.ordinal)
            .putInt("tripleTap", tripleTap.ordinal)
            .putInt("hideSeconds", hideSeconds.coerceIn(3, 300))
            .putString("doubleTapTask", doubleTapTask.take(4000))
            .putString("tripleTapTask", tripleTapTask.take(4000))
            .apply()
    }
}
