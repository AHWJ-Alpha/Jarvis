package com.jarvis.app.runtime

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Build
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
        return JSONObject().put("active", true)
            .put("package", root?.packageName ?: JSONObject.NULL)
            .put("nodes", root?.let { collect(it) } ?: JSONArray())
    }

    fun click(x: Float, y: Float): Boolean {
        val current = service ?: return false
        val path = Path().apply { moveTo(x, y) }
        return current.dispatchGesture(
            GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 80)).build(),
            null,
            null
        )
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
            val text = item.text?.toString().orEmpty()
            val description = item.contentDescription?.toString().orEmpty()
            if (text.isNotBlank() || description.isNotBlank()) output.put(JSONObject().put("text", text).put("description", description).put("class", item.className ?: ""))
            for (index in 0 until item.childCount) item.getChild(index)?.let(::visit)
        }
        visit(node)
        return output
    }
}

class JarvisAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityController.service = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (AccessibilityController.service === this) AccessibilityController.service = null
        super.onDestroy()
    }
}
