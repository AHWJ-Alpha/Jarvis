package com.jarvis.app.runtime

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/** Launches the platform speech UI without opening Jarvis's full chat screen. */
class FloatingVoiceActivity : ComponentActivity() {
    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val transcript = if (result.resultCode == RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim().orEmpty()
        } else ""

        when {
            transcript.isNotBlank() -> FloatingAssistantService.stageVoiceResult(this, transcript)
            result.resultCode == RESULT_CANCELED -> {
                FloatingAssistantController.clearVoiceOrigin()
                FloatingAssistantController.reportLaunchFailure("语音识别已取消。")
            }
            else -> {
                FloatingAssistantController.clearVoiceOrigin()
                FloatingAssistantController.reportLaunchFailure("没有识别到语音内容，请重试或检查系统语音服务。")
            }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出要交给 Jarvis 执行的任务")
        }
        if (intent.resolveActivity(packageManager) == null) {
            FloatingAssistantController.clearVoiceOrigin()
            FloatingAssistantController.reportLaunchFailure("设备未提供语音识别服务；请安装或启用系统语音输入。")
            finish()
            return
        }
        runCatching { speechLauncher.launch(intent) }.onFailure {
            FloatingAssistantController.clearVoiceOrigin()
            FloatingAssistantController.reportLaunchFailure("无法启动语音识别：${it.message ?: "系统服务不可用"}")
            finish()
        }
    }
}
