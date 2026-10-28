package com.jarvis.app.runtime

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Build
import android.os.Bundle
import android.graphics.Rect
import android.graphics.Bitmap
import androidx.annotation.RequiresApi
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.TextRecognition
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object AccessibilityController {
    @Volatile var service: JarvisAccessibilityService? = null

    fun inspect(): JSONObject {
        val current = service ?: return JSONObject().put("active", false)
        val root = current.rootInActiveWindow
        val bounds = Rect().also { root?.getBoundsInScreen(it) }
        val result = JSONObject().put("active", true)
            .put("package", root?.packageName ?: JSONObject.NULL)
            .put("bounds", JSONObject().put("left", bounds.left).put("top", bounds.top)
                .put("right", bounds.right).put("bottom", bounds.bottom))
            .put("nodes", root?.let { collect(it) } ?: JSONArray())
        return result.put("fingerprint", OperationMemory.fingerprint(result))
    }

    /** Match exactly one live node; never silently fall back to a different field or old coordinate. */
    fun perform(arguments: JSONObject): CapabilityResult {
        val current = service ?: return CapabilityResult(false, "需要启用无障碍服务")
        val root = current.rootInActiveWindow ?: return CapabilityResult(false, "当前窗口不可读取")
        if (root.packageName?.toString() != arguments.optString("package")) return CapabilityResult(false, "当前应用不匹配")
        val selector = arguments.optJSONObject("selector") ?: return CapabilityResult(false, "缺少 selector")
        val found = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            if (node.isVisibleToUser && OperationMemory.matches(describe(node), selector)) found.add(node)
            for (i in 0 until node.childCount) node.getChild(i)?.let(::visit)
        }
        visit(root)
        if (found.size != 1) return CapabilityResult(false, "选择器必须恰好匹配一个可见控件，实际 ${found.size} 个")
        val target = found.single()
        val ok = when (arguments.optString("action")) {
            "input" -> target.isEditable && arguments.optString("text").length <= 4000 && target.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply { putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, arguments.optString("text")) })
            "click" -> {
                var clickable: AccessibilityNodeInfo? = target
                while (clickable != null && !clickable.isClickable) clickable = clickable.parent
                if (clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) true else {
                    val bounds = Rect().also { target.getBoundsInScreen(it) }
                    bounds.width() > 0 && bounds.height() > 0 && gesture(bounds.exactCenterX(), bounds.exactCenterY())
                }
            }
            else -> return CapabilityResult(false, "action 仅支持 click 或 input")
        }
        return CapabilityResult(ok, if (ok) "已对唯一匹配控件执行操作；需核实后续界面" else "目标控件拒绝操作")
    }

    fun swipe(arguments: JSONObject): Boolean = gesture(arguments.optDouble("x").toFloat(), arguments.optDouble("y").toFloat(),
        arguments.optDouble("endX").toFloat(), arguments.optDouble("endY").toFloat(), arguments.optLong("durationMs", 350).coerceIn(100, 1500))

    private fun gesture(x: Float, y: Float, endX: Float = x, endY: Float = y, duration: Long = 80): Boolean {
        val current = service ?: return false
        val latch = CountDownLatch(1)
        var completed = false
        val path = Path().apply { moveTo(x, y); if (x != endX || y != endY) lineTo(endX, endY) }
        val submitted = current.dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, duration)).build(),
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) { completed = true; latch.countDown() }
                override fun onCancelled(gestureDescription: GestureDescription?) { latch.countDown() }
            }, null)
        return submitted && latch.await(duration + 2000, TimeUnit.MILLISECONDS) && completed
    }

    fun click(x: Float, y: Float): Boolean {
        return gesture(x, y)
    }

    fun input(text: String): Boolean {
        val root = service?.rootInActiveWindow ?: return false
        fun editable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isEditable) return node
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { editable(it)?.let { found -> return found } }
            }
            return null
        }
        val target = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable }
            ?: editable(root) ?: return false
        return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,
            Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) })
    }

    fun global(action: String): Boolean {
        val id = when (action.lowercase()) {
            "home" -> AccessibilityService.GLOBAL_ACTION_HOME
            "back" -> AccessibilityService.GLOBAL_ACTION_BACK
            "recents" -> AccessibilityService.GLOBAL_ACTION_RECENTS
            "notifications" -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            else -> return false
        }
        return service?.performGlobalAction(id) == true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun screenshotOcr(): JSONObject {
        val current = service ?: return JSONObject().put("active", false)
        val latch = CountDownLatch(1)
        val result = JSONObject().put("active", true).put("text", "")
        val executor = Executors.newSingleThreadExecutor()
        current.takeScreenshot(0, executor, object : AccessibilityService.TakeScreenshotCallback {
            override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                var bitmap: Bitmap? = null
                try {
                    bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)?.copy(Bitmap.Config.ARGB_8888, false)
                        ?: error("screenshot bitmap unavailable")
                    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                    val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)), 10, TimeUnit.SECONDS).text
                    result.put("text", text)
                    recognizer.close()
                } catch (error: Exception) {
                    result.put("error", error.message ?: "OCR failed")
                } finally {
                    runCatching { screenshot.hardwareBuffer.close() }
                    bitmap?.recycle()
                    latch.countDown()
                }
            }

            override fun onFailure(errorCode: Int) {
                result.put("error", "screenshot failed: $errorCode")
                latch.countDown()
            }
        })
        latch.await(12, TimeUnit.SECONDS)
        executor.shutdownNow()
        return result
    }

    private fun collect(node: AccessibilityNodeInfo): JSONArray {
        val output = JSONArray()
        fun visit(item: AccessibilityNodeInfo) {
            if (output.length() >= 200) return
            val text = item.text?.toString().orEmpty()
            val description = item.contentDescription?.toString().orEmpty()
            if (item.isVisibleToUser && (text.isNotBlank() || description.isNotBlank() || item.isClickable || item.isEditable || item.viewIdResourceName != null)) {
                output.put(describe(item))
            }
            for (index in 0 until item.childCount) item.getChild(index)?.let(::visit)
        }
        visit(node)
        return output
    }

    private fun describe(item: AccessibilityNodeInfo): JSONObject {
        val bounds = Rect().also { item.getBoundsInScreen(it) }
        return JSONObject().put("text", if (item.isPassword) "" else item.text?.toString().orEmpty())
            .put("description", item.contentDescription?.toString().orEmpty()).put("viewId", item.viewIdResourceName ?: "")
            .put("class", item.className ?: "").put("clickable", item.isClickable).put("editable", item.isEditable)
            .put("password", item.isPassword).put("bounds", JSONObject().put("left", bounds.left).put("top", bounds.top)
                .put("right", bounds.right).put("bottom", bounds.bottom))
    }
}

class JarvisAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityController.service = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { current ->
            if (current.eventType in setOf(
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED,
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                )) {
                ForegroundAppTracker.record(current.packageName?.toString())
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (AccessibilityController.service === this) AccessibilityController.service = null
        super.onDestroy()
    }
}
