package com.jarvis.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.jarvis.app.runtime.BootstrapController
import com.jarvis.app.runtime.BootstrapReport
import com.jarvis.app.runtime.OpenClawProcessManager
import com.jarvis.app.runtime.RuntimeState
import com.jarvis.app.runtime.RuntimeRecoveryManager
import com.jarvis.app.runtime.JarvisBridgeServer
import com.jarvis.app.runtime.JarvisBridgeService
import com.jarvis.app.runtime.RuntimeToolManager
import com.jarvis.app.runtime.RuntimeToolStatus
import com.jarvis.app.runtime.ToolSource
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class JarvisTab(val label: String) { Chat("对话"), Tasks("任务"), Workspace("工作区"), Settings("设置") }
private enum class JarvisTheme(val label: String, val primary: Color, val background: Color, val surface: Color) {
    Deep("深海", Color(0xFF176B87), Color(0xFFF4F7F6), Color.White),
    Graphite("石墨", Color(0xFF46515C), Color(0xFFF2F4F6), Color.White),
    Forest("松林", Color(0xFF2E6B55), Color(0xFFF2F7F3), Color.White),
    Ink("墨黑", Color(0xFFB8D8D4), Color(0xFF101519), Color(0xFF1B2428))
}

private data class ConversationBranch(val title: String, val messages: List<String>)

private enum class RootMode(val label: String) {
    Off("关闭"), Standard("标准"), Full("完全")
}

private val reasoningEfforts = listOf("off", "minimal", "low", "medium", "high", "max")

private fun normalizeReasoningEffort(value: String): String = value.lowercase().trim().let {
    if (it in reasoningEfforts) it else "off"
}

private data class JarvisConfig(
    val restrictionMode: Boolean = true,
    val rootMode: RootMode = RootMode.Standard,
    val commandChannelEnabled: Boolean = false,
    val commandPrefix: String = "/claw",
    val modelBaseUrl: String = "",
    val modelApiKey: String = "",
    val modelName: String = "",
    val modelChain: String = "",
    val deepThinkingEnabled: Boolean = false,
    val reasoningEffort: String = "off",
    val deepThinkingModel: String = "",
    val visionBaseUrl: String = "",
    val visionApiKey: String = "",
    val visionModel: String = "",
    val mentorPrompt: String = "",
    val worldBook: String = "",
    val theme: JarvisTheme = JarvisTheme.Deep
)

private class ConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE)

    fun load(): JarvisConfig = JarvisConfig(
        restrictionMode = prefs.getBoolean("restrictionMode", true),
        rootMode = RootMode.entries.getOrElse(prefs.getInt("rootMode", 1)) { RootMode.Standard },
        commandChannelEnabled = prefs.getBoolean("commandChannelEnabled", false),
        commandPrefix = prefs.getString("commandPrefix", "/claw") ?: "/claw",
        modelBaseUrl = prefs.getString("modelBaseUrl", "") ?: "",
        modelApiKey = prefs.getString("modelApiKey", "") ?: "",
        modelName = prefs.getString("modelName", "") ?: "",
        modelChain = prefs.getString("modelChain", "") ?: "",
        deepThinkingEnabled = prefs.getBoolean("deepThinkingEnabled", false),
        reasoningEffort = normalizeReasoningEffort(prefs.getString("reasoningEffort", "off") ?: "off"),
        deepThinkingModel = prefs.getString("deepThinkingModel", "") ?: "",
        visionBaseUrl = prefs.getString("visionBaseUrl", "") ?: "",
        visionApiKey = prefs.getString("visionApiKey", "") ?: "",
        visionModel = prefs.getString("visionModel", "") ?: "",
        mentorPrompt = prefs.getString("mentorPrompt", "") ?: "",
        worldBook = prefs.getString("worldBook", "") ?: "",
        theme = JarvisTheme.entries.getOrElse(prefs.getInt("theme", 0)) { JarvisTheme.Deep }
    )

    fun save(config: JarvisConfig) {
        val effort = normalizeReasoningEffort(config.reasoningEffort)
        prefs.edit()
            .putBoolean("restrictionMode", config.restrictionMode)
            .putInt("rootMode", config.rootMode.ordinal)
            .putBoolean("commandChannelEnabled", config.commandChannelEnabled)
            .putString("commandPrefix", config.commandPrefix)
            .putString("modelBaseUrl", config.modelBaseUrl)
            .putString("modelApiKey", config.modelApiKey)
            .putString("modelName", config.modelName)
            .putString("modelChain", config.modelChain)
            .putBoolean("deepThinkingEnabled", config.deepThinkingEnabled && effort != "off")
            .putString("reasoningEffort", effort)
            .putString("deepThinkingModel", config.deepThinkingModel)
            .putString("visionBaseUrl", config.visionBaseUrl)
            .putString("visionApiKey", config.visionApiKey)
            .putString("visionModel", config.visionModel)
            .putString("mentorPrompt", config.mentorPrompt)
            .putString("worldBook", config.worldBook)
            .putInt("theme", config.theme.ordinal)
            .apply()
    }
}

private fun JarvisConfig.toAiClassroomJson(): JSONObject = JSONObject().apply {
    put("version", 1)
    put("source", "jarvis")
    put("config", JSONObject().apply {
        put("provider", "OpenAI-compatible")
        put("apiKey", modelApiKey)
        put("baseUrl", modelBaseUrl)
        put("selectedModel", modelName)
        put("customModel", modelName)
        put("modelChain", modelChain.ifBlank { modelName })
        put("deepThinkingEnabled", deepThinkingEnabled && normalizeReasoningEffort(reasoningEffort) != "off")
        put("deepThinkingModel", deepThinkingModel)
        put("reasoningEffort", normalizeReasoningEffort(reasoningEffort))
        put("visionProvider", "OpenAI-compatible")
        put("visionApiKey", visionApiKey)
        put("visionBaseUrl", visionBaseUrl)
        put("visionModel", visionModel)
        put("mentorPrompt", mentorPrompt)
        put("worldBook", worldBook)
    })
}

private fun JSONObject.toJarvisConfig(current: JarvisConfig): JarvisConfig {
    val source = optJSONObject("config") ?: this
    val effort = normalizeReasoningEffort(source.optString("reasoningEffort", current.reasoningEffort))
    val deepEnabled = source.optBoolean("deepThinkingEnabled", current.deepThinkingEnabled) && effort != "off"
    val mainKey = source.optString("apiKey", current.modelApiKey)
    val mainBase = source.optString("baseUrl", current.modelBaseUrl)
    val mainModel = source.optString("selectedModel")
        .ifBlank { source.optString("customModel") }
        .ifBlank { current.modelName }
    return current.copy(
        modelBaseUrl = mainBase,
        modelApiKey = mainKey,
        modelName = mainModel,
        modelChain = source.optString("modelChain", current.modelChain),
        deepThinkingEnabled = deepEnabled,
        reasoningEffort = if (deepEnabled) effort else "off",
        deepThinkingModel = source.optString("deepThinkingModel", current.deepThinkingModel),
        visionBaseUrl = source.optString("visionBaseUrl", mainBase),
        visionApiKey = source.optString("visionApiKey", mainKey),
        visionModel = source.optString("visionModel", current.visionModel.ifBlank { mainModel }),
        mentorPrompt = source.optString("mentorPrompt", current.mentorPrompt),
        worldBook = source.optString("worldBook", current.worldBook)
    )
}

private fun parseAiClassroomConfig(raw: String, current: JarvisConfig): JarvisConfig {
    val trimmed = raw.trim()
    if (trimmed.startsWith("[")) {
        val classes = JSONArray(trimmed)
        for (index in 0 until classes.length()) {
            val candidate = classes.optJSONObject(index)?.optJSONObject("config") ?: continue
            return candidate.toJarvisConfig(current)
        }
        error("未找到课堂配置")
    }
    return JSONObject(trimmed).toJarvisConfig(current)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BootstrapController(this).bootstrap()
        val bridgeIntent = Intent(this, JarvisBridgeService::class.java)
        ContextCompat.startForegroundService(this, bridgeIntent)
        setContent { JarvisApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JarvisApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { ConfigStore(context) }
    val bootstrap = remember { BootstrapController(context) }
    val processManager = remember { OpenClawProcessManager(context) }
    val recoveryManager = remember { RuntimeRecoveryManager(context) }
    val runtimeScope = rememberCoroutineScope()
    var bootstrapReport by remember { mutableStateOf(bootstrap.bootstrap()) }
    var runtimeMessage by remember { mutableStateOf("") }
    var config by remember { mutableStateOf(store.load()) }
    var tab by rememberSaveable { mutableStateOf(JarvisTab.Chat) }
    var draft by rememberSaveable { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf("Jarvis 已启动。你可以从这里开始描述任务。")) }
    var showRootWarning by remember { mutableStateOf(false) }
    var pendingRootMode by remember { mutableStateOf<RootMode?>(null) }
    var showRecovery by remember { mutableStateOf(false) }
    var configNotice by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var showNavigation by rememberSaveable { mutableStateOf(false) }
    var workspaceName by rememberSaveable { mutableStateOf("默认工作区") }
    var branches by remember { mutableStateOf(listOf(ConversationBranch("主对话", messages))) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    fun update(next: JarvisConfig) {
        config = next
        store.save(next)
    }

    val importConfigLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runtimeScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("无法读取文件")
                }
            }
            result.onSuccess { raw ->
                runCatching { parseAiClassroomConfig(raw, config) }
                    .onSuccess { imported ->
                        update(imported)
                        configNotice = "已同步 AI Classroom 配置"
                    }
                    .onFailure { error -> configNotice = "导入失败：${error.message ?: "配置格式无效"}" }
            }.onFailure { error -> configNotice = "读取失败：${error.message ?: "无法访问文件"}" }
        }
    }
    val exportConfigLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                it.write(config.toAiClassroomJson().toString(2))
            } ?: error("无法写入文件")
        }.onSuccess { configNotice = "已导出 AI Classroom 兼容配置" }
            .onFailure { error -> configNotice = "导出失败：${error.message ?: "无法写入文件"}" }
    }

    MaterialTheme(colorScheme = if (config.theme == JarvisTheme.Ink) androidx.compose.material3.darkColorScheme(
        primary = config.theme.primary,
        secondary = Color(0xFFB8D8D4),
        background = config.theme.background,
        surface = config.theme.surface,
        onSurface = Color(0xFFE6F1F0)
    ) else androidx.compose.material3.lightColorScheme(
        primary = config.theme.primary,
        secondary = Color(0xFFB56D3E),
        background = config.theme.background,
        surface = config.theme.surface,
        onSurface = Color(0xFF10212B)
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { showNavigation = !showNavigation }) {
                                Icon(Icons.Default.Menu, contentDescription = "打开导航")
                            }
                        },
                        title = {
                            Column {
                                Text(workspaceName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Jarvis · ${if (config.rootMode == RootMode.Full) "完全模式" else "个人助理"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            if (config.restrictionMode) {
                                Icon(Icons.Default.Shield, contentDescription = "限制模式", tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Default.Warning, contentDescription = "限制模式已关闭", tint = MaterialTheme.colorScheme.error)
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                    )
                },
                bottomBar = {}
            ) { padding ->
                if (isLandscape) {
                    Row(Modifier.fillMaxSize().padding(padding)) {
                        SessionRail(tab, workspaceName, branches, onTab = { tab = it }, onWorkspace = { workspaceName = it }, modifier = Modifier.width(280.dp))
                        VerticalDivider()
                        Box(Modifier.weight(1f)) { MainContent(tab, padding = PaddingValues(0.dp), messages, draft, sending, onDraftChange = { draft = it }, onSend = { clean ->
                            if (clean.isNotEmpty()) { messages = messages + "你：$clean"; draft = ""; sending = true; runtimeScope.launch { messages = messages + "Jarvis：${callJarvisModel(config, clean)}"; sending = false } }
                        }, onBranch = { branches = branches + ConversationBranch("分支 ${branches.size}", messages); tab = JarvisTab.Chat }, config = config, bootstrapReport = bootstrapReport, runtimeMessage = runtimeMessage, onConfig = ::update, onRefreshRuntime = { bootstrapReport = bootstrap.bootstrap() }, onRecovery = { showRecovery = true }, onStartRuntime = { runtimeScope.launch { runtimeMessage = processManager.start().message; bootstrapReport = bootstrap.bootstrap() } }, onStopRuntime = { runtimeScope.launch { processManager.stop(); runtimeMessage = "OpenClaw Gateway 已停止"; bootstrapReport = bootstrap.bootstrap() } }, onRootModeRequest = { pendingRootMode = it; if (it == RootMode.Full) showRootWarning = true else update(config.copy(rootMode = it)) }, configNotice = configNotice, onImportAiClassroom = { importConfigLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, onExportAiClassroom = { exportConfigLauncher.launch("jarvis-ai-classroom-config.json") }) }
                    }
                } else when (tab) {
                    JarvisTab.Chat -> ChatScreen(
                        padding = padding,
                        messages = messages,
                        draft = draft,
                        onDraftChange = { draft = it },
                        sending = sending,
                        onSend = { clean ->
                            if (clean.isNotEmpty()) {
                                messages = messages + "你：$clean"
                                draft = ""
                                sending = true
                                runtimeScope.launch {
                                    val reply = callJarvisModel(config, clean)
                                    messages = messages + "Jarvis：$reply"
                                    sending = false
                                }
                            }
                        }
                    )
                    JarvisTab.Tasks -> TasksScreen(padding)
                    JarvisTab.Workspace -> WorkspaceScreen(padding)
                    JarvisTab.Settings -> SettingsScreen(
                        padding = padding,
                        config = config,
                        bootstrapReport = bootstrapReport,
                        onRefreshRuntime = { bootstrapReport = bootstrap.bootstrap() },
                        runtimeMessage = runtimeMessage,
                        onRecovery = { showRecovery = true },
                        onStartRuntime = {
                            runtimeScope.launch {
                                runtimeMessage = processManager.start().message
                                bootstrapReport = bootstrap.bootstrap()
                            }
                        },
                        onStopRuntime = {
                            runtimeScope.launch {
                                processManager.stop()
                                runtimeMessage = "OpenClaw Gateway 已停止"
                                bootstrapReport = bootstrap.bootstrap()
                            }
                        },
                        onRestrictionChange = { update(config.copy(restrictionMode = it)) },
                        onCommandChange = { update(config.copy(commandChannelEnabled = it)) },
                        onPrefixChange = { update(config.copy(commandPrefix = it)) },
                        onModelBaseUrlChange = { update(config.copy(modelBaseUrl = it)) },
                        onModelNameChange = { update(config.copy(modelName = it)) },
                        onModelApiKeyChange = { update(config.copy(modelApiKey = it)) },
                        onModelChainChange = { update(config.copy(modelChain = it)) },
                        onDeepThinkingChange = { enabled ->
                            val effort = if (enabled && config.reasoningEffort == "off") "medium" else if (!enabled) "off" else config.reasoningEffort
                            update(config.copy(deepThinkingEnabled = enabled, reasoningEffort = effort))
                        },
                        onReasoningEffortChange = { effort ->
                            val normalized = normalizeReasoningEffort(effort)
                            update(config.copy(reasoningEffort = normalized, deepThinkingEnabled = normalized != "off"))
                        },
                        onDeepThinkingModelChange = { update(config.copy(deepThinkingModel = it)) },
                        onVisionBaseUrlChange = { update(config.copy(visionBaseUrl = it)) },
                        onVisionApiKeyChange = { update(config.copy(visionApiKey = it)) },
                        onVisionModelChange = { update(config.copy(visionModel = it)) },
                        onMentorPromptChange = { update(config.copy(mentorPrompt = it)) },
                        onWorldBookChange = { update(config.copy(worldBook = it)) },
                        onThemeChange = { update(config.copy(theme = it)) },
                        configNotice = configNotice,
                        onImportAiClassroom = { importConfigLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        onExportAiClassroom = { exportConfigLauncher.launch("jarvis-ai-classroom-config.json") },
                        onRootModeRequest = {
                            pendingRootMode = it
                            if (it == RootMode.Full) showRootWarning = true else update(config.copy(rootMode = it))
                        }
                    )
                }
            }
        }
    }

    if (isLandscape.not() && showNavigation) {
        NavigationDrawerOverlay(tab, workspaceName, onTab = { tab = it; showNavigation = false }, onWorkspace = { workspaceName = it; showNavigation = false }, onDismiss = { showNavigation = false })
    }

    if (showRootWarning && pendingRootMode == RootMode.Full) {
        RootWarningDialog(
            onCancel = { showRootWarning = false; pendingRootMode = null },
            onConfirm = {
                update(config.copy(rootMode = RootMode.Full))
                showRootWarning = false
                pendingRootMode = null
            }
        )
    }

    if (showRecovery) {
        RuntimeRecoveryDialog(
            onDismiss = { showRecovery = false },
            onReinstall = {
                val result = recoveryManager.reinstallEmbeddedPreservingData()
                runtimeMessage = result.message
                bootstrapReport = bootstrap.bootstrap()
                showRecovery = false
            },
            onTermux = {
                val result = recoveryManager.launchTermuxSetup()
                runtimeMessage = result.message
                showRecovery = false
            }
        )
    }
}

private suspend fun callJarvisModel(config: JarvisConfig, prompt: String): String = withContext(Dispatchers.IO) {
    val baseUrl = config.modelBaseUrl.trim()
    val models = (listOfNotNull(
        config.deepThinkingModel.takeIf { config.deepThinkingEnabled && normalizeReasoningEffort(config.reasoningEffort) != "off" },
        config.modelName
    ) + config.modelChain.split(',', '\n', '，', '；', ';'))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
    if (baseUrl.isBlank() || models.isEmpty()) {
        return@withContext "请先在设置中填写模型 Base URL 和模型名。"
    }
    val system = buildString {
        append(config.mentorPrompt.ifBlank { "你是 Jarvis，一个运行在 Android 手机上的个人助理。请直接、清晰地完成用户请求。" })
        if (config.worldBook.isNotBlank()) {
            append("\n\n世界书：\n")
            append(config.worldBook)
        }
    }
    val effort = normalizeReasoningEffort(config.reasoningEffort)
    var lastError = "未知错误"
    for (model in models) {
        try {
            return@withContext requestChatCompletion(
                baseUrl = baseUrl,
                apiKey = config.modelApiKey,
                model = model,
                messages = JSONArray()
                    .put(JSONObject().put("role", "system").put("content", system))
                    .put(JSONObject().put("role", "user").put("content", prompt)),
                reasoningEffort = if (config.deepThinkingEnabled) effort else "off"
            )
        } catch (error: Exception) {
            lastError = error.message ?: lastError
        }
    }
    "模型请求失败：$lastError"
}

private suspend fun callVisionModel(config: JarvisConfig, prompt: String, dataUrl: String): String = withContext(Dispatchers.IO) {
    val baseUrl = config.visionBaseUrl.ifBlank { config.modelBaseUrl }.trim()
    val model = config.visionModel.ifBlank { config.modelName }.trim()
    if (baseUrl.isBlank() || model.isBlank()) return@withContext "请先在设置中填写识图模型配置。"
    val system = config.mentorPrompt.ifBlank { "你是 Jarvis 的视觉理解模块，请准确描述图片中与用户问题有关的内容。" }
    val content = JSONArray()
        .put(JSONObject().put("type", "text").put("text", prompt))
        .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", dataUrl)))
    requestChatCompletion(
        baseUrl = baseUrl,
        apiKey = config.visionApiKey.ifBlank { config.modelApiKey },
        model = model,
        messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", system))
            .put(JSONObject().put("role", "user").put("content", content)),
        reasoningEffort = "off"
    )
}

@Composable
private fun MainContent(
    tab: JarvisTab,
    padding: PaddingValues,
    messages: List<String>,
    draft: String,
    sending: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onBranch: () -> Unit,
    config: JarvisConfig,
    bootstrapReport: BootstrapReport,
    runtimeMessage: String,
    onConfig: (JarvisConfig) -> Unit,
    onRefreshRuntime: () -> Unit,
    onRecovery: () -> Unit,
    onStartRuntime: () -> Unit,
    onStopRuntime: () -> Unit,
    onRootModeRequest: (RootMode) -> Unit,
    configNotice: String,
    onImportAiClassroom: () -> Unit,
    onExportAiClassroom: () -> Unit
) {
    when (tab) {
        JarvisTab.Chat -> ChatScreen(padding, messages, draft, onDraftChange, sending, onSend, onBranch)
        JarvisTab.Tasks -> TasksScreen(padding)
        JarvisTab.Workspace -> WorkspaceScreen(padding)
        JarvisTab.Settings -> SettingsScreen(
            padding, config, bootstrapReport, onRefreshRuntime, runtimeMessage, onRecovery,
            onStartRuntime, onStopRuntime,
            { onConfig(config.copy(restrictionMode = it)) },
            { onConfig(config.copy(commandChannelEnabled = it)) },
            { onConfig(config.copy(commandPrefix = it)) },
            { onConfig(config.copy(modelBaseUrl = it)) },
            { onConfig(config.copy(modelName = it)) },
            { onConfig(config.copy(modelApiKey = it)) },
            { onConfig(config.copy(modelChain = it)) },
            { enabled -> onConfig(config.copy(deepThinkingEnabled = enabled, reasoningEffort = if (enabled && config.reasoningEffort == "off") "medium" else if (!enabled) "off" else config.reasoningEffort)) },
            { effort -> onConfig(config.copy(reasoningEffort = normalizeReasoningEffort(effort), deepThinkingEnabled = normalizeReasoningEffort(effort) != "off")) },
            { onConfig(config.copy(deepThinkingModel = it)) }, { onConfig(config.copy(visionBaseUrl = it)) },
            { onConfig(config.copy(visionApiKey = it)) }, { onConfig(config.copy(visionModel = it)) },
            { onConfig(config.copy(mentorPrompt = it)) }, { onConfig(config.copy(worldBook = it)) },
            { onConfig(config.copy(theme = it)) },
            configNotice, onImportAiClassroom, onExportAiClassroom, onRootModeRequest
        )
    }
}

@Composable
private fun SessionRail(tab: JarvisTab, workspace: String, branches: List<ConversationBranch>, onTab: (JarvisTab) -> Unit, onWorkspace: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(14.dp)) {
        Text("Jarvis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("工作区", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        TextButton(onClick = { onWorkspace("默认工作区") }, modifier = Modifier.fillMaxWidth()) { Text(workspace, modifier = Modifier.weight(1f)) }
        Spacer(Modifier.height(8.dp))
        Text("会话", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        JarvisTab.entries.forEach { item -> TextButton(onClick = { onTab(item) }, modifier = Modifier.fillMaxWidth()) { Icon(when (item) { JarvisTab.Chat -> Icons.Default.Chat; JarvisTab.Tasks -> Icons.Default.CheckCircle; JarvisTab.Workspace -> Icons.Default.Home; JarvisTab.Settings -> Icons.Default.Settings }, null); Spacer(Modifier.width(8.dp)); Text(item.label) } }
        Spacer(Modifier.height(12.dp))
        Text("分支", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        branches.forEach { Text(it.title, modifier = Modifier.padding(vertical = 6.dp), fontSize = 13.sp) }
    }
}

@Composable
private fun VerticalDivider() { Box(Modifier.width(1.dp).fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant)) }

@Composable
private fun NavigationDrawerOverlay(tab: JarvisTab, workspace: String, onTab: (JarvisTab) -> Unit, onWorkspace: (String) -> Unit, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f))) {
        SessionRail(tab, workspace, listOf(ConversationBranch("主对话", emptyList())), onTab, onWorkspace, Modifier.widthIn(max = 300.dp).fillMaxHeight())
        Spacer(Modifier.weight(1f))
    }
}

private fun requestChatCompletion(
    baseUrl: String,
    apiKey: String,
    model: String,
    messages: JSONArray,
    reasoningEffort: String
): String {
    val endpoint = baseUrl.trimEnd('/').let { if (it.endsWith("/chat/completions")) it else "$it/chat/completions" }
    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doInput = true
        doOutput = true
        connectTimeout = 20_000
        readTimeout = 90_000
        setRequestProperty("Content-Type", "application/json")
        if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
    }
    val request = JSONObject()
        .put("model", model)
        .put("messages", messages)
        .put("temperature", 0.35)
        .put("stream", false)
    if (reasoningEffort in reasoningEfforts && reasoningEffort != "off") {
        request.put("reasoning_effort", reasoningEffort)
    }
    OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(request.toString()) }
    val responseStream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
    val response = BufferedReader(InputStreamReader(responseStream, Charsets.UTF_8)).use { it.readText() }
    if (connection.responseCode !in 200..299) {
        throw IllegalStateException("HTTP ${connection.responseCode}: ${response.take(220)}")
    }
    val content = JSONObject(response).optJSONArray("choices")
        ?.optJSONObject(0)?.optJSONObject("message")?.opt("content")
    return when (content) {
        is String -> content.trim()
        is JSONArray -> buildString {
            for (index in 0 until content.length()) {
                val item = content.optJSONObject(index)
                append(item?.optString("text", "") ?: content.optString(index))
            }
        }.trim()
        else -> "模型返回了空内容。"
    }
}

@Composable
private fun ChatScreen(
    padding: PaddingValues,
    messages: List<String>,
    draft: String,
    onDraftChange: (String) -> Unit,
    sending: Boolean,
    onSend: (String) -> Unit,
    onBranch: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("对话", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("需要规划时，Jarvis 会先判断是否使用工作区。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            IconButton(onClick = onBranch) { Icon(Icons.Default.Add, contentDescription = "创建分支") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { message ->
                val isUser = message.startsWith("你：")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                    Surface(color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp), tonalElevation = if (isUser) 0.dp else 1.dp, modifier = Modifier.widthIn(max = 760.dp)) {
                        Text(message.removePrefix("你：").removePrefix("Jarvis："), modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("给 Jarvis 发消息，或描述一个任务…") },
                maxLines = 4
            )
            IconButton(onClick = { onSend(draft.trim()) }, enabled = !sending, modifier = Modifier.padding(start = 6.dp).size(52.dp)) {
                Icon(Icons.Default.Send, contentDescription = "发送", tint = MaterialTheme.colorScheme.primary)
            }
        }
        }
        if (sending) Text("正在请求模型…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun TasksScreen(padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("任务", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("挂机、自动化和后台任务会在这里显示。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { }) { Icon(Icons.Default.Add, contentDescription = "新建任务") }
        }
        Spacer(Modifier.height(14.dp))
        EmptyState(Icons.Default.CheckCircle, "还没有任务", "先在对话中描述一个任务，Jarvis 会生成可审核的执行计划。")
    }
}

@Composable
private fun WorkspaceScreen(padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("工作区", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Button(onClick = { }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("新建工作区") } }
        Text("按项目组织会话、资料与分支。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        WorkspaceRow("默认工作区", "通用对话与临时任务", "3 个会话", true)
        WorkspaceRow("OpenClaw 集成", "运行时、插件和设备桥接", "5 个会话", false)
        WorkspaceRow("AI 配置", "模型、人格与世界书", "2 个会话", false)
    }
}

@Composable
private fun WorkspaceRow(title: String, description: String, count: String, selected: Boolean) {
    Surface(color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }; Text(count, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
    }
}

@Composable
private fun RuntimeStatusCard(
    report: BootstrapReport,
    onRefresh: () -> Unit,
    runtimeMessage: String,
    onRecovery: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val statusText = when (report.state) {
        RuntimeState.READY -> "就绪"
        RuntimeState.RUNNING -> "运行中"
        RuntimeState.DEGRADED -> "兼容模式"
        RuntimeState.NOT_CONFIGURED -> "未配置"
        RuntimeState.ERROR -> "配置错误"
    }
    val statusColor = when (report.state) {
        RuntimeState.READY, RuntimeState.RUNNING -> Color(0xFF2E7D5B)
        RuntimeState.DEGRADED -> Color(0xFF9A6A18)
        else -> MaterialTheme.colorScheme.error
    }
    SettingsCard(Icons.Default.Code, "OpenClaw 运行时", "单 APK 运行目录和插件目录状态") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(statusText, color = statusColor, fontWeight = FontWeight.SemiBold)
                Text(report.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            TextButton(onClick = onRefresh) { Text("重新检查") }
        }
        Spacer(Modifier.height(8.dp))
        Text("版本：${report.runtimeVersion}", fontSize = 12.sp)
        Text("root：${if (report.rootAvailable) "已检测到" else "未检测到"} · Bridge Token：${if (report.bridgeTokenReady) "已生成" else "缺失"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("插件目录：${report.pluginDirectory}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (runtimeMessage.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(runtimeMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStart) { Text("启动 Gateway") }
            OutlinedButton(onClick = onStop) { Text("停止") }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRecovery, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Build, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("运行时修复 / Termux 备用方案")
        }
    }
}

@Composable
private fun SettingsScreen(
    padding: PaddingValues,
    config: JarvisConfig,
    bootstrapReport: BootstrapReport,
    onRefreshRuntime: () -> Unit,
    runtimeMessage: String,
    onRecovery: () -> Unit,
    onStartRuntime: () -> Unit,
    onStopRuntime: () -> Unit,
    onRestrictionChange: (Boolean) -> Unit,
    onCommandChange: (Boolean) -> Unit,
    onPrefixChange: (String) -> Unit,
    onModelBaseUrlChange: (String) -> Unit,
    onModelNameChange: (String) -> Unit,
    onModelApiKeyChange: (String) -> Unit,
    onModelChainChange: (String) -> Unit,
    onDeepThinkingChange: (Boolean) -> Unit,
    onReasoningEffortChange: (String) -> Unit,
    onDeepThinkingModelChange: (String) -> Unit,
    onVisionBaseUrlChange: (String) -> Unit,
    onVisionApiKeyChange: (String) -> Unit,
    onVisionModelChange: (String) -> Unit,
    onMentorPromptChange: (String) -> Unit,
    onWorldBookChange: (String) -> Unit,
    onThemeChange: (JarvisTheme) -> Unit,
    configNotice: String,
    onImportAiClassroom: () -> Unit,
    onExportAiClassroom: () -> Unit,
    onRootModeRequest: (RootMode) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val toolManager = remember { RuntimeToolManager(context) }
    val toolScope = rememberCoroutineScope()
    var toolStatuses by remember { mutableStateOf<List<RuntimeToolStatus>>(emptyList()) }
    var checkingTools by remember { mutableStateOf(true) }
    var toolNotice by remember { mutableStateOf("") }

    fun refreshTools() {
        checkingTools = true
        toolScope.launch {
            val result = withContext(Dispatchers.IO) { toolManager.inspect() }
            toolStatuses = result
            checkingTools = false
        }
    }

    LaunchedEffect(Unit) { refreshTools() }

    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        Text("设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("先配置基础策略，再逐步接入模型、QQ 和设备工具。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                RuntimeStatusCard(bootstrapReport, onRefreshRuntime, runtimeMessage, onRecovery, onStartRuntime, onStopRuntime)
            }
            item {
                SettingsCard(Icons.Default.Shield, "限制模式", "默认开启，控制高风险内容和动作") {
                    SettingSwitch("限制模式", "关闭后会持续显示警告状态", config.restrictionMode, onRestrictionChange)
                }
            }
            item {
                SettingsCard(Icons.Default.Lock, "root 模式", "完全模式会在启用前显示三秒警告") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RootMode.entries.forEach { mode ->
                            FilterChip(
                                selected = config.rootMode == mode,
                                onClick = { onRootModeRequest(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("当前：${config.rootMode.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                SettingsCard(Icons.Default.Terminal, "QQ 命令通道", "指定会话可发送 /claw 命令，其余消息走普通人格") {
                    SettingSwitch("启用命令通道", "首版默认只做预览，不直接执行", config.commandChannelEnabled, onCommandChange)
                    if (config.commandChannelEnabled) {
                        OutlinedTextField(
                            value = config.commandPrefix,
                            onValueChange = onPrefixChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("命令前缀") }
                        )
                    }
                }
            }
            item {
                SettingsCard(Icons.Default.Build, "模型连接", "兼容 OpenAI Chat Completions 的服务") {
                    OutlinedTextField(
                        value = config.modelBaseUrl,
                        onValueChange = onModelBaseUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.openai.com/v1") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = config.modelName,
                        onValueChange = onModelNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("模型名") },
                        placeholder = { Text("gpt-4o-mini") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = config.modelApiKey,
                        onValueChange = onModelApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("API Key") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = config.modelChain,
                        onValueChange = onModelChainChange,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text("模型回退链（每行一个，可选）") },
                        placeholder = { Text("主模型失败时按顺序尝试") }
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingSwitch(
                        "深度思考",
                        "关闭时不发送 reasoning_effort；开启后由下方档位控制",
                        config.deepThinkingEnabled,
                        onDeepThinkingChange
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("思考深度：${normalizeReasoningEffort(config.reasoningEffort)}", fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        reasoningEfforts.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { effort ->
                                    FilterChip(
                                        selected = normalizeReasoningEffort(config.reasoningEffort) == effort,
                                        onClick = { onReasoningEffortChange(effort) },
                                        label = { Text(effort) }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = config.deepThinkingModel,
                        onValueChange = onDeepThinkingModelChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("深度思考模型（可选）") },
                        placeholder = { Text("留空则使用主模型") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("识图请求会使用单独的多模态配置；未填写时回退到主模型。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            item {
                SettingsCard(Icons.Default.DarkMode, "识图模型", "用于相机取样、截图和多模态任务，兼容 OpenAI image_url 格式") {
                    OutlinedTextField(
                        value = config.visionBaseUrl,
                        onValueChange = onVisionBaseUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("识图 Base URL") },
                        placeholder = { Text("留空继承主模型 Base URL") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = config.visionModel,
                        onValueChange = onVisionModelChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("识图模型名") },
                        placeholder = { Text("gpt-4o-mini") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = config.visionApiKey,
                        onValueChange = onVisionApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("识图 API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = { Text("留空继承主模型 API Key") }
                    )
                }
            }
            item {
                SettingsCard(Icons.Default.Extension, "AI Classroom 配置同步", "导入课堂配置文件，或导出 Jarvis 可再次导入的兼容 JSON") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onImportAiClassroom, modifier = Modifier.weight(1f)) { Text("导入") }
                        OutlinedButton(onClick = onExportAiClassroom, modifier = Modifier.weight(1f)) { Text("导出") }
                    }
                    if (configNotice.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(configNotice, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("支持 AI Classroom 的 classes JSON（会读取第一个课堂的 config），也支持单独的 config JSON。API Key 只在本机配置中保存。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            item {
                SettingsCard(Icons.Default.Chat, "全局人格与世界书", "作为所有工作区的默认提示；工作区覆盖能力会在后续工作区编辑器中接入") {
                    OutlinedTextField(
                        value = config.mentorPrompt,
                        onValueChange = onMentorPromptChange,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("全局提示词") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = config.worldBook,
                        onValueChange = onWorldBookChange,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("全局世界书") }
                    )
                }
            }
            item {
                SettingsCard(Icons.Default.Tune, "风格与外观", "切换界面色调；人格风格仍由提示词和工作区控制") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JarvisTheme.entries.forEach { theme ->
                            FilterChip(selected = config.theme == theme, onClick = { onThemeChange(theme) }, label = { Text(theme.label) })
                        }
                    }
                }
            }
            item {
                SettingsCard(Icons.Default.Extension, "基础插件", "检查内置运行时、系统/root 和 Termux 中的工具") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("工具状态", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { refreshTools(); toolNotice = "正在检查工具路径" }, enabled = !checkingTools) { Text("刷新") }
                    }
                    if (checkingTools && toolStatuses.isEmpty()) {
                        Text("正在检查内置运行时、系统/root 和 Termux…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    } else {
                        toolStatuses.forEach { status ->
                            PluginToolRow(status) {
                                val result = toolManager.install(status)
                                toolNotice = result.message
                                if (result.success) refreshTools()
                            }
                        }
                    }
                    if (toolNotice.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(toolNotice, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Rust、Python 和 Git 体积较大，缺少时通过 Termux 按需安装；Jarvis 不会把未经校验的二进制直接写入系统目录。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PluginToolRow(status: RuntimeToolStatus, onInstall: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Icon(Icons.Default.Extension, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(status.label, fontWeight = FontWeight.SemiBold)
            Text(status.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (status.source == ToolSource.MISSING) {
            TextButton(onClick = onInstall) { Text("安装") }
        } else {
            Text(status.source.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RuntimeRecoveryDialog(
    onDismiss: () -> Unit,
    onReinstall: () -> Unit,
    onTermux: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("选择 OpenClaw 恢复方式") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Jarvis 会保留配置、插件、工作区和记忆数据。APK 已携带 Android arm64 OpenClaw 候选运行时，可先重建它并重新启动。")
                Text("修复内置运行时：清理并重新解压内置 Node/OpenClaw，保留数据。若设备原生依赖不兼容，再切换 Termux。", fontSize = 13.sp)
                Text("Termux 备用方案：打开已安装的 Termux，自动生成固定版本的安装命令，下载 Node.js 和 OpenClaw 2026.9.7。", fontSize = 13.sp)
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = onTermux) { Text("使用 Termux 备用方案") }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onReinstall) { Text("修复内置运行时并保留数据") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun SettingsCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(28.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RootWarningDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    var seconds by remember { mutableStateOf(3) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (seconds > 0) {
            kotlinx.coroutines.delay(1000)
            seconds -= 1
        }
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("启用 root 完全模式？") },
        text = {
            Column {
                Text("这会开放 Jarvis 已实现的系统查看、修改、进程和输入工具。所有动作仍会经过结构化工具协议并写入审计日志。")
                Spacer(Modifier.height(10.dp))
                Text(if (seconds > 0) "请阅读风险说明，${seconds} 秒后可以确认。" else "确认后完全模式立即生效。", fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = seconds == 0) { Text("确认启用") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("取消") } }
    )
}
