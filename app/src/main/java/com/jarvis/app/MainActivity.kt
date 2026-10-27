package com.jarvis.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.jarvis.app.runtime.BootstrapController
import com.jarvis.app.runtime.BootstrapReport
import com.jarvis.app.runtime.OpenClawProcessManager
import com.jarvis.app.runtime.RuntimeState
import com.jarvis.app.runtime.RuntimeRecoveryManager
import com.jarvis.app.runtime.JarvisBridgeServer
import com.jarvis.app.runtime.JarvisBridgeService
import com.jarvis.app.runtime.DeviceTaskCoordinator
import com.jarvis.app.runtime.ConversationStore
import com.jarvis.app.runtime.ChatMessage
import com.jarvis.app.runtime.ChatMessageRole
import com.jarvis.app.runtime.ChatSession
import com.jarvis.app.runtime.CommunityPluginManager
import com.jarvis.app.runtime.GeneratedOpenClawPlugin
import com.jarvis.app.runtime.ManagedOpenClawPlugin
import com.jarvis.app.runtime.RuntimeToolManager
import com.jarvis.app.runtime.RuntimeToolStatus
import com.jarvis.app.runtime.ToolSource
import com.jarvis.app.runtime.ModelConnectionChecker
import com.jarvis.app.runtime.AccessibilityStartupManager
import com.jarvis.app.runtime.AccessibilityController
import com.jarvis.app.runtime.AutomationManager
import com.jarvis.app.runtime.AutomationKind
import com.jarvis.app.runtime.AutomationRule
import com.jarvis.app.runtime.AutomationEvent
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import android.content.res.Configuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Gives every interactive control a short, consistent press response. */
private fun Modifier.jarvisPressScale(): Modifier = composed {
    val pressed = remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed.value) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press-scale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            pressed.value = true
            try {
                waitForUpOrCancellation()
            } finally {
                pressed.value = false
            }
        }
    }
}

private enum class JarvisTab(val label: String) { Chat("对话"), Tasks("任务"), Workspace("工作区"), Settings("设置") }

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

    fun save(config: JarvisConfig, durable: Boolean = false): Boolean {
        val effort = normalizeReasoningEffort(config.reasoningEffort)
        val editor = prefs.edit()
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
        return if (durable) editor.commit() else { editor.apply(); true }
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
        BootstrapController(this).prepareTokens()
        // Render the shell before any runtime probing so a slow ROM, root check, or
        // missing embedded binary can never leave the activity blank.
        setContent { JarvisApp() }

        // The bridge is optional during first render. Starting it is best effort and must
        // not block or crash the visible shell on devices that restrict foreground services.
        runCatching {
            val bridgeIntent = Intent(this, JarvisBridgeService::class.java)
            ContextCompat.startForegroundService(this, bridgeIntent)
        }
    }

    override fun onStart() {
        super.onStart()
        val manager = AccessibilityStartupManager(this)
        if (manager.autoConnect && !manager.isEnabled()) {
            Thread { manager.connectWithRoot() }.start()
        }
    }
}

private fun initialBootstrapReport(context: Context): BootstrapReport = BootstrapReport(
    state = RuntimeState.DEGRADED,
    runtimeMode = com.jarvis.app.runtime.RuntimeMode.REMOTE,
    runtimeVersion = "未检测到兼容运行时",
    filesReady = false,
    rootAvailable = false,
    bridgeTokenReady = false,
    pluginDirectory = java.io.File(context.filesDir, "jarvis/plugins").absolutePath,
    message = "正在检查 OpenClaw 运行时…"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JarvisApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { ConfigStore(context) }
    val bootstrap = remember { BootstrapController(context) }
    val processManager = remember { OpenClawProcessManager.get(context) }
    val recoveryManager = remember { RuntimeRecoveryManager(context) }
    val runtimeScope = rememberCoroutineScope()
    var bootstrapReport by remember { mutableStateOf(initialBootstrapReport(context)) }
    var runtimeMessage by remember { mutableStateOf("") }

    // Probe OpenClaw after the first Compose frame. Runtime extraction, root checks, and
    // ROM service probes are allowed to fail without preventing the chat shell from opening.
    LaunchedEffect(Unit) {
        val report = withContext(Dispatchers.IO) {
            runCatching { bootstrap.bootstrap() }.getOrElse { error ->
                initialBootstrapReport(context).copy(
                    state = RuntimeState.ERROR,
                    message = "运行时检查失败：${error.message ?: "未知错误"}"
                )
            }
        }
        bootstrapReport = report
    }
    var config by remember { mutableStateOf(store.load()) }
    val conversationStore = remember { ConversationStore(context) }
    var conversations by remember { mutableStateOf(conversationStore.loadOrCreate()) }
    var activeConversationId by rememberSaveable { mutableStateOf(conversationStore.activeSessionId(conversations)) }
    val view = LocalView.current
    SideEffect {
        (context as? ComponentActivity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !config.theme.dark
                isAppearanceLightNavigationBars = !config.theme.dark
            }
        }
    }
    var tab by rememberSaveable { mutableStateOf(JarvisTab.Chat) }
    var draft by rememberSaveable { mutableStateOf("") }
    var editingMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    var showRootWarning by remember { mutableStateOf(false) }
    var pendingRootMode by remember { mutableStateOf<RootMode?>(null) }
    var showRecovery by remember { mutableStateOf(false) }
    var configNotice by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var showNavigation by rememberSaveable { mutableStateOf(false) }
    var workspaceName by rememberSaveable {
        mutableStateOf(conversations.firstOrNull { it.id == activeConversationId }?.workspace ?: "默认工作区")
    }
    val activeConversation = conversations.firstOrNull { it.id == activeConversationId } ?: conversations.first()
    val messages = activeConversation.messages
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) {
        config = store.load()
    }

    fun update(next: JarvisConfig) {
        config = next
        store.save(next)
    }

    fun saveConversation(updated: ChatSession) {
        val next = conversations.map { if (it.id == updated.id) updated else it }.sortedByDescending { it.updatedAt }
        conversations = next
        conversationStore.save(next, activeConversationId)
    }

    fun createConversation() {
        val created = ChatSession(workspace = workspaceName)
        conversations = (listOf(created) + conversations).take(100)
        activeConversationId = created.id
        conversationStore.save(conversations, created.id)
        draft = ""
        editingMessageId = null
        tab = JarvisTab.Chat
        showNavigation = false
    }

    fun createBranch() {
        val source = activeConversation
        val created = ChatSession(
            title = "分支 · ${source.title}".take(32),
            workspace = source.workspace,
            messages = source.messages
        )
        conversations = (listOf(created) + conversations).take(100)
        activeConversationId = created.id
        conversationStore.save(conversations, created.id)
        draft = ""
        editingMessageId = null
        tab = JarvisTab.Chat
        showNavigation = false
    }

    fun selectConversation(id: String) {
        if (conversations.none { it.id == id }) return
        activeConversationId = id
        conversationStore.save(conversations, id)
        draft = ""
        editingMessageId = null
        workspaceName = conversations.first { it.id == id }.workspace
        tab = JarvisTab.Chat
        showNavigation = false
    }

    fun undoLatestTurn() {
        if (sending) return
        editingMessageId = null
        draft = ""
        val editable = activeConversation.messages.toMutableList()
        val lastAssistant = editable.indexOfLast { it.role == ChatMessageRole.ASSISTANT }
        if (lastAssistant >= 0) {
            editable.removeAt(lastAssistant)
            val precedingUser = editable.indexOfLast { it.role == ChatMessageRole.USER }
            if (precedingUser >= 0) editable.removeAt(precedingUser)
        } else {
            val pendingUser = editable.indexOfLast { it.role == ChatMessageRole.USER }
            if (pendingUser >= 0) editable.removeAt(pendingUser)
        }
        saveConversation(activeConversation.copy(
            title = if (editable.none { it.role == ChatMessageRole.USER }) "新对话" else activeConversation.title,
            messages = editable,
            updatedAt = System.currentTimeMillis()
        ))
    }

    fun editLatestUserMessage() {
        if (sending) return
        val latestUser = activeConversation.messages.lastOrNull { it.role == ChatMessageRole.USER }
        val userIndex = activeConversation.messages.indexOfLast { it.role == ChatMessageRole.USER }
        if (userIndex < 0) return
        editingMessageId = latestUser?.id
        draft = latestUser?.content.orEmpty()
    }

    fun cancelEdit() {
        editingMessageId = null
        draft = ""
    }

    fun sendPrompt(clean: String) {
        if (clean.isBlank() || sending) return
        val sessionId = activeConversation.id
        val editIndex = editingMessageId?.let { id -> activeConversation.messages.indexOfFirst { it.id == id } } ?: -1
        val previousHistory = if (editIndex >= 0) activeConversation.messages.take(editIndex) else activeConversation.messages
        val currentMessages = (previousHistory + ChatMessage(role = ChatMessageRole.USER, content = clean)).takeLast(300)
        val title = if (editIndex == 0 || activeConversation.messages.none { it.role == ChatMessageRole.USER })
            clean.lineSequence().first().take(32) else activeConversation.title
        saveConversation(activeConversation.copy(title = title, messages = currentMessages, updatedAt = System.currentTimeMillis()))
        draft = ""
        editingMessageId = null
        val ruleDraft = AutomationManager.parse(clean)
        if (ruleDraft != null) {
            val rule = AutomationManager(context).create(ruleDraft)
            val extra = if (rule.kind == AutomationKind.PIXIV_NEW_WORK && rule.artist.isBlank())
                "请到“任务”里补上画师名称或 ID。" else "请到“任务”里检查规则并手动启用。"
            val latest = conversations.firstOrNull { it.id == sessionId } ?: return
            saveConversation(latest.copy(messages = (latest.messages + ChatMessage(
                role = ChatMessageRole.ASSISTANT,
                content = "已创建自动化草案「${rule.title}」，目前暂停。$extra 触发时会先检查通知内容，再按全局风格执行一次操作。"
            )).takeLast(300), updatedAt = System.currentTimeMillis()))
            return
        }
        sending = true
        runtimeScope.launch {
            val reply = callJarvisModel(context, config, sessionId, currentMessages)
            val latest = conversations.firstOrNull { it.id == sessionId }
            if (latest != null) saveConversation(latest.copy(
                messages = (latest.messages + ChatMessage(role = ChatMessageRole.ASSISTANT, content = reply)).takeLast(300),
                updatedAt = System.currentTimeMillis()
            ))
            sending = false
        }
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

    Crossfade(targetState = config.theme, animationSpec = tween(300), label = "theme-transition") { activeTheme ->
    CompositionLocalProvider(LocalJarvisTheme provides activeTheme) {
    MaterialTheme(colorScheme = activeTheme.colorScheme(), shapes = androidx.compose.material3.Shapes(
        small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                        navigationIcon = {
                            IconButton(onClick = { showNavigation = !showNavigation }, modifier = Modifier.jarvisPressScale()) {
                                Icon(Icons.Default.Menu, contentDescription = "打开导航")
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                            JarvisBrandMark(size = 34)
                            Spacer(Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(activeConversation.title.ifBlank { workspaceName }, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Jarvis · ${if (config.rootMode == RootMode.Full) "完全模式" else "个人助理"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                        SessionRail(tab, workspaceName, conversations, activeConversationId, onTab = { tab = it }, onWorkspace = { workspaceName = it }, onSelectConversation = ::selectConversation, onNewConversation = ::createConversation, modifier = Modifier.width(280.dp))
                        VerticalDivider()
                        Box(Modifier.weight(1f)) { Crossfade(targetState = tab, animationSpec = tween(220), label = "landscape-content") { selectedTab -> MainContent(selectedTab, padding = PaddingValues(0.dp), activeConversation.id, messages, draft, sending, onDraftChange = { draft = it }, onSend = ::sendPrompt, onNewConversation = ::createConversation, onBranch = ::createBranch, onUndoLatest = ::undoLatestTurn, onEditLatest = ::editLatestUserMessage, onCancelEdit = ::cancelEdit, editingLatest = editingMessageId != null, canUndoLatest = messages.isNotEmpty() && !sending, canEditLatest = messages.any { it.role == ChatMessageRole.USER } && !sending, config = config, bootstrapReport = bootstrapReport, runtimeMessage = runtimeMessage, onConfig = ::update, onRefreshRuntime = { bootstrapReport = bootstrap.bootstrap() }, onRecovery = { showRecovery = true }, onStartRuntime = { runtimeScope.launch { runtimeMessage = processManager.start().message; bootstrapReport = bootstrap.bootstrap() } }, onStopRuntime = { runtimeScope.launch { processManager.stop(); runtimeMessage = "OpenClaw Gateway 已停止"; bootstrapReport = bootstrap.bootstrap() } }, onRootModeRequest = { pendingRootMode = it; if (it == RootMode.Full) showRootWarning = true else update(config.copy(rootMode = it)) }, configNotice = configNotice, onImportAiClassroom = { importConfigLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, onExportAiClassroom = { exportConfigLauncher.launch("jarvis-ai-classroom-config.json") }) } }
                    }
                } else {
                    Crossfade(targetState = tab, animationSpec = tween(220), label = "portrait-content") { selectedTab -> when (selectedTab) {
                    JarvisTab.Chat -> ChatScreen(
                        padding = padding,
                        conversationId = activeConversation.id,
                        messages = messages,
                        draft = draft,
                        onDraftChange = { draft = it },
                        sending = sending,
                        onSend = ::sendPrompt,
                        onNewConversation = ::createConversation,
                        onBranch = ::createBranch,
                        onUndoLatest = ::undoLatestTurn,
                        onEditLatest = ::editLatestUserMessage,
                        onCancelEdit = ::cancelEdit,
                        editingLatest = editingMessageId != null,
                        canUndoLatest = messages.isNotEmpty() && !sending,
                        canEditLatest = messages.any { it.role == ChatMessageRole.USER } && !sending
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
        }
    AnimatedVisibility(
        visible = isLandscape.not() && showNavigation,
        enter = fadeIn(tween(180)) + slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(260)),
        exit = fadeOut(tween(150)) + slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(220))
    ) {
        NavigationDrawerOverlay(tab, workspaceName, conversations, activeConversationId, onTab = { tab = it; showNavigation = false }, onWorkspace = { workspaceName = it; showNavigation = false }, onSelectConversation = ::selectConversation, onNewConversation = ::createConversation, onDismiss = { showNavigation = false })
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
                showRecovery = false
                runtimeMessage = "正在修复内置运行时并保留数据…"
                runtimeScope.launch {
                    val result = withContext(Dispatchers.IO) { recoveryManager.reinstallEmbeddedPreservingData() }
                    runtimeMessage = result.message
                    bootstrapReport = withContext(Dispatchers.IO) { bootstrap.bootstrap() }
                }
            },
            onTermux = {
                val result = recoveryManager.launchTermuxSetup()
                runtimeMessage = result.message
                showRecovery = false
            }
        )
    }
    }
    }
    }
}

private suspend fun callJarvisModel(
    context: Context,
    config: JarvisConfig,
    conversationId: String,
    history: List<ChatMessage>
): String = withContext(Dispatchers.IO) {
    DeviceTaskCoordinator.run("本机对话") {
    val prompt = history.lastOrNull { it.role == ChatMessageRole.USER }?.content.orEmpty()
    val operationMemory = com.jarvis.app.runtime.OperationMemory.at(context.filesDir)
    val memoryStatus = runCatching { operationMemory.begin(prompt); operationMemory.summary() }
        .getOrElse { "操作记忆暂不可用，请通过普通工具完成任务并如实报告记忆错误。" }
    val gateway = OpenClawProcessManager.get(context)
    val started = gateway.start()
    if (!started.started) "OpenClaw 未就绪：${started.message}" else runCatching {
        val system = buildString {
            append(config.mentorPrompt.ifBlank { "你是 Jarvis，运行在 Android 手机上的个人助理。需要访问设备时调用 Jarvis 设备工具，并如实报告结果。" })
            if (config.worldBook.isNotBlank()) append("\n\n世界书：\n${config.worldBook}")
            append("\n\n" + com.jarvis.app.runtime.OperationMemory.instructions)
            append("\n" + memoryStatus)
        }
        val conversationMessages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", system))
        history.takeLast(40).forEach { item ->
            conversationMessages.put(JSONObject()
                .put("role", if (item.role == ChatMessageRole.USER) "user" else "assistant")
                .put("content", item.content))
        }
        val request = JSONObject()
            .put("model", "openclaw")
            .put("user", "jarvis-local-$conversationId-${history.lastOrNull()?.id.orEmpty()}")
            .put("stream", false)
            .put("messages", conversationMessages)
        val connection = (URL("http://127.0.0.1:18789/v1/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 5_000
            // Device tasks span multiple model/tool round trips while other apps are open.
            readTimeout = 15 * 60_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${gateway.token()}")
        }
        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(request.toString()) }
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (status !in 200..299) error("Gateway HTTP $status: ${response.take(240)}")
            JSONObject(response).optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.optString("content")?.takeIf { it.isNotBlank() }
                ?: error("Gateway 没有返回可显示的内容")
        } finally {
            connection.disconnect()
        }
    }.getOrElse { "OpenClaw 请求失败：${it.message ?: it.javaClass.simpleName}" }
    }
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
    conversationId: String,
    messages: List<ChatMessage>,
    draft: String,
    sending: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onNewConversation: () -> Unit,
    onBranch: () -> Unit,
    onUndoLatest: () -> Unit,
    onEditLatest: () -> Unit,
    onCancelEdit: () -> Unit,
    editingLatest: Boolean,
    canUndoLatest: Boolean,
    canEditLatest: Boolean,
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
        JarvisTab.Chat -> ChatScreen(
            padding = padding,
            conversationId = conversationId,
            messages = messages,
            draft = draft,
            onDraftChange = onDraftChange,
            sending = sending,
            onSend = onSend,
            onNewConversation = onNewConversation,
            onBranch = onBranch,
            onUndoLatest = onUndoLatest,
            onEditLatest = onEditLatest,
            onCancelEdit = onCancelEdit,
            editingLatest = editingLatest,
            canUndoLatest = canUndoLatest,
            canEditLatest = canEditLatest
        )
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
private fun SessionRail(
    tab: JarvisTab,
    workspace: String,
    conversations: List<ChatSession>,
    activeConversationId: String,
    onTab: (JarvisTab) -> Unit,
    onWorkspace: (String) -> Unit,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compact = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Column(modifier.background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(if (compact) 12.dp else 18.dp), horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = if (compact) 2.dp else 12.dp)) {
            JarvisBrandMark(size = if (compact) 30 else 42)
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text("Jarvis", style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (!compact) Text(LocalJarvisTheme.current.story, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(if (compact) 4.dp else 20.dp))
        if (!compact) Text("工作区", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        TextButton(onClick = { onWorkspace("默认工作区") }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = if (compact) 0.dp else 12.dp), modifier = Modifier.fillMaxWidth().jarvisPressScale()) {
            Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(workspace, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(if (compact) 2.dp else 16.dp))
        if (!compact) Text("导航", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.height(if (compact) 2.dp else 8.dp))
        JarvisTab.entries.forEach { item ->
            Surface(color = if (tab == item) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = if (compact) 2.dp else 3.dp)
                    .jarvisPressScale().clickable(role = androidx.compose.ui.semantics.Role.Tab, onClick = { onTab(item) })) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 13.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start) {
                    Icon(when (item) { JarvisTab.Chat -> Icons.Default.Chat; JarvisTab.Tasks -> Icons.Default.CheckCircle; JarvisTab.Workspace -> Icons.Default.Home; JarvisTab.Settings -> Icons.Default.Settings }, null,
                        modifier = Modifier.size(20.dp), tint = if (tab == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(item.label, modifier = Modifier.weight(1f), fontWeight = if (tab == item) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("對話", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onNewConversation, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), modifier = Modifier.jarvisPressScale()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("新建", fontSize = 12.sp)
            }
        }
        conversations.take(30).forEach { conversation ->
            val selected = conversation.id == activeConversationId && tab == JarvisTab.Chat
            Surface(
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    .clickable { onSelectConversation(conversation.id) }
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Text(conversation.title, maxLines = 1, fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    val preview = conversation.messages.lastOrNull()?.content.orEmpty()
                    if (preview.isNotBlank()) Text(preview, maxLines = 1, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun VerticalDivider() { Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant)) }

@Composable
private fun NavigationDrawerOverlay(
    tab: JarvisTab,
    workspace: String,
    conversations: List<ChatSession>,
    activeConversationId: String,
    onTab: (JarvisTab) -> Unit,
    onWorkspace: (String) -> Unit,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f))) {
        SessionRail(tab, workspace, conversations, activeConversationId, onTab, onWorkspace,
            onSelectConversation, onNewConversation, Modifier.widthIn(max = 300.dp).fillMaxHeight())
        Spacer(Modifier.weight(1f).fillMaxHeight().clickable(onClick = onDismiss))
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
    conversationId: String,
    messages: List<ChatMessage>,
    draft: String,
    onDraftChange: (String) -> Unit,
    sending: Boolean,
    onSend: (String) -> Unit,
    onNewConversation: () -> Unit = {},
    onBranch: () -> Unit = {},
    onUndoLatest: () -> Unit = {},
    onEditLatest: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    editingLatest: Boolean = false,
    canUndoLatest: Boolean = false,
    canEditLatest: Boolean = false
) {
    val listState = rememberLazyListState()
    var showConversationMenu by remember { mutableStateOf(false) }
    val welcome = messages.isEmpty()
    val compact = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    LaunchedEffect(conversationId, messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("对话", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("撤销移除上一轮聊天记录；已经执行的设备操作不会回滚。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            TextButton(onClick = if (editingLatest) onCancelEdit else onEditLatest,
                enabled = !sending && (editingLatest || canEditLatest),
                contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.jarvisPressScale()) {
                Icon(if (editingLatest) Icons.Default.Close else Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(if (editingLatest) "取消" else "编辑", fontSize = 12.sp)
            }
            TextButton(onClick = onUndoLatest, enabled = canUndoLatest, contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.jarvisPressScale()) {
                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("撤销", fontSize = 12.sp)
            }
            Box {
                IconButton(onClick = { showConversationMenu = true }, modifier = Modifier.jarvisPressScale()) { Icon(Icons.Default.Add, contentDescription = "新建对话或分支") }
                DropdownMenu(expanded = showConversationMenu, onDismissRequest = { showConversationMenu = false }) {
                    DropdownMenuItem(text = { Text("新建对话") }, onClick = { showConversationMenu = false; onNewConversation() })
                    DropdownMenuItem(text = { Text("从当前对话创建分支") }, onClick = { showConversationMenu = false; onBranch() })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
            if (welcome) {
                item {
                    ThemeIllustration("让想法，\n开始行动。", "交给 Jarvis，逐步完成。", compact = compact)
                    Spacer(Modifier.height(18.dp))
                    Text("从一个小任务开始", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple(Icons.Default.Tune, "手机状态", "查询本机手机型号、系统版本和当前设备状态。"),
                            Triple(Icons.Default.Home, "打开应用", "帮我打开一个应用，请先问我要打开哪个。"),
                            Triple(Icons.Default.Build, "操作记忆", "查看已保存的操作路径，告诉我有哪些可以复用。")
                        ).forEach { (icon, title, prompt) ->
                            OutlinedButton(onClick = { onDraftChange(prompt) }, modifier = Modifier.jarvisPressScale()) {
                                Icon(icon, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text(title)
                            }
                        }
                    }
                }
            }
            items(if (welcome) emptyList() else messages, key = { it.id }) { message ->
                AnimatedVisibility(visible = true, enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220))) {
                    val isUser = message.role == ChatMessageRole.USER
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                        Text(if (isUser) "你" else "Jarvis", modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Surface(color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.widthIn(max = 760.dp)) {
                            Text(message.content, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
        if (editingLatest) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("正在修改上一条；发送后才会替换原对话轮次。", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (editingLatest) "修改上一条消息后重新发送…" else "给 Jarvis 发消息，或描述一个任务…") },
                maxLines = 4
            )
            IconButton(onClick = { onSend(draft.trim()) }, enabled = !sending && draft.isNotBlank(), modifier = Modifier.padding(start = 6.dp).size(52.dp).jarvisPressScale()) {
                Icon(Icons.Default.Send, contentDescription = "发送", tint = MaterialTheme.colorScheme.primary)
            }
        }
        }
        if (sending) Text("正在请求模型…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun TasksScreen(padding: PaddingValues) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember(context) { com.jarvis.app.runtime.AutomationManager(context) }
    var rules by remember { mutableStateOf(manager.list()) }
    var events by remember { mutableStateOf(manager.recentEvents()) }
    var notice by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<AutomationRule?>(null) }
    var artistDraft by remember { mutableStateOf("") }
    var replyDraft by remember { mutableStateOf("") }
    var notificationAccess by remember { mutableStateOf(manager.hasNotificationAccess()) }
    var accessibilityReady by remember { mutableStateOf(AccessibilityController.service != null) }

    fun refresh() {
        rules = manager.list()
        events = manager.recentEvents()
        notificationAccess = manager.hasNotificationAccess()
        accessibilityReady = AccessibilityController.service != null
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            kotlinx.coroutines.delay(1200)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("自动化", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("通过对话创建，收到通知后按规则执行一次。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { refresh() }, modifier = Modifier.jarvisPressScale()) { Icon(Icons.Default.Tune, contentDescription = "刷新自动化") }
        }
        Spacer(Modifier.height(10.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("运行前检查", fontWeight = FontWeight.SemiBold)
                Text("通知访问：${if (notificationAccess) "已授权" else "未授权"} · 无障碍：${if (accessibilityReady) "已连接" else "未连接"}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("规则默认暂停；启用后只匹配目标应用通知，并在完成后核实结果。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    if (!notificationAccess) OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, modifier = Modifier.jarvisPressScale()) { Text("授权通知") }
                    if (!accessibilityReady) OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.jarvisPressScale()) { Text("开启无障碍") }
                }
            }
        }
        if (notice.isNotBlank()) {
            Text(notice, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
        Spacer(Modifier.height(6.dp))
        if (rules.isEmpty()) {
            EmptyState(Icons.Default.CheckCircle, "还没有自动化", "在对话中输入“当哔哩哔哩上有人回复我就立刻回复”或“当 Pixiv 画师发布新作品就立刻下载”。")
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rules.forEach { rule ->
                    AutomationRuleCard(rule, onEdit = {
                        editing = rule
                        artistDraft = rule.artist
                        replyDraft = rule.replyInstruction
                    }, readiness = if (rule.enabled) null else manager.readiness(rule), onToggle = { enabled ->
                        runCatching { manager.setEnabled(rule.id, enabled) }
                            .onSuccess { notice = if (enabled) "已启用“${rule.title}”" else "已暂停“${rule.title}”"; refresh() }
                            .onFailure { notice = "无法启用：${it.message ?: "请先完成权限和配置"}" }
                    }, onDelete = {
                        manager.delete(rule.id)
                        notice = "已删除自动化规则"
                        refresh()
                    })
                }
                if (events.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("最近执行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    events.take(8).forEach { event ->
                        val ok = event.optBoolean("ok")
                        Text("${if (ok) "✓" else "!"} ${event.optString("title")} · ${event.optString("message")}", fontSize = 12.sp, color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    editing?.let { rule ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("配置自动化") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(rule.title, fontWeight = FontWeight.SemiBold)
                    Text("原始要求：${rule.sourcePrompt}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (rule.kind == AutomationKind.PIXIV_NEW_WORK) {
                        OutlinedTextField(value = artistDraft, onValueChange = { artistDraft = it }, singleLine = true, label = { Text("Pixiv 画师名称或用户 ID") })
                    }
                    if (rule.kind == AutomationKind.BILIBILI_REPLY) {
                        OutlinedTextField(value = replyDraft, onValueChange = { replyDraft = it }, minLines = 2, label = { Text("回复风格要求") })
                    }
                    Text("启用前还需要目标应用通知访问和 Jarvis 无障碍服务。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    runCatching { manager.update(rule.copy(artist = artistDraft.trim(), replyInstruction = replyDraft.trim())) }
                        .onSuccess { notice = "配置已保存；规则仍保持暂停，请检查后启用"; editing = null; refresh() }
                        .onFailure { notice = "保存失败：${it.message}" }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun AutomationRuleCard(
    rule: AutomationRule,
    onEdit: () -> Unit,
    readiness: String?,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(rule.title, fontWeight = FontWeight.SemiBold)
                    Text(rule.kind.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = rule.enabled, onCheckedChange = onToggle, modifier = Modifier.jarvisPressScale())
            }
            Text(if (rule.enabled) "已启用 · 等待目标应用通知" else "已暂停 · 不会自动操作", fontSize = 13.sp, color = if (rule.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            if (rule.kind == AutomationKind.PIXIV_NEW_WORK) Text("画师：${rule.artist.ifBlank { "未配置" }}", fontSize = 12.sp)
            if (!rule.enabled && readiness != null) Text("启用前：$readiness", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            if (rule.lastResult.isNotBlank()) Text("最近结果：${rule.lastResult}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 6.dp)) {
                TextButton(onClick = onEdit, modifier = Modifier.jarvisPressScale()) { Text("配置") }
                TextButton(onClick = onDelete, modifier = Modifier.jarvisPressScale()) { Text("删除") }
            }
        }
    }
}

@Composable
private fun WorkspaceScreen(padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("工作区", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Button(onClick = { }, modifier = Modifier.jarvisPressScale()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("新建工作区") } }
        Text("按项目组织会话、资料与分支。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        WorkspaceRow("默认工作区", "通用对话与临时任务", "3 个会话", true)
        WorkspaceRow("OpenClaw 集成", "运行时、插件和设备桥接", "5 个会话", false)
        WorkspaceRow("AI 配置", "模型、人格与世界书", "2 个会话", false)
    }
}

@Composable
private fun WorkspaceRow(title: String, description: String, count: String, selected: Boolean) {
    Surface(color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).animateContentSize()) {
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
        RuntimeState.READY, RuntimeState.RUNNING -> MaterialTheme.colorScheme.primary
        RuntimeState.DEGRADED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.error
    }
    SettingsCard(Icons.Default.Code, "OpenClaw 运行时", "单 APK 运行目录和插件目录状态") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(statusText, color = statusColor, fontWeight = FontWeight.SemiBold)
                Text(report.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            TextButton(onClick = onRefresh, modifier = Modifier.jarvisPressScale()) { Text("重新检查") }
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
            Button(onClick = onStart, modifier = Modifier.jarvisPressScale()) { Text("启动 Gateway") }
            OutlinedButton(onClick = onStop, modifier = Modifier.jarvisPressScale()) { Text("停止") }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRecovery, modifier = Modifier.fillMaxWidth().jarvisPressScale()) {
            Icon(Icons.Default.Build, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("运行时修复 / Termux 备用方案")
        }
    }
}

@Composable
private fun ConfigurationActions(config: JarvisConfig) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val latestConfig by rememberUpdatedState(config)
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }

    fun applyConfiguration(verify: Boolean) {
        val snapshot = config
        val saved = runCatching { ConfigStore(context).save(snapshot, durable = true) }.getOrDefault(false)
        if (!saved) {
            failed = true
            notice = "保存失败，请检查手机存储空间后重试。"
            return
        }
        busy = true
        failed = false
        notice = "配置已保存，正在应用到 OpenClaw…"
        scope.launch {
            try {
                val started = OpenClawProcessManager.get(context).start()
                if (!started.started) {
                    failed = true
                    notice = "配置已保存；OpenClaw 尚未就绪：${started.message}"
                } else if (verify) {
                    notice = "配置已应用，正在验证主模型连接…"
                    val result = withContext(Dispatchers.IO) {
                        val selectedModel = if (snapshot.deepThinkingEnabled) snapshot.deepThinkingModel.ifBlank { snapshot.modelName } else snapshot.modelName
                        ModelConnectionChecker.test(snapshot.modelBaseUrl, snapshot.modelApiKey, selectedModel)
                    }
                    failed = !result.ok
                    notice = "配置已保存并应用。${result.message}"
                } else {
                    notice = "配置已保存并应用，OpenClaw 已就绪。"
                }
                if (latestConfig != snapshot) notice += " 检测到新的修改，请再次保存并应用。"
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                failed = true
                notice = "配置已保存；应用配置失败，请重新启动 Gateway 后重试。"
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { applyConfiguration(false) }, enabled = !busy, modifier = Modifier.weight(1f).jarvisPressScale()) { Text("保存并应用") }
            OutlinedButton(onClick = { applyConfiguration(true) }, enabled = !busy, modifier = Modifier.weight(1f).jarvisPressScale()) { Text(if (busy) "处理中…" else "保存并验证") }
        }
        Text(
            notice.ifBlank { "编辑内容自动保存在本机；保存并应用可更新智能体。验证会发起一次简短的主模型请求。" },
            color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun OperationMemoryCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("正在读取操作记忆…") }
    var routes by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    fun refresh(action: String = "list", id: String = "") {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bridge = com.jarvis.app.runtime.DeviceCapabilityBridge(context)
                    fun call(args: JSONObject) = bridge.execute(com.jarvis.app.runtime.CapabilityRequest(
                        "operation.memory", args, com.jarvis.app.runtime.CapabilityRisk.LOW, false))
                    if (action == "forget") call(JSONObject().put("action", action).put("id", id))
                    val data = call(JSONObject().put("action", "list")).data.optJSONArray("recipes") ?: JSONArray()
                    com.jarvis.app.runtime.OperationMemory.at(context.filesDir).summary() to
                        (0 until data.length()).map { data.getJSONObject(it) }
                }
            }
            result.onSuccess { status = it.first; routes = it.second }
                .onFailure { status = "读取失败：${it.message}" }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(1000)
        }
    }
    SettingsCard(Icons.Default.Build, "操作记忆", "首次成功后学习，后续优先复用；失败时重新探索") {
        Text(status, fontSize = 13.sp)
        Text("保存在本机，重装内置运行时仍保留。可在对话中要求查看、更新或删除路径。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { refresh() }) { Text("刷新操作路径") }
        routes.forEach { route ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(route.optString("title"), fontSize = 14.sp)
                    Text("${if (route.optString("status") == "ready") "可复用" else "已失效，待重新学习"} · 成功 ${route.optInt("successes")} / 失败 ${route.optInt("failures")}", fontSize = 12.sp)
                }
                TextButton(onClick = { refresh("forget", route.getString("id")) }) { Text("删除") }
            }
        }
    }
}

@Composable
private fun AccessibilityControlCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember(context) { AccessibilityStartupManager(context) }
    val scope = rememberCoroutineScope()
    var autoConnect by remember { mutableStateOf(manager.autoConnect) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun refresh() {
        status = when {
            AccessibilityController.service != null -> "无障碍已连接，可以操作当前屏幕"
            manager.isEnabled() -> "系统已启用无障碍，等待服务连接"
            else -> "无障碍未启用"
        }
    }
    LaunchedEffect(Unit) { refresh() }

    SettingsCard(Icons.Default.Tune, "屏幕操作", "读取界面、输入中文和执行点击；自动连接需 root 完全模式") {
        Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        SettingSwitch("打开 Jarvis 时自动连接", "需要 root 完全模式；关闭此项不会撤销已有授权", autoConnect) {
            autoConnect = it
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                manager.autoConnect = autoConnect
                status = if (autoConnect) "已保存，正在连接…" else "已保存；下次打开时不自动连接"
                if (autoConnect) scope.launch {
                    busy = true
                    status = withContext(Dispatchers.IO) { manager.connectWithRoot() }
                    busy = false
                }
            }, enabled = !busy, modifier = Modifier.jarvisPressScale()) { Text("保存") }
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }, modifier = Modifier.jarvisPressScale()) { Text("手动授权") }
            TextButton(onClick = ::refresh, modifier = Modifier.jarvisPressScale()) { Text("刷新") }
        }
    }
}

@Composable
private fun RemoteAccessCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember(context) { com.jarvis.app.runtime.RemoteAccessManager(context) }
    var enabled by remember { mutableStateOf(manager.enabled) }
    var deviceControl by remember { mutableStateOf(manager.remoteDeviceControl) }
    var pairing by remember { mutableStateOf(manager.currentPairingInfo()) }
    var urls by remember { mutableStateOf(manager.localUrls()) }
    var notice by remember { mutableStateOf("") }

    fun syncService() {
        val intent = Intent(context, JarvisBridgeService::class.java).setAction(JarvisBridgeService.ACTION_SYNC_REMOTE)
        runCatching { ContextCompat.startForegroundService(context, intent) }
            .onFailure { notice = "服务同步失败：${it.message ?: "请重新打开 Jarvis"}" }
    }

    SettingsCard(Icons.Default.Code, "远程连接", "通过局域网或 Tailscale 访问网页控制台，不镜像屏幕也不发送远程触控") {
        SettingSwitch("启用远程网页控制台", "默认关闭；只允许已配对的浏览器提交任务", enabled) {
            enabled = it
            manager.enabled = it
            if (!it) {
                manager.remoteDeviceControl = false
                deviceControl = false
                manager.revoke()
                pairing = null
                notice = "远程连接已关闭，已有配对令牌已失效"
            } else {
                pairing = manager.newPairingCode()
                urls = manager.localUrls()
                notice = "远程连接已开启，请在 5 分钟内完成配对"
            }
            syncService()
        }
        AnimatedVisibility(visible = enabled, enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.98f, animationSpec = tween(180)), exit = fadeOut(tween(120))) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pairing != null) {
                    Text("一次性配对码", fontWeight = FontWeight.SemiBold)
                    Text(pairing!!.code, style = MaterialTheme.typography.headlineMedium, letterSpacing = 5.sp, color = MaterialTheme.colorScheme.primary)
                    Text("有效至 ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(pairing!!.expiresAt))}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("还没有配对码，点击下方生成。", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = {
                    pairing = manager.newPairingCode()
                    urls = manager.localUrls()
                    notice = "已生成新的 5 分钟配对码，旧码已失效"
                }, modifier = Modifier.jarvisPressScale()) { Text("生成新配对码") }
                SettingSwitch("允许远程设备操作", "关闭时远程端只能对话和整理计划，不会调用无障碍、相机、应用启动或 root 工具", deviceControl) {
                    deviceControl = it
                    manager.remoteDeviceControl = it
                    notice = if (it) "已允许远程任务调用设备能力" else "已禁止远程任务调用设备能力"
                }
                if (urls.isNotEmpty()) {
                    Text("在同一网络设备打开：", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    urls.forEach { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp) }
                } else {
                    Text("暂未发现局域网或 Tailscale 地址；确认手机已连接网络后刷新。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { urls = manager.localUrls(); notice = "地址已刷新" }, modifier = Modifier.jarvisPressScale()) { Text("刷新地址") }
                    TextButton(onClick = {
                        manager.revoke()
                        pairing = null
                        notice = "已撤销所有远程配对，现有浏览器会立即失效"
                    }, modifier = Modifier.jarvisPressScale()) { Text("撤销配对") }
                }
                Text("不要在路由器上做端口映射。Tailscale 请使用手机显示的 100.x 地址；远程任务会和手机本机任务排队，不会同时驱动无障碍服务。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

private fun parseGeneratedPlugin(raw: String): GeneratedOpenClawPlugin {
    val trimmed = raw.trim()
        .removePrefix("```json").removePrefix("```JSON").removePrefix("```")
        .removeSuffix("```").trim()
    val start = trimmed.indexOf('{')
    val end = trimmed.lastIndexOf('}')
    require(start >= 0 && end > start) { "Jarvis 没有返回有效 JSON 插件草稿" }
    val json = JSONObject(trimmed.substring(start, end + 1))
    val tools = json.optJSONArray("tools") ?: JSONArray()
    val toolNames = (0 until tools.length()).map { tools.optString(it) }.filter { it.isNotBlank() }
    return GeneratedOpenClawPlugin(
        id = json.optString("id").lowercase().trim(),
        name = json.optString("name").trim(),
        version = json.optString("version", "1.0.0").trim(),
        description = json.optString("description").trim(),
        tools = toolNames,
        source = json.optString("source")
    )
}

@Composable
private fun CommunityPluginsCard(config: JarvisConfig) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember(context) { CommunityPluginManager(context) }
    val scope = rememberCoroutineScope()
    var plugins by remember { mutableStateOf<List<ManagedOpenClawPlugin>>(emptyList()) }
    var generatedDraft by remember { mutableStateOf<GeneratedOpenClawPlugin?>(null) }
    var request by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var pendingEnable by remember { mutableStateOf<ManagedOpenClawPlugin?>(null) }
    var pendingRemove by remember { mutableStateOf<ManagedOpenClawPlugin?>(null) }

    fun refresh() {
        scope.launch { plugins = withContext(Dispatchers.IO) { manager.list() } }
    }

    fun setPluginEnabled(plugin: ManagedOpenClawPlugin, enabled: Boolean) {
        scope.launch {
            busy = true
            val outcome = runCatching {
                val result = DeviceTaskCoordinator.run("加载插件") {
                    withContext(Dispatchers.IO) { manager.setEnabled(plugin.id, enabled) }
                    OpenClawProcessManager.get(context).start()
                }
                plugins = withContext(Dispatchers.IO) { manager.list() }
                if (result.started) {
                    if (enabled) "${plugin.name} 已启用，OpenClaw Gateway 已重载。"
                    else "${plugin.name} 已停用，OpenClaw Gateway 已重载。"
                } else {
                    "插件状态已保存；Gateway 重载失败：${result.message}"
                }
            }
            outcome.onSuccess { notice = it }.onFailure { notice = "插件状态更新失败：${it.message ?: "未知错误"}" }
            busy = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use(manager::installArchive)
                        ?: error("无法读取插件压缩包")
                }
            }
            result.onSuccess {
                plugins = withContext(Dispatchers.IO) { manager.list() }
                notice = "已导入 ${it.name} ${it.version}；插件当前停用，审阅后再启用。"
            }.onFailure { notice = "插件导入失败：${it.message ?: "压缩包格式不支持"}" }
            busy = false
        }
    }

    fun generateDraft() {
        if (request.isBlank()) {
            notice = "先描述你想添加的插件功能。"
            return
        }
        if (config.modelBaseUrl.isBlank() || config.modelName.isBlank()) {
            notice = "请先在“模型连接”中配置主模型。"
            return
        }
        busy = true
        notice = "Jarvis 正在编写插件草稿…"
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val prompt = buildString {
                        appendLine("根据以下功能需求生成一个 Jarvis 可安装的 OpenClaw JavaScript 插件草稿。")
                        appendLine("需求：${request.trim().take(1200)}")
                        appendLine("只输出一个 JSON 对象，不要 Markdown 或解释，字段为 id、name、version、description、tools（字符串数组）、source（完整 ESM 源码字符串）。")
                        appendLine("id 使用 3–40 位小写字母/数字/短横线，必须以小写字母开头；version 用 1.0.0 形式。tools 至少一个，每个工具名以小写字母开头，只含小写字母、数字和下划线。")
                        appendLine("源码必须 export default 一个 { id, name, register(api) } 插件。每个工具按 api.registerTool(() => ({name,label,description,parameters,execute}), {name}) 注册；parameters 使用 JSON Schema，execute 返回 {content:[{type:'text',text:'结果'}]}。")
                        appendLine("生成纯 JavaScript 工具。不要使用 eval、Function、require、process、文件系统、子进程、网络请求、Android/root/无障碍 API；不要内置密钥、自动发送消息、发布内容或执行破坏操作。工具名必须与 tools 数组完全一致。")
                        appendLine("参考结构：export default { id:'sample-tool', name:'Sample Tool', register(api) { api.registerTool(() => ({ name:'sample_tool', label:'Sample Tool', description:'...', parameters:{type:'object',additionalProperties:false,properties:{}}, execute:async (_id,args)=>({content:[{type:'text',text:'...'}]}) }), {name:'sample_tool'}); } };")
                    }
                    val messages = JSONArray()
                        .put(JSONObject().put("role", "system").put("content", "你是 Jarvis 的 OpenClaw 插件开发器。输出符合用户要求的 JSON 插件草稿，源码需可读、最小化并只实现所描述的功能。"))
                        .put(JSONObject().put("role", "user").put("content", prompt))
                    val response = requestChatCompletion(
                        config.modelBaseUrl, config.modelApiKey,
                        config.deepThinkingModel.takeIf { config.deepThinkingEnabled && it.isNotBlank() } ?: config.modelName,
                        messages,
                        if (config.deepThinkingEnabled) config.reasoningEffort else "off"
                    )
                    parseGeneratedPlugin(response)
                }
            }
            result.onSuccess {
                generatedDraft = it
                notice = "草稿已生成。检查插件标识、工具清单和源码，再手动安装；安装后仍默认停用。"
            }.onFailure { notice = "生成失败：${it.message ?: "请检查模型连接"}" }
            busy = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    SettingsCard(Icons.Default.Extension, "插件管理", "导入 OpenClaw 插件 ZIP，或让 Jarvis 按需求生成源码草稿") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*")) },
                enabled = !busy, modifier = Modifier.weight(1f).jarvisPressScale()) { Text("导入插件 ZIP") }
            OutlinedButton(onClick = { refresh() }, enabled = !busy, modifier = Modifier.jarvisPressScale()) { Text("刷新") }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = request,
            onValueChange = { request = it.take(1200) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            label = { Text("描述插件功能") },
            placeholder = { Text("例如：提供一个把用户输入整理成待办清单的工具") }
        )
        Button(onClick = ::generateDraft, enabled = !busy, modifier = Modifier.padding(top = 8.dp).jarvisPressScale()) {
            Text(if (busy) "处理中…" else "让 Jarvis 编写插件草稿")
        }
        if (generatedDraft != null) {
            val draft = generatedDraft!!
            Spacer(Modifier.height(12.dp))
            Text("插件草稿（安装前可编辑）", fontWeight = FontWeight.SemiBold)
            Text("ID：${draft.id}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text("${draft.name} · ${draft.version} · 工具：${draft.tools.joinToString().ifBlank { "未识别" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = draft.source, onValueChange = { generatedDraft = draft.copy(source = it) },
                modifier = Modifier.fillMaxWidth(), minLines = 8, maxLines = 20, label = { Text("JavaScript 源码") })
            Text("源码会在手机内置 OpenClaw Gateway 进程中运行。导入或生成的代码都应先审阅；启用插件前会再次确认。", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        busy = true
                        val install = withContext(Dispatchers.IO) { runCatching { manager.installGenerated(draft) } }
                        install.onSuccess {
                            plugins = withContext(Dispatchers.IO) { manager.list() }
                            generatedDraft = null
                            notice = "${it.name} 已安装并保持停用。审阅后可在下方启用。"
                        }.onFailure { notice = "安装失败：${it.message ?: "插件草稿无效"}" }
                        busy = false
                    }
                }, enabled = !busy, modifier = Modifier.jarvisPressScale()) { Text("安装草稿（保持停用）") }
                TextButton(onClick = { generatedDraft = null }, enabled = !busy, modifier = Modifier.jarvisPressScale()) { Text("丢弃") }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (plugins.isEmpty()) {
            Text("还没有安装用户插件。导入包需在 ZIP 根目录包含 openclaw.plugin.json、package.json 和 JavaScript 入口文件。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            plugins.forEach { plugin ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(plugin.name, fontWeight = FontWeight.SemiBold)
                            Text("${plugin.id} · v${plugin.version}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = plugin.enabled, onCheckedChange = { enabled ->
                            if (enabled) pendingEnable = plugin else setPluginEnabled(plugin, false)
                        }, enabled = !busy)
                    }
                    if (plugin.description.isNotBlank()) Text(plugin.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("工具：${plugin.tools.joinToString().ifBlank { "未声明" }} · 来源：${if (plugin.source == "generated") "Jarvis 生成" else "本机导入"}", fontSize = 11.sp)
                    TextButton(onClick = { pendingRemove = plugin }, enabled = !busy, modifier = Modifier.jarvisPressScale()) { Text("卸载") }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }

    pendingEnable?.let { plugin ->
        AlertDialog(
            onDismissRequest = { pendingEnable = null },
            title = { Text("启用插件？") },
            text = { Text("${plugin.name} 的 JavaScript 将与 OpenClaw Gateway 在同一进程运行。第三方插件可能联网、读取 Gateway 环境数据，并调用 Gateway 已有设备能力（包括当前配置允许的 root 能力）。Jarvis 无法提供进程级沙箱；请确认已审阅来源和源码。") },
            confirmButton = { Button(onClick = { pendingEnable = null; setPluginEnabled(plugin, true) }) { Text("我已审阅，启用") } },
            dismissButton = { TextButton(onClick = { pendingEnable = null }) { Text("取消") } }
        )
    }
    pendingRemove?.let { plugin ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("卸载 ${plugin.name}？") },
            text = { Text("将删除本机插件文件，并重载 OpenClaw Gateway。") },
            confirmButton = {
                Button(onClick = {
                    pendingRemove = null
                    scope.launch {
                        busy = true
                        val result = runCatching {
                            DeviceTaskCoordinator.run("卸载插件") {
                                withContext(Dispatchers.IO) { manager.remove(plugin.id) }
                                OpenClawProcessManager.get(context).start()
                            }
                        }
                        if (result.isSuccess) {
                            plugins = withContext(Dispatchers.IO) { manager.list() }
                            val reload = result.getOrThrow()
                            notice = if (reload.started) "已卸载 ${plugin.name} 并重载 Gateway。" else "插件已卸载；Gateway 重载失败：${reload.message}"
                        } else notice = "卸载失败：${result.exceptionOrNull()?.message}"
                        busy = false
                    }
                }) { Text("卸载") }
            },
            dismissButton = { TextButton(onClick = { pendingRemove = null }) { Text("取消") } }
        )
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

    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), horizontalAlignment = Alignment.Start) {
        Text("设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("Jarvis ${BuildConfig.VERSION_NAME} · 模型、权限与设备连接", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { ConfigurationActions(config) }
            item { AccessibilityControlCard() }
            item { RemoteAccessCard() }
            item {
                SettingsCard(Icons.Default.Tune, "风格与外观", "图片与控件同步切换，每套主题使用统一色系", saveConfig = config) {
                    ThemePicker(config.theme, onThemeChange)
                }
            }
            item {
                RuntimeStatusCard(bootstrapReport, onRefreshRuntime, runtimeMessage, onRecovery, onStartRuntime, onStopRuntime)
            }
            item { OperationMemoryCard() }
            item {
                SettingsCard(Icons.Default.Shield, "限制模式", "默认开启，控制高风险内容和动作", saveConfig = config) {
                    SettingSwitch("限制模式", "关闭后会持续显示警告状态", config.restrictionMode, onRestrictionChange)
                }
            }
            item {
                SettingsCard(Icons.Default.Lock, "root 模式", "完全模式会在启用前显示三秒警告", saveConfig = config) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RootMode.entries.forEach { mode ->
                            FilterChip(
                                selected = config.rootMode == mode,
                                onClick = { onRootModeRequest(mode) },
                                modifier = Modifier.jarvisPressScale(),
                                label = { Text(mode.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("当前：${config.rootMode.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                SettingsCard(Icons.Default.Terminal, "QQ 命令通道", "指定会话可发送 /claw 命令，其余消息走普通人格", saveConfig = config) {
                    SettingSwitch("启用命令通道", "首版默认只做预览，不直接执行", config.commandChannelEnabled, onCommandChange)
                    AnimatedVisibility(visible = config.commandChannelEnabled, enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.98f, animationSpec = tween(180)), exit = fadeOut(tween(120))) {
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
                SettingsCard(Icons.Default.Build, "模型连接", "兼容 OpenAI Chat Completions 的服务", saveConfig = config, applyGateway = true) {
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
                                        modifier = Modifier.jarvisPressScale(),
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
                SettingsCard(Icons.Default.DarkMode, "识图模型", "用于相机取样、截图和多模态任务，兼容 OpenAI image_url 格式", saveConfig = config) {
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
                        Button(onClick = onImportAiClassroom, modifier = Modifier.weight(1f).jarvisPressScale()) { Text("导入") }
                        OutlinedButton(onClick = onExportAiClassroom, modifier = Modifier.weight(1f).jarvisPressScale()) { Text("导出") }
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
                SettingsCard(Icons.Default.Chat, "全局人格与世界书", "作为所有工作区的默认提示；工作区覆盖能力会在后续工作区编辑器中接入", saveConfig = config) {
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
                CommunityPluginsCard(config)
            }
            item {
                SettingsCard(Icons.Default.Extension, "运行时工具", "检查内置运行时、系统/root 和 Termux 中的命令行工具") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("工具状态", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { refreshTools(); toolNotice = "正在检查工具路径" }, enabled = !checkingTools, modifier = Modifier.jarvisPressScale()) { Text("刷新") }
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
            TextButton(onClick = onInstall, modifier = Modifier.jarvisPressScale()) { Text("安装") }
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
                Button(onClick = onTermux, modifier = Modifier.jarvisPressScale()) { Text("使用 Termux 备用方案") }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onReinstall, modifier = Modifier.jarvisPressScale()) { Text("修复内置运行时并保留数据") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.jarvisPressScale()) { Text("取消") } }
    )
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    saveConfig: JarvisConfig? = null,
    applyGateway: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
            if (saveConfig != null) {
                Spacer(Modifier.height(12.dp))
                SettingsSaveAction(title, saveConfig, applyGateway)
            }
        }
    }
}

@Composable
private fun SettingsSaveAction(section: String, config: JarvisConfig, applyGateway: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val latestConfig by rememberUpdatedState(config)
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(config) {
        if (!busy) notice = ""
    }

    Column(horizontalAlignment = Alignment.Start) {
        OutlinedButton(onClick = {
            val snapshot = config
            if (!ConfigStore(context).save(snapshot, durable = true)) {
                failed = true
                notice = "$section 保存失败，请检查手机存储空间。"
                return@OutlinedButton
            }
            failed = false
            if (applyGateway) {
                busy = true
                notice = "$section 已保存，正在应用到 OpenClaw…"
                scope.launch {
                    val result = runCatching { OpenClawProcessManager.get(context).start() }
                    result.onSuccess {
                        failed = !it.started
                        notice = if (it.started) "$section 已保存并生效。" else "$section 已保存；OpenClaw 尚未就绪：${it.message}"
                    }.onFailure {
                        if (it is kotlinx.coroutines.CancellationException) throw it
                        failed = true
                        notice = "$section 已保存；应用失败，请检查 Gateway 状态。"
                    }
                    if (latestConfig != snapshot) notice += " 有新的修改，请再次保存。"
                    busy = false
                }
            } else {
                notice = "$section 已保存并生效。"
            }
        }, enabled = !busy, modifier = Modifier.jarvisPressScale()) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (busy) "应用中…" else "保存并生效")
        }
        if (notice.isNotBlank()) {
            Text(notice, color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
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
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.jarvisPressScale())
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        ThemeIllustration(title, subtitle,
            compact = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE)
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Button(onClick = onConfirm, enabled = seconds == 0, modifier = Modifier.jarvisPressScale()) { Text("确认启用") }
        },
        dismissButton = { TextButton(onClick = onCancel, modifier = Modifier.jarvisPressScale()) { Text("取消") } }
    )
}
