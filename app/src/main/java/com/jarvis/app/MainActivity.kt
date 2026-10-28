package com.jarvis.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.app.Activity
import android.speech.RecognizerIntent
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
import com.jarvis.app.runtime.JarvisBridgeService
import com.jarvis.app.runtime.DeviceTaskCoordinator
import com.jarvis.app.runtime.ConversationStore
import com.jarvis.app.runtime.ChatMessage
import com.jarvis.app.runtime.ChatMessageRole
import com.jarvis.app.runtime.ChatSession
import com.jarvis.app.runtime.WorkspaceProfile
import com.jarvis.app.runtime.WorkspaceStore
import com.jarvis.app.runtime.CommunityPluginManager
import com.jarvis.app.runtime.GeneratedOpenClawPlugin
import com.jarvis.app.runtime.ManagedOpenClawPlugin
import com.jarvis.app.runtime.RuntimeToolManager
import com.jarvis.app.runtime.RuntimeToolStatus
import com.jarvis.app.runtime.ToolSource
import com.jarvis.app.runtime.ModelConnectionChecker
import com.jarvis.app.runtime.CapabilityResult
import com.jarvis.app.runtime.OperationHost
import com.jarvis.app.runtime.OperationMemory
import com.jarvis.app.runtime.RemoteAccessManager
import com.jarvis.app.runtime.AccessibilityStartupManager
import com.jarvis.app.runtime.AccessibilityController
import com.jarvis.app.runtime.FloatingAssistantService
import com.jarvis.app.runtime.FloatingLaunchAction
import com.jarvis.app.runtime.FloatingAssistantPreferences
import com.jarvis.app.runtime.FloatingShape
import com.jarvis.app.runtime.FloatingGestureAction
import com.jarvis.app.runtime.AutomationDraft
import com.jarvis.app.runtime.AutomationManager
import com.jarvis.app.runtime.AutomationKind
import com.jarvis.app.runtime.AutomationRule
import android.content.Intent
import android.provider.Settings
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
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
import com.jarvis.ui.shell.JarvisShell
import com.jarvis.ui.shell.ShellChrome
import com.jarvis.ui.theme.AutomationRuleView
import com.jarvis.ui.theme.ChatUiContext
import com.jarvis.ui.theme.ChoiceOption
import com.jarvis.ui.theme.FloatingAssistantView
import com.jarvis.ui.theme.JarvisThemeRegistry
import com.jarvis.ui.theme.LegacyThemeMigration
import com.jarvis.ui.theme.MemoryEntryView
import com.jarvis.ui.theme.PluginView
import com.jarvis.ui.theme.PersonaSourceView
import com.jarvis.ui.theme.RecentEventView
import com.jarvis.ui.theme.RootModeOption
import com.jarvis.ui.theme.RuntimeToolView
import com.jarvis.ui.theme.SettingsUiContext
import com.jarvis.ui.theme.ShellUiContext
import com.jarvis.ui.theme.TasksUiContext
import com.jarvis.ui.theme.ThemeMode
import com.jarvis.ui.theme.ThemeModeOverride
import com.jarvis.ui.theme.ThemeOptionView
import com.jarvis.ui.theme.ToolStatusView
import com.jarvis.ui.theme.WorkspaceFileView
import com.jarvis.ui.theme.WorkspaceUiContext
import com.jarvis.ui.theme.forMode
import com.jarvis.ui.theme.resolveMode
import com.jarvis.ui.tokens.JarvisMotion

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

private const val WORKSPACE_PREFS = "jarvis_workspaces"
private const val MAX_WORKSPACE_FILES = 200
private const val MAX_PREVIEW_CHARS = 4000
private const val MAX_PREVIEW_BYTES = 256L * 1024L
private val PREVIEWABLE_EXTENSIONS = setOf(
    "txt", "md", "markdown", "json", "jsonl", "csv", "log", "xml", "yaml", "yml", "ini", "toml",
    "properties", "gradle", "kts", "kt", "java", "py", "js", "ts", "sh", "html", "css"
)

/** 目录显示名：SAF 树 id 的末段（primary:Download/Jarvis -> Jarvis）。 */
private fun directoryLabel(uri: Uri): String {
    val treeId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull().orEmpty()
    val tail = treeId.substringAfterLast(':').trim('/').substringAfterLast('/')
    return tail.ifBlank { "已绑定目录" }
}

/** 是否仍持有该目录的持久化读权限（重启后依然有效才算）。 */
private fun hasDirectoryPermission(context: Context, dirUri: String): Boolean {
    if (dirUri.isBlank()) return false
    return runCatching {
        context.contentResolver.persistedUriPermissions.any { it.uri.toString() == dirUri && it.isReadPermission }
    }.getOrDefault(false)
}

/** 只读扫描结果：文件清单 + 一句提示。 */
private data class WorkspaceFileScan(val files: List<WorkspaceFileView>, val notice: String)

private fun readPreviewHead(reader: java.io.Reader): String {
    val buffer = CharArray(MAX_PREVIEW_CHARS)
    var filled = 0
    while (filled < buffer.size) {
        val read = runCatching { reader.read(buffer, filled, buffer.size - filled) }.getOrDefault(-1)
        if (read <= 0) break
        filled += read
    }
    return String(buffer, 0, filled)
}

/**
 * 用 ContentResolver 列 dirUri 下一层，最多 [MAX_WORKSPACE_FILES] 条，全程只读。
 * 文本文件（文本 MIME 或常见扩展名，大小 < 256KB）带 previewText，最多 [MAX_PREVIEW_CHARS] 字符。
 */
private suspend fun scanWorkspaceFiles(context: Context, dirUri: String): WorkspaceFileScan =
    withContext(Dispatchers.IO) {
        if (dirUri.isBlank()) return@withContext WorkspaceFileScan(emptyList(), "")
        val tree = runCatching { Uri.parse(dirUri) }.getOrNull()
            ?: return@withContext WorkspaceFileScan(emptyList(), "目录地址无效，请重新选择")
        if (!hasDirectoryPermission(context, dirUri)) {
            return@withContext WorkspaceFileScan(emptyList(), "目录读权限已失效，请重新选择该目录")
        }
        val resolver = context.contentResolver
        val childrenUri = runCatching {
            DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        }.getOrNull() ?: return@withContext WorkspaceFileScan(emptyList(), "无法解析该目录")
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID
        )
        runCatching {
            val items = ArrayList<WorkspaceFileView>()
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext() && items.size < MAX_WORKSPACE_FILES) {
                    val name = cursor.getString(0) ?: continue
                    val mime = cursor.getString(1)
                    val size = if (cursor.isNull(2)) -1L else cursor.getLong(2)
                    val modified = if (cursor.isNull(3)) 0L else cursor.getLong(3)
                    val documentId = cursor.getString(4) ?: continue
                    val isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR
                    val textish = mime?.startsWith("text/") == true ||
                        name.substringAfterLast('.', "").lowercase() in PREVIEWABLE_EXTENSIONS
                    val previewable = !isDirectory && textish && (size <= 0L || size < MAX_PREVIEW_BYTES)
                    val previewText = if (previewable) runCatching {
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(tree, documentId)
                        resolver.openInputStream(documentUri)?.let { input ->
                            input.bufferedReader().use { readPreviewHead(it) }
                        }
                    }.getOrNull().orEmpty() else ""
                    items += WorkspaceFileView(
                        name = name,
                        sizeBytes = size,
                        isDirectory = isDirectory,
                        lastModified = modified,
                        previewable = previewText.isNotEmpty(),
                        previewText = previewText
                    )
                }
            }
            items.sortedWith(
                compareByDescending<WorkspaceFileView> { it.isDirectory }.thenBy { it.name.lowercase() }
            )
        }.fold(
            onSuccess = { list ->
                WorkspaceFileScan(list, if (list.size >= MAX_WORKSPACE_FILES) "仅显示前 ${MAX_WORKSPACE_FILES} 项" else "")
            },
            onFailure = { error -> WorkspaceFileScan(emptyList(), "读取目录失败：${error.message ?: "未知错误"}") }
        )
    }

class MainActivity : ComponentActivity() {
    private val quickTaskRequests = Channel<String>(Channel.BUFFERED)
    private val floatingActions = Channel<FloatingLaunchAction>(Channel.BUFFERED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BootstrapController(this).prepareTokens()
        acceptFloatingIntent(intent)
        // Render the shell before any runtime probing so a slow ROM, root check, or
        // missing embedded binary can never leave the activity blank.
        setContent { JarvisApp(quickTaskRequests.receiveAsFlow(), floatingActions.receiveAsFlow()) }

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptFloatingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val enabled = getSharedPreferences("jarvis_config", Context.MODE_PRIVATE)
            .getBoolean("floatingAssistantEnabled", false)
        if (!enabled) FloatingAssistantService.hide(this)
        else if (android.provider.Settings.canDrawOverlays(this)) {
            FloatingAssistantService.show(this)
        }
    }

    private fun acceptFloatingIntent(intent: Intent?) {
        if (intent == null) return
        intent.getStringExtra(EXTRA_QUICK_TASK)?.takeIf { it.isNotBlank() }?.let {
            quickTaskRequests.trySend(it)
            intent.removeExtra(EXTRA_QUICK_TASK)
        }
        intent.getStringExtra(EXTRA_FLOATING_ACTION)?.let { value ->
            FloatingLaunchAction.entries.firstOrNull { it.name == value }?.let(floatingActions::trySend)
            intent.removeExtra(EXTRA_FLOATING_ACTION)
        }
    }

    companion object {
        const val EXTRA_QUICK_TASK = "com.jarvis.app.extra.QUICK_TASK"
        const val EXTRA_FLOATING_ACTION = "com.jarvis.app.extra.FLOATING_ACTION"
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

/**
 * 操作记忆的只读宿主。设置页只用 execute(action=find/forget) 读列表、删记录，
 * 这两个分支不调用 [OperationHost]，其余能力一律拒绝，避免从界面误触发设备操作。
 */
private object MemoryReadOnlyHost : OperationHost {
    override fun call(capability: String, arguments: JSONObject): CapabilityResult =
        CapabilityResult(false, "设置页只读，不能执行设备操作")
    override fun inspect(): JSONObject = JSONObject()
    override fun version(packageName: String): String? = null
    override fun fileState(path: String): JSONObject? = null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JarvisApp(quickTaskRequests: Flow<String>, floatingActions: Flow<FloatingLaunchAction>) {
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
    val workspacePrefs = remember { context.getSharedPreferences(WORKSPACE_PREFS, Context.MODE_PRIVATE) }
    // 一次性迁移：旧版把归档对话 id 存在偏好里，现在落到 ChatSession.archivedAt（conversations.json）。
    // 迁移成功后偏好键被清空，因此重复启动/重组只会执行 0 次迁移。
    var conversations by remember {
        mutableStateOf(
            conversationStore.loadOrCreate().let { loaded ->
                if (conversationStore.migrateArchivedFromPreferences(workspacePrefs) > 0) {
                    conversationStore.loadOrCreate()
                } else loaded
            }
        )
    }
    val workspaceStore = remember { WorkspaceStore(context) }
    var workspaces by remember { mutableStateOf(workspaceStore.load(conversations.map { it.workspace })) }
    // 工作区归档/目录/文件视图状态。
    // 「显示已归档」开关同时作用于工作区列表与对话列表；对话归档标志落在 conversations.json，
    // 不复用偏好（旧偏好在启动时一次性迁移，见 migrateArchivedFromPreferences）。
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var workspaceFiles by remember { mutableStateOf<List<WorkspaceFileView>>(emptyList()) }
    var workspaceFilesLoading by remember { mutableStateOf(false) }
    var workspaceFilesNotice by remember { mutableStateOf("") }
    var workspaceDirReady by remember { mutableStateOf(false) }
    var pendingDirectoryWorkspace by remember { mutableStateOf<String?>(null) }
    var activeConversationId by rememberSaveable { mutableStateOf(conversationStore.activeSessionId(conversations)) }
    // 状态栏/导航栏外观由 JarvisShell 按当前主题模块的明暗统一设置（旧枚举 config.theme.dark 不再参与），
    // 避免两处写入互相覆盖。
    var tab by rememberSaveable { mutableStateOf(JarvisTab.Chat) }
    var draft by rememberSaveable { mutableStateOf("") }
    var editingMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    var showRootWarning by remember { mutableStateOf(false) }
    var pendingRootMode by remember { mutableStateOf<RootMode?>(null) }
    var showRecovery by remember { mutableStateOf(false) }
    var configNotice by remember { mutableStateOf("") }
    var speechNotice by remember { mutableStateOf("") }
    var autoSendSpeech by remember { mutableStateOf(false) }
    var pendingSpeechExecution by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var showNavigation by rememberSaveable { mutableStateOf(false) }
    var showLandscapeRail by rememberSaveable { mutableStateOf(true) }
    var workspaceName by rememberSaveable {
        mutableStateOf(conversations.firstOrNull { it.id == activeConversationId }?.workspace ?: "默认工作区")
    }
    val activeConversation = conversations.firstOrNull { it.id == activeConversationId } ?: conversations.first()
    val messages = activeConversation.messages
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val speechInputLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val transcript = if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty().trim()
        } else ""
        val executeVoiceTask = autoSendSpeech
        autoSendSpeech = false
        if (transcript.isNotBlank()) {
            if (executeVoiceTask) {
                speechNotice = "已识别语音，正在执行任务…"
                pendingSpeechExecution = transcript
            } else {
                draft = listOf(draft.trimEnd(), transcript).filter { it.isNotBlank() }.joinToString(" ")
                speechNotice = "语音已转成文字，请检查后再发送。"
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            speechNotice = "语音输入已取消。"
        } else {
            speechNotice = "没有识别到文字，请重试或检查系统语音识别服务。"
        }
    }

    fun startSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出要发送给 Jarvis 的内容")
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            speechNotice = "设备未提供语音识别服务；安装或启用系统语音输入后可使用。"
            return
        }
        speechNotice = "正在打开系统语音识别…"
        runCatching { speechInputLauncher.launch(intent) }
            .onFailure { speechNotice = "无法启动语音识别：${it.message ?: "系统服务不可用"}" }
    }

    LaunchedEffect(Unit) {
        config = store.load()
    }

    // 当前工作区绑定的 SAF 目录发生变化（切换工作区 / 绑定新目录 / 解除绑定）时重读第一层文件。
    val selectedWorkspaceDirUri = workspaces.firstOrNull { it.name == workspaceName }?.dirUri.orEmpty()
    LaunchedEffect(workspaceName, selectedWorkspaceDirUri) {
        workspaceDirReady = hasDirectoryPermission(context, selectedWorkspaceDirUri)
        if (selectedWorkspaceDirUri.isBlank()) {
            workspaceFiles = emptyList()
            workspaceFilesLoading = false
            workspaceFilesNotice = ""
            return@LaunchedEffect
        }
        workspaceFilesLoading = true
        val scan = scanWorkspaceFiles(context, selectedWorkspaceDirUri)
        workspaceFiles = scan.files
        workspaceFilesNotice = scan.notice
        workspaceFilesLoading = false
        workspaceDirReady = hasDirectoryPermission(context, selectedWorkspaceDirUri)
    }

    fun update(next: JarvisConfig) {
        val themeChanged = next.theme != config.theme
        config = next
        store.save(next)
        if (themeChanged && context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE)
                .getBoolean("floatingAssistantEnabled", false)) FloatingAssistantService.refresh(context)
    }

    fun saveConversation(updated: ChatSession) {
        val next = conversations.map { if (it.id == updated.id) updated else it }.sortedByDescending { it.updatedAt }
        conversations = next
        conversationStore.save(next, activeConversationId)
    }

    fun selectWorkspace(name: String) {
        if (workspaces.none { it.name == name }) return
        workspaceName = name
        val existing = conversations.firstOrNull { it.workspace == name }
        if (existing != null) {
            activeConversationId = existing.id
            conversationStore.save(conversations, existing.id)
            draft = ""
            editingMessageId = null
            tab = JarvisTab.Chat
            showNavigation = false
        } else {
            val created = ChatSession(workspace = name)
            conversations = (listOf(created) + conversations).take(100)
            activeConversationId = created.id
            conversationStore.save(conversations, created.id)
            draft = ""
            editingMessageId = null
            tab = JarvisTab.Chat
            showNavigation = false
        }
    }

    fun saveWorkspace(profile: WorkspaceProfile, previousName: String? = null) {
        val oldName = previousName?.takeIf { it != profile.name }
        val wasSelected = oldName != null && workspaceName == oldName
        workspaces = (workspaces.filterNot { it.name == previousName || it.name == profile.name } + profile)
            .sortedWith(compareBy<WorkspaceProfile> { it.name != WorkspaceProfile.DEFAULT_NAME }.thenBy { it.name })
        workspaceStore.save(workspaces)
        if (oldName != null) {
            conversations = conversations.map { if (it.workspace == oldName) it.copy(workspace = profile.name) else it }
            conversationStore.save(conversations, activeConversationId)
        }
        if (wasSelected || previousName == null) selectWorkspace(profile.name)
    }

    /** 归档/还原工作区：只改 archived 标志，档案与对话全部保留。 */
    fun setWorkspaceArchived(name: String, archived: Boolean) {
        val profile = workspaces.firstOrNull { it.name == name } ?: return
        if (archived && name == WorkspaceProfile.DEFAULT_NAME) {
            workspaceFilesNotice = "默认工作区不能归档，请先切换到别的工作区"
            return
        }
        if (profile.archived != archived) {
            workspaces = workspaces.map { if (it.name == name) it.copy(archived = archived) else it }
            workspaceStore.save(workspaces)
        }
        workspaceFilesNotice = if (archived) "已归档「${profile.name}」，数据保留" else "已还原「${profile.name}」"
        if (archived && workspaceName == name) {
            val fallback = workspaces.firstOrNull { it.name != name && !it.archived }?.name
                ?: WorkspaceProfile.DEFAULT_NAME
            selectWorkspace(fallback)
        }
    }

    /** 目录选择器回调落地：持久化读权限 + 写回 dirUri/dirLabel。 */
    fun bindWorkspaceDirectory(name: String, uri: Uri?) {
        val profile = workspaces.firstOrNull { it.name == name } ?: return
        if (uri == null) {
            workspaceFilesNotice = "未选择目录，绑定未变更"
            return
        }
        val persisted = runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.isSuccess
        val label = directoryLabel(uri)
        saveWorkspace(profile.copy(dirUri = uri.toString(), dirLabel = label), previousName = profile.name)
        workspaceFilesNotice = if (persisted) "已绑定目录「$label」" else "目录已记录，但系统未授予长期读权限，请重新选择"
    }

    fun clearWorkspaceDirectory(name: String) {
        val profile = workspaces.firstOrNull { it.name == name } ?: return
        if (!profile.hasDirectory) {
            workspaceFilesNotice = "该工作区还没有绑定目录"
            return
        }
        saveWorkspace(profile.copy(dirUri = "", dirLabel = ""), previousName = profile.name)
        workspaceFilesNotice = "已解除目录绑定，磁盘文件未改动"
    }

    fun deleteConversation(id: String) {
        val remaining = conversations.filterNot { it.id == id }
        val next = remaining.ifEmpty { listOf(ChatSession(workspace = workspaceName)) }
        conversations = next
        val nextActive = if (activeConversationId == id) next.first().id else activeConversationId
        activeConversationId = nextActive
        conversationStore.save(next, nextActive)
    }

    fun renameConversation(id: String, title: String) {
        val clean = title.trim().take(60)
        if (clean.isEmpty()) return
        conversations = conversations.map { if (it.id == id) it.copy(title = clean) else it }
        conversationStore.save(conversations, activeConversationId)
    }

    fun moveConversation(id: String, targetWorkspace: String) {
        val target = workspaces.firstOrNull { it.name == targetWorkspace } ?: return
        val moved = conversations.map {
            if (it.id == id) it.copy(workspace = target.name, updatedAt = System.currentTimeMillis()) else it
        }
        conversations = moved
        if (activeConversationId == id) workspaceName = target.name
        conversationStore.save(moved, activeConversationId)
    }

    /** 归档/还原对话：archivedAt 落进 conversations.json，消息本身不动。 */
    fun setConversationArchived(id: String, archived: Boolean) {
        if (conversations.none { it.id == id }) return
        val updated = conversationStore.setArchived(conversations, id, archived)
        conversations = updated
        conversationStore.save(updated, activeConversationId)
        if (archived && activeConversationId == id) {
            val next = ConversationStore.visibleSessions(updated).firstOrNull { it.id != id }
            if (next != null) {
                activeConversationId = next.id
                workspaceName = next.workspace
                draft = ""
                editingMessageId = null
                conversationStore.save(conversations, next.id)
            }
        }
    }

    val workspaceDirPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        val target = pendingDirectoryWorkspace
        pendingDirectoryWorkspace = null
        if (target != null) bindWorkspaceDirectory(target, uri)
    }

    fun pickWorkspaceDirectory(name: String) {
        pendingDirectoryWorkspace = name
        runCatching { workspaceDirPickerLauncher.launch(null as Uri?) }
            .onFailure {
                pendingDirectoryWorkspace = null
                workspaceFilesNotice = "无法打开目录选择器：${it.message ?: "系统不支持"}"
            }
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

    fun createBranch(messageId: String? = null) {
        val source = activeConversation
        val branchIndex = source.messages.indexOfFirst { it.id == messageId }
        val created = ChatSession(
            title = "分支 · ${source.title}".take(32),
            workspace = source.workspace,
            messages = if (branchIndex >= 0) source.messages.take(branchIndex + 1) else source.messages
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
        val sessionWorkspace = activeConversation.workspace
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
                content = "已创建自动化草案「${rule.title}」，目前暂停。$extra 触发时会先检查通知内容，再按当前配置执行一次操作。"
            )).takeLast(300), updatedAt = System.currentTimeMillis()))
            return
        }
        sending = true
        runtimeScope.launch {
            val profile = workspaces.firstOrNull { it.name == sessionWorkspace }
            val reply = callJarvisModel(context, config, profile, sessionId, currentMessages)
            val latest = conversations.firstOrNull { it.id == sessionId }
            if (latest != null) saveConversation(latest.copy(
                messages = (latest.messages + ChatMessage(role = ChatMessageRole.ASSISTANT, content = reply)).takeLast(300),
                updatedAt = System.currentTimeMillis()
            ))
            sending = false
        }
    }

    LaunchedEffect(pendingSpeechExecution) {
        val task = pendingSpeechExecution ?: return@LaunchedEffect
        pendingSpeechExecution = null
        sendPrompt(task)
    }

    LaunchedEffect(quickTaskRequests, floatingActions) {
        kotlinx.coroutines.coroutineScope {
            launch {
                quickTaskRequests.collect { task ->
                    tab = JarvisTab.Chat
                    showNavigation = false
                    autoSendSpeech = false
                    sendPrompt(task)
                }
            }
            launch {
                floatingActions.collect { action ->
                    tab = JarvisTab.Chat
                    showNavigation = false
                    when (action) {
                        FloatingLaunchAction.VOICE_TASK -> {
                            autoSendSpeech = true
                            kotlinx.coroutines.delay(250)
                            startSpeechInput()
                        }
                        FloatingLaunchAction.OPEN_MAIN_CHAT -> autoSendSpeech = false
                    }
                }
            }
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

    // ── 阶段 3B 主题装配：theme_id 优先，旧 theme(Int) 作为回退，只有装配层写 theme_id ──────────
    val themePrefs = remember { context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE) }

    fun resolveThemeId(): String {
        val stored = themePrefs.getString("theme_id", null)
        if (stored != null && JarvisThemeRegistry.all.any { it.id == stored }) return stored
        val migrated = LegacyThemeMigration.legacyOrdinalToThemeId(themePrefs.getInt("theme", 0))
        themePrefs.edit().putString("theme_id", migrated).apply()
        return migrated
    }

    var themeId by remember { mutableStateOf(resolveThemeId()) }
    val activeModule = JarvisThemeRegistry.all.firstOrNull { it.id == themeId } ?: JarvisThemeRegistry.default()

    // ── 明暗双模式：mode_override（follow/light/dark）+ 系统明暗 -> 实际生效的明暗 ──────────────
    /** 读持久化明暗偏好；键缺失（老用户只有 theme_id）或读取失败一律回落 follow，不动其主题选择。 */
    fun resolveModeOverride(): ThemeModeOverride =
        ThemeModeOverride.fromKey(runCatching { themePrefs.getString("mode_override", null) }.getOrNull())

    var modeOverride by remember { mutableStateOf(resolveModeOverride()) }
    // isSystemInDarkTheme() 跟随配置变化：系统切明/切暗会触发重组，effectiveMode 随之更新。
    val effectiveMode = resolveMode(modeOverride, isSystemInDarkTheme())

    /** 明暗偏好权威入口：写 mode_override，并让浮窗立即按新明暗重绘。 */
    fun switchModeOverride(next: ThemeModeOverride) {
        themePrefs.edit().putString("mode_override", next.key).apply()
        modeOverride = next
        if (themePrefs.getBoolean("floatingAssistantEnabled", false)) FloatingAssistantService.refresh(context)
    }

    /** 主题选择的权威入口：只认 registry 里的 id，能覆盖全部 5 套。 */
    fun switchThemeById(id: String) {
        if (JarvisThemeRegistry.all.none { it.id == id }) return
        themePrefs.edit().putString("theme_id", id).apply()
        themeId = id
        // 兜底：浮窗只在自己收到事件时才比对 theme_id，这里主动推一次刷新让头像/配色立即跟随。
        if (themePrefs.getBoolean("floatingAssistantEnabled", false)) FloatingAssistantService.refresh(context)
        // 5 套主题与旧枚举一一对应：只有落在 legacy 顺序里时才回写 config.theme，
        // 否则 config.theme 原样保留（它现在只服务于未回填的旧界面配色）。
        val legacyOrdinal = LegacyThemeMigration.legacyThemeOrder.indexOf(id)
        if (legacyOrdinal >= 0) {
            JarvisTheme.entries.getOrNull(legacyOrdinal)?.let { legacy ->
                if (legacy != config.theme) update(config.copy(theme = legacy))
            }
        }
    }

    /** 旧枚举通道：映射到同一套 id 逻辑，避免两条切换路径写出不同的 theme_id。 */
    fun switchTheme(legacy: JarvisTheme) {
        switchThemeById(LegacyThemeMigration.legacyOrdinalToThemeId(legacy.ordinal))
    }

    // ── 任务页状态：由外壳持有，主题屏只读（主题不再自己 new AutomationManager 读授权态）───────
    val automationManager = remember { AutomationManager(context) }
    var taskRules by remember { mutableStateOf(automationManager.list()) }
    var taskEvents by remember { mutableStateOf(automationManager.recentEvents()) }
    var taskNotice by remember { mutableStateOf("") }
    var taskEditingRuleId by remember { mutableStateOf<String?>(null) }
    var taskArtistDraft by remember { mutableStateOf("") }
    var taskReplyDraft by remember { mutableStateOf("") }

    // 任务页「直接新建草案」表单状态：与聊天里 parse 后自动创建并存，两条路径互不影响。
    var ruleCreating by remember { mutableStateOf(false) }
    var ruleDraftTitle by remember { mutableStateOf("") }
    var ruleDraftKindKey by remember { mutableStateOf(AutomationKind.BILIBILI_REPLY.name) }
    var ruleDraftPrompt by remember { mutableStateOf("") }
    var ruleDraftArtist by remember { mutableStateOf("") }
    var ruleDraftError by remember { mutableStateOf("") }
    val ruleKindOptions = remember {
        AutomationKind.entries.map { ChoiceOption(key = it.name, label = it.label) }
    }
    var taskNotificationAccess by remember { mutableStateOf(automationManager.hasNotificationAccess()) }
    var taskAccessibility by remember { mutableStateOf(AccessibilityController.service != null) }

    fun refreshTasks() {
        taskRules = automationManager.list()
        taskEvents = automationManager.recentEvents()
        taskNotificationAccess = automationManager.hasNotificationAccess()
        taskAccessibility = AccessibilityController.service != null
    }

    fun resetRuleDraftForm() {
        ruleCreating = false
        ruleDraftTitle = ""
        ruleDraftKindKey = AutomationKind.BILIBILI_REPLY.name
        ruleDraftPrompt = ""
        ruleDraftArtist = ""
        ruleDraftError = ""
    }

    /** 任务页直接新建草案的落库入口：标题/触发要求必填，缺失写 ruleDraftError，不静默失败。 */
    fun submitRuleDraft() {
        val title = ruleDraftTitle.trim()
        val prompt = ruleDraftPrompt.trim()
        val kind = AutomationKind.entries.firstOrNull { it.name == ruleDraftKindKey } ?: AutomationKind.BILIBILI_REPLY
        val missing = when {
            title.isEmpty() -> "请先填写草案标题"
            prompt.isEmpty() -> "请先填写触发要求（什么通知该触发、要回复什么）"
            else -> null
        }
        if (missing != null) {
            ruleDraftError = missing
            return
        }
        runCatching {
            automationManager.create(
                AutomationDraft(kind = kind, title = title, sourcePrompt = prompt, artist = ruleDraftArtist.trim())
            )
        }.onSuccess {
            ruleDraftError = ""
            ruleCreating = false
            ruleDraftTitle = ""
            ruleDraftPrompt = ""
            ruleDraftArtist = ""
            taskNotice = "已创建草案「$title」，默认暂停，请检查后启用"
            refreshTasks()
        }.onFailure { error ->
            ruleDraftError = "创建失败：${error.message ?: "无法写入自动化规则"}"
        }
    }

    fun ruleView(rule: AutomationRule) = AutomationRuleView(
        id = rule.id,
        title = rule.title,
        kindKey = rule.kind.name,
        kindLabel = rule.kind.label,
        sourcePrompt = rule.sourcePrompt,
        artist = rule.artist,
        replyInstruction = rule.replyInstruction,
        enabled = rule.enabled,
        lastResult = rule.lastResult,
        lastRunAt = rule.lastRunAt,
        readiness = if (rule.enabled) null else automationManager.readiness(rule)
    )

    LaunchedEffect(tab) {
        if (tab != JarvisTab.Tasks) return@LaunchedEffect
        while (true) {
            refreshTasks()
            delay(1500)
        }
    }

    // ── 设置页：工具检查 / 悬浮助手 / 保存并应用 ─────────────────────────────────────────
    val runtimeToolManager = remember { RuntimeToolManager(context) }
    var toolStatuses by remember { mutableStateOf<List<RuntimeToolStatus>>(emptyList()) }
    var checkingTools by remember { mutableStateOf(true) }
    var toolNotice by remember { mutableStateOf("") }
    var applying by remember { mutableStateOf(false) }
    var applyNotice by remember { mutableStateOf("") }
    var applyFailed by remember { mutableStateOf(false) }
    var showRuntimeTools by remember { mutableStateOf(false) }

    fun refreshTools() {
        checkingTools = true
        runtimeScope.launch {
            toolStatuses = withContext(Dispatchers.IO) { runtimeToolManager.inspect() }
            checkingTools = false
        }
    }

    LaunchedEffect(Unit) { refreshTools() }

    val accessibilityStartup = remember { AccessibilityStartupManager(context) }
    var accessibilityAutoConnect by remember { mutableStateOf(accessibilityStartup.autoConnect) }
    var floatingPreferences by remember { mutableStateOf(FloatingAssistantPreferences.load(context)) }
    var floatingEnabled by remember { mutableStateOf(themePrefs.getBoolean("floatingAssistantEnabled", false)) }

    fun saveFloatingPreferences(next: FloatingAssistantPreferences) {
        floatingPreferences = next
        next.save(context)
        FloatingAssistantService.refresh(context)
    }

    fun setFloatingEnabled(enabled: Boolean) {
        floatingEnabled = enabled
        themePrefs.edit().putBoolean("floatingAssistantEnabled", enabled).apply()
        FloatingAssistantService.refresh(context)
    }

    // ── P1-1 远程访问：RemoteAccessManager 只存开关与配对令牌，HTTP 监听在 Bridge 服务里按 ACTION_SYNC_REMOTE 同步 ──
    val remoteAccess = remember { RemoteAccessManager(context) }
    var remoteServerEnabled by remember { mutableStateOf(remoteAccess.enabled) }
    var remotePairing by remember { mutableStateOf(remoteAccess.currentPairingInfo()) }
    var remoteDeviceControl by remember { mutableStateOf(remoteAccess.remoteDeviceControl) }
    var remoteUrls by remember { mutableStateOf(remoteAccess.localUrls()) }

    fun syncRemoteServer() {
        remoteUrls = remoteAccess.localUrls()
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, JarvisBridgeService::class.java).setAction(JarvisBridgeService.ACTION_SYNC_REMOTE)
            )
        }
    }

    // ── P1-1 社区插件：list/install/uninstall/toggle 都走 CommunityPluginManager ────────────
    val communityPlugins = remember { CommunityPluginManager(context) }
    var pluginList by remember { mutableStateOf(communityPlugins.list()) }
    var pluginNotice by remember { mutableStateOf("") }
    var pluginBusy by remember { mutableStateOf(false) }

    fun refreshPlugins() {
        runCatching { communityPlugins.list() }
            .onSuccess { pluginList = it }
            .onFailure { pluginNotice = "读取插件列表失败：${it.message ?: "插件目录不可用"}" }
    }

    fun installPlugin(raw: String) {
        if (pluginBusy) return
        pluginBusy = true
        runtimeScope.launch {
            pluginNotice = withContext(Dispatchers.IO) {
                runCatching { communityPlugins.installGenerated(parseGeneratedPlugin(raw)) }
                    .fold(
                        onSuccess = { "已安装「${it.name}」，默认停用；确认来源后再启用。" },
                        onFailure = { "安装失败：${it.message ?: "插件草稿无效"}" }
                    )
            }
            pluginBusy = false
            refreshPlugins()
        }
    }

    fun uninstallPlugin(id: String) {
        if (pluginBusy) return
        pluginBusy = true
        runtimeScope.launch {
            pluginNotice = withContext(Dispatchers.IO) {
                runCatching { communityPlugins.remove(id) }
                    .fold(onSuccess = { "已卸载插件" }, onFailure = { "卸载失败：${it.message ?: "插件目录不可用"}" })
            }
            pluginBusy = false
            refreshPlugins()
        }
    }

    fun togglePlugin(id: String, enabled: Boolean) {
        if (pluginBusy) return
        pluginBusy = true
        runtimeScope.launch {
            pluginNotice = withContext(Dispatchers.IO) {
                runCatching { communityPlugins.setEnabled(id, enabled) }
                    .fold(
                        onSuccess = { if (enabled) "插件已启用" else "插件已停用" },
                        onFailure = { "切换失败：${it.message ?: "插件清单不合法"}" }
                    )
            }
            pluginBusy = false
            refreshPlugins()
        }
    }

    // ── P1-1 操作记忆：只读宿主，列表/删除走 OperationMemory.execute(find/forget) ────────────
    val operationMemory = remember { OperationMemory.at(context.filesDir) }
    var memoryEntries by remember { mutableStateOf<List<MemoryEntryView>>(emptyList()) }
    var memoryStatusText by remember { mutableStateOf("") }

    fun refreshMemory() {
        memoryStatusText = runCatching { operationMemory.summary() }
            .getOrElse { "操作记忆暂不可用：${it.message}" }
        memoryEntries = runCatching {
            val found = operationMemory.execute(
                JSONObject().put("action", "find").put("query", ""),
                MemoryReadOnlyHost
            )
            val recipes = found.data.optJSONArray("recipes") ?: JSONArray()
            (0 until recipes.length()).map { index ->
                val item = recipes.optJSONObject(index) ?: JSONObject()
                MemoryEntryView(
                    id = item.optString("id"),
                    title = item.optString("title"),
                    ready = item.optString("status") == "ready",
                    successes = item.optInt("successes"),
                    failures = item.optInt("failures")
                )
            }
        }.getOrDefault(emptyList())
    }

    LaunchedEffect(Unit) { refreshMemory() }

    fun applySettings(verify: Boolean) {
        if (applying) return
        val snapshot = config
        if (!store.save(snapshot, durable = true)) {
            applyFailed = true
            applyNotice = "保存失败，请检查手机存储空间。"
            return
        }
        applying = true
        applyFailed = false
        applyNotice = "配置已保存，正在应用到 OpenClaw…"
        runtimeScope.launch {
            runCatching { withContext(Dispatchers.IO) { processManager.start() } }
                .onSuccess { result ->
                    applyFailed = !result.started
                    applyNotice = if (result.started) "配置已保存并生效。" else "配置已保存；OpenClaw 尚未就绪：${result.message}"
                }
                .onFailure { error ->
                    applyFailed = true
                    applyNotice = "配置已保存；应用失败：${error.message ?: "请检查 Gateway 状态"}"
                }
            if (verify) {
                val check = withContext(Dispatchers.IO) {
                    ModelConnectionChecker.test(snapshot.modelBaseUrl, snapshot.modelApiKey, snapshot.modelName)
                }
                if (!check.ok) applyFailed = true
                applyNotice = applyNotice + " 模型验证：${check.message}"
            }
            bootstrapReport = withContext(Dispatchers.IO) { bootstrap.bootstrap() }
            applying = false
        }
    }

    // 四个 UiContext 都携带同一份 shell 上下文，主题屏据此接自己的底栏（P0-1）。
    val shellContext = ShellUiContext(
        tabIndex = tab.ordinal,
        tabs = ShellUiContext.DefaultTabs,
        onNavigate = { index ->
            JarvisTab.entries.getOrNull(index)?.let { next ->
                tab = next
                showNavigation = false
            }
        },
        // 第 5 项「浮窗」：直接唤起真正的悬浮球；没有悬浮窗权限时退到设置页并申请权限（P0-5）。
        onOpenFloating = {
            if (Settings.canDrawOverlays(context)) {
                setFloatingEnabled(true)
                FloatingAssistantService.show(context)
            } else {
                tab = JarvisTab.Settings
                showNavigation = false
                configNotice = "浮窗需要「显示在其他应用上层」权限；已为你申请，授权后再点一次浮窗。"
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    )
                }
            }
        }
    )

    // 归档默认隐藏，由「显示已归档」开关决定是否出现（工作区与对话同一开关）。
    val visibleWorkspaces = if (showArchived) workspaces else workspaces.filterNot { it.archived }
    val visibleConversations = ConversationStore.visibleSessions(conversations, showArchived)

    Crossfade(targetState = activeModule.id, animationSpec = tween(JarvisMotion.Slow), label = "theme-transition") { activeThemeId ->
        // 外壳只读 module.palette 与 module.mode；forMode 把两者换成运行期实际明暗，
        // 于是状态栏图标与整套配色一起跟随系统，不需要改 ui/shell。
        val module = remember(activeThemeId, effectiveMode) {
            (JarvisThemeRegistry.all.firstOrNull { it.id == activeThemeId } ?: activeModule).forMode(effectiveMode)
        }
        CompositionLocalProvider(LocalJarvisTheme provides config.theme) {
            JarvisShell(
                module = module,
                shell = shellContext,
                chat = ChatUiContext(
                    state = ChatUiContext.ChatState(
                        contentPadding = PaddingValues(0.dp),
                        conversationId = activeConversation.id,
                        messages = messages,
                        draft = draft,
                        sending = sending,
                        speechNotice = speechNotice,
                        editingLatest = editingMessageId != null,
                        canUndoLatest = messages.isNotEmpty() && !sending,
                        canEditLatest = messages.any { it.role == ChatMessageRole.USER } && !sending,
                        reasoningEffort = config.reasoningEffort,
                        reasoningEfforts = reasoningEfforts,
                        toolboxVisible = showRuntimeTools
                    ),
                    callbacks = ChatUiContext.ChatCallbacks(
                        onDraftChange = { draft = it },
                        onSend = { prompt -> sendPrompt(prompt.trim()) },
                        onVoiceInput = ::startSpeechInput,
                        onNewConversation = ::createConversation,
                        onBranch = ::createBranch,
                        onUndoLatest = ::undoLatestTurn,
                        onEditLatest = ::editLatestUserMessage,
                        onCancelEdit = ::cancelEdit,
                        onReasoningEffortChange = { effort ->
                            val normalized = normalizeReasoningEffort(effort)
                            update(config.copy(reasoningEffort = normalized, deepThinkingEnabled = normalized != "off"))
                        },
                        onOpenTools = { showRuntimeTools = true },
                        onCloseTools = { showRuntimeTools = false }
                    ),
                    shell = shellContext
                ),
                tasks = TasksUiContext(
                    state = TasksUiContext.TasksState(
                        contentPadding = PaddingValues(0.dp),
                        rules = taskRules.map(::ruleView),
                        recentEvents = taskEvents.map { event ->
                            RecentEventView(
                                id = event.optString("id"),
                                title = event.optString("title"),
                                message = event.optString("message"),
                                ok = event.optBoolean("ok"),
                                occurredAt = event.optLong("at")
                            )
                        },
                        notificationAccess = taskNotificationAccess,
                        accessibilityGranted = taskAccessibility,
                        runtimeReady = bootstrapReport.state == RuntimeState.READY || bootstrapReport.state == RuntimeState.RUNNING,
                        notice = taskNotice,
                        editingRuleId = taskEditingRuleId,
                        artistDraft = taskArtistDraft,
                        replyDraft = taskReplyDraft,
                        creatingRule = ruleCreating,
                        ruleDraftTitle = ruleDraftTitle,
                        ruleDraftKindKey = ruleDraftKindKey,
                        ruleDraftKindOptions = ruleKindOptions,
                        ruleDraftPrompt = ruleDraftPrompt,
                        ruleDraftArtist = ruleDraftArtist,
                        ruleDraftError = ruleDraftError
                    ),
                    callbacks = TasksUiContext.TasksCallbacks(
                        onToggleRule = { id, enabled ->
                            runCatching { automationManager.setEnabled(id, enabled) }
                                .onSuccess { taskNotice = if (enabled) "已启用规则" else "已暂停规则" }
                                .onFailure { taskNotice = "无法启用：${it.message ?: "请先完成权限和配置"}" }
                            refreshTasks()
                        },
                        onDeleteRule = { id ->
                            runCatching { automationManager.delete(id) }
                            taskNotice = "已删除自动化规则"
                            refreshTasks()
                        },
                        onConfigureRule = { id ->
                            automationManager.findRule(id)?.let { rule ->
                                taskEditingRuleId = rule.id
                                taskArtistDraft = rule.artist
                                taskReplyDraft = rule.replyInstruction
                            }
                        },
                        onSaveRuleConfig = { id, artist, reply ->
                            automationManager.findRule(id)?.let { rule ->
                                runCatching { automationManager.update(rule.copy(artist = artist.trim(), replyInstruction = reply.trim())) }
                                    .onSuccess { taskNotice = "配置已保存；规则仍保持暂停，请检查后启用" }
                                    .onFailure { taskNotice = "保存失败：${it.message}" }
                            }
                            taskEditingRuleId = null
                            refreshTasks()
                        },
                        onCancelRuleConfig = { taskEditingRuleId = null },
                        onRequestNotificationAccess = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onRequestAccessibility = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onRefresh = ::refreshTasks,
                        onCreateRuleStart = {
                            ruleCreating = true
                            ruleDraftError = ""
                            taskNotice = ""
                        },
                        onCreateRuleTitleChange = { ruleDraftTitle = it },
                        onCreateRuleKindChange = { ruleDraftKindKey = it },
                        onCreateRulePromptChange = { ruleDraftPrompt = it },
                        onCreateRuleArtistChange = { ruleDraftArtist = it },
                        onCreateRuleSubmit = { submitRuleDraft() },
                        onCreateRuleCancel = { resetRuleDraftForm() }
                    ),
                    shell = shellContext
                ),
                workspace = WorkspaceUiContext(
                    state = WorkspaceUiContext.WorkspaceState(
                        contentPadding = PaddingValues(0.dp),
                        // 归档的工作区/对话默认不进列表，只有打开「显示已归档」才出现。
                        workspaces = visibleWorkspaces,
                        conversations = visibleConversations,
                        selectedWorkspace = workspaceName,
                        globalPersona = config.mentorPrompt,
                        globalWorldBook = config.worldBook,
                        conversationCountByWorkspace = visibleConversations.groupingBy { it.workspace }.eachCount(),
                        personaSources = conversations.map { session ->
                            val profile = workspaces.firstOrNull { it.name == session.workspace }
                            PersonaSourceView(
                                sessionId = session.id,
                                workspace = session.workspace,
                                title = session.title,
                                mentorPrompt = profile?.resolvedMentorPrompt(config.mentorPrompt) ?: config.mentorPrompt,
                                worldBook = profile?.resolvedWorldBook(config.worldBook) ?: config.worldBook
                            )
                        },
                        files = workspaceFiles,
                        filesLoading = workspaceFilesLoading,
                        filesNotice = workspaceFilesNotice,
                        dirReady = workspaceDirReady,
                        showArchived = showArchived,
                        archivedWorkspaces = workspaces.filter { it.archived },
                        archivedConversationIds = ConversationStore.archivedIds(conversations)
                    ),
                    callbacks = WorkspaceUiContext.WorkspaceCallbacks(
                        onSelect = ::selectWorkspace,
                        onSave = { profile, previous -> saveWorkspace(profile, previous) },
                        onDelete = { name ->
                            val remaining = workspaces.filterNot { it.name == name }
                            // 默认工作区必须存在（WorkspaceStore.save 有 require）；删它就等于清空，故忽略。
                            if (name != WorkspaceProfile.DEFAULT_NAME) {
                                workspaces = remaining
                                workspaceStore.save(workspaces)
                                if (workspaceName == name) selectWorkspace(WorkspaceProfile.DEFAULT_NAME)
                            } else {
                                workspaceFilesNotice = "默认工作区不能删除"
                            }
                        },
                        onArchive = { name -> setWorkspaceArchived(name, true) },
                        onUnarchive = { name -> setWorkspaceArchived(name, false) },
                        onPickDirectory = ::pickWorkspaceDirectory,
                        onClearDirectory = ::clearWorkspaceDirectory,
                        onToggleShowArchived = { showArchived = it },
                        onMoveConversation = { id, target -> moveConversation(id, target) },
                        onDeleteConversation = ::deleteConversation,
                        onArchiveConversation = { id -> setConversationArchived(id, true) },
                        onUnarchiveConversation = { id -> setConversationArchived(id, false) },
                        onRenameConversation = { id, title -> renameConversation(id, title) }
                    ),
                    shell = shellContext
                ),
                settings = SettingsUiContext(
                    state = SettingsUiContext.SettingsState(
                        contentPadding = PaddingValues(0.dp),
                        restrictionMode = config.restrictionMode,
                        commandChannelEnabled = config.commandChannelEnabled,
                        commandPrefix = config.commandPrefix,
                        modelBaseUrl = config.modelBaseUrl,
                        modelName = config.modelName,
                        modelApiKey = config.modelApiKey,
                        modelChain = config.modelChain,
                        deepThinkingEnabled = config.deepThinkingEnabled,
                        reasoningEffort = config.reasoningEffort,
                        deepThinkingModel = config.deepThinkingModel,
                        visionBaseUrl = config.visionBaseUrl,
                        visionApiKey = config.visionApiKey,
                        visionModel = config.visionModel,
                        mentorPrompt = config.mentorPrompt,
                        worldBook = config.worldBook,
                        theme = config.theme,
                        themeId = themeId,
                        modeOverride = modeOverride,
                        effectiveMode = effectiveMode,
                        rootModes = RootMode.entries.map { RootModeOption(key = it.name, label = it.label) },
                        rootModeKey = config.rootMode.name,
                        bootstrapReport = bootstrapReport,
                        runtimeMessage = runtimeMessage,
                        configNotice = configNotice,
                        themeOptions = JarvisThemeRegistry.all.map { item ->
                            ThemeOptionView(
                                id = item.id,
                                label = item.label,
                                story = item.story,
                                description = item.description,
                                modeKey = if (item.mode == ThemeMode.LIGHT) "light" else "dark"
                            )
                        },
                        reasoningEfforts = reasoningEfforts,
                        versionName = BuildConfig.VERSION_NAME,
                        avatarSizeDp = floatingPreferences.sizeDp,
                        avatarHideSeconds = floatingPreferences.hideSeconds,
                        floatingAssistant = FloatingAssistantView(
                            enabled = floatingEnabled,
                            sizeDp = floatingPreferences.sizeDp,
                            shape = floatingPreferences.shape.name.lowercase(),
                            opacity = 1f,
                            hideSeconds = floatingPreferences.hideSeconds
                        ),
                        floatingEnabled = floatingEnabled,
                        floatingSizeDp = floatingPreferences.sizeDp,
                        floatingShapeKey = floatingPreferences.shape.name.lowercase(),
                        floatingShapes = FloatingShape.entries.map { ChoiceOption(it.name.lowercase(), it.label) },
                        overlayGranted = Settings.canDrawOverlays(context),
                        accessibilityGranted = AccessibilityController.service != null,
                        doubleGestureKey = floatingPreferences.doubleTap.name.lowercase(),
                        tripleGestureKey = floatingPreferences.tripleTap.name.lowercase(),
                        doubleTask = floatingPreferences.doubleTapTask,
                        tripleTask = floatingPreferences.tripleTapTask,
                        gestureOptions = FloatingGestureAction.entries.map { ChoiceOption(it.name.lowercase(), it.label) },
                        savedAutomations = taskRules.map(::ruleView),
                        runtimeTools = toolStatuses.map { status ->
                            RuntimeToolView(
                                name = status.label,
                                installed = status.source != ToolSource.MISSING,
                                version = status.source.label
                            )
                        },
                        tools = toolStatuses.map { status ->
                            ToolStatusView(
                                id = status.id,
                                label = status.label,
                                description = status.description,
                                sourceLabel = status.source.label,
                                path = status.path,
                                installPackage = status.installPackage
                            )
                        },
                        checkingTools = checkingTools,
                        cmakeReady = toolStatuses.any { it.id == "cmake" && it.source != ToolSource.MISSING },
                        ffmpegReady = toolStatuses.any { it.id == "ffmpeg" && it.source != ToolSource.MISSING },
                        toolNotice = toolNotice,
                        serverRunning = remoteServerEnabled,
                        pairingCode = remotePairing?.code.orEmpty(),
                        pairingExpiresAt = remotePairing?.expiresAt ?: 0L,
                        remoteDeviceControl = remoteDeviceControl,
                        remoteUrls = remoteUrls,
                        plugins = pluginList.map { plugin ->
                            PluginView(
                                id = plugin.id,
                                name = plugin.name,
                                version = plugin.version,
                                description = plugin.description,
                                tools = plugin.tools,
                                enabled = plugin.enabled,
                                source = plugin.source
                            )
                        },
                        pluginNotice = pluginNotice,
                        pluginBusy = pluginBusy,
                        memoryEntries = memoryEntries,
                        memoryStatus = memoryStatusText,
                        accessibilityAutoConnect = accessibilityAutoConnect,
                        accessibilityStatus = when {
                            AccessibilityController.service != null -> "无障碍已连接，可以操作当前屏幕"
                            accessibilityStartup.isEnabled() -> "系统已启用无障碍，等待服务连接"
                            else -> "无障碍未启用"
                        },
                        importNotice = configNotice,
                        exportNotice = configNotice,
                        saving = applying,
                        applyNotice = applyNotice,
                        applyFailed = applyFailed
                    ),
                    callbacks = SettingsUiContext.SettingsCallbacks(
                        onRefreshRuntime = {
                            runtimeScope.launch { bootstrapReport = withContext(Dispatchers.IO) { bootstrap.bootstrap() } }
                        },
                        onRecovery = { showRecovery = true },
                        onStartRuntime = {
                            runtimeScope.launch {
                                runtimeMessage = withContext(Dispatchers.IO) { processManager.start().message }
                                bootstrapReport = withContext(Dispatchers.IO) { bootstrap.bootstrap() }
                            }
                        },
                        onStopRuntime = {
                            runtimeScope.launch {
                                withContext(Dispatchers.IO) { processManager.stop() }
                                runtimeMessage = "OpenClaw Gateway 已停止"
                                bootstrapReport = withContext(Dispatchers.IO) { bootstrap.bootstrap() }
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
                            val effort = when {
                                enabled && config.reasoningEffort == "off" -> "medium"
                                !enabled -> "off"
                                else -> config.reasoningEffort
                            }
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
                        onThemeChange = ::switchTheme,
                        onModeOverrideChange = ::switchModeOverride,
                        onImportAiClassroom = {
                            importConfigLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        onExportAiClassroom = { exportConfigLauncher.launch("jarvis-ai-classroom-config.json") },
                        onRootModeRequest = { option ->
                            val mode = RootMode.entries.firstOrNull { it.name == option.key }
                            if (mode != null) {
                                pendingRootMode = mode
                                if (mode == RootMode.Full) showRootWarning = true else update(config.copy(rootMode = mode))
                            }
                        },
                        onAvatarSizeChange = { saveFloatingPreferences(floatingPreferences.copy(sizeDp = it)) },
                        onAvatarHideSecondsChange = { saveFloatingPreferences(floatingPreferences.copy(hideSeconds = it)) },
                        onFloatingEnabledChange = ::setFloatingEnabled,
                        onFloatingSizeChange = { saveFloatingPreferences(floatingPreferences.copy(sizeDp = it)) },
                        onFloatingShapeChange = { key ->
                            FloatingShape.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
                                ?.let { saveFloatingPreferences(floatingPreferences.copy(shape = it)) }
                        },
                        onOverlayPermissionRequest = {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            )
                        },
                        onAccessibilitySettingsOpen = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onDoubleGestureChange = { key ->
                            FloatingGestureAction.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
                                ?.let { saveFloatingPreferences(floatingPreferences.copy(doubleTap = it)) }
                        },
                        onTripleGestureChange = { key ->
                            FloatingGestureAction.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
                                ?.let { saveFloatingPreferences(floatingPreferences.copy(tripleTap = it)) }
                        },
                        onDoubleTaskChange = { saveFloatingPreferences(floatingPreferences.copy(doubleTapTask = it)) },
                        onTripleTaskChange = { saveFloatingPreferences(floatingPreferences.copy(tripleTapTask = it)) },
                        onSelectAutomationForGesture = { gestureKey, ruleId ->
                            val rule = taskRules.firstOrNull { it.id == ruleId }
                            if (rule != null) {
                                val isDouble = floatingPreferences.doubleTap.name.equals(gestureKey, ignoreCase = true)
                                saveFloatingPreferences(
                                    if (isDouble) floatingPreferences.copy(doubleTap = FloatingGestureAction.Task, doubleTapTask = rule.sourcePrompt)
                                    else floatingPreferences.copy(tripleTap = FloatingGestureAction.Task, tripleTapTask = rule.sourcePrompt)
                                )
                            }
                        },
                        onRefreshTools = ::refreshTools,
                        onCheckTools = ::refreshTools,
                        onInstallTool = { key ->
                            toolStatuses.firstOrNull { it.id == key || it.label == key }?.let { status ->
                                runtimeScope.launch {
                                    val result = withContext(Dispatchers.IO) { runtimeToolManager.install(status) }
                                    toolNotice = result.message
                                    if (result.success) refreshTools()
                                }
                            }
                        },
                        onAccessibilityAutoConnectChange = { enabled ->
                            accessibilityAutoConnect = enabled
                            accessibilityStartup.autoConnect = enabled
                            if (enabled) {
                                runtimeScope.launch {
                                    val message = withContext(Dispatchers.IO) { accessibilityStartup.connectWithRoot() }
                                    runtimeMessage = message
                                }
                            }
                        },
                        onApply = { applySettings(verify = false) },
                        onApplyAndVerify = { applySettings(verify = true) },
                        onThemeChangeById = ::switchThemeById,
                        onStartServer = {
                            remoteAccess.enabled = true
                            remoteServerEnabled = true
                            remoteDeviceControl = remoteAccess.remoteDeviceControl
                            syncRemoteServer()
                        },
                        onStopServer = {
                            remoteAccess.enabled = false
                            remoteAccess.revoke()
                            remoteServerEnabled = false
                            remotePairing = null
                            syncRemoteServer()
                        },
                        onNewPairingCode = { remotePairing = remoteAccess.newPairingCode() },
                        onCopyPairingCode = {
                            val code = remotePairing?.code
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            if (code != null && clipboard != null) {
                                clipboard.setPrimaryClip(ClipData.newPlainText("Jarvis 配对码", code))
                            }
                        },
                        onRevokePairing = {
                            remoteAccess.revoke()
                            remotePairing = null
                        },
                        onRemoteDeviceControlChange = { allowed ->
                            remoteAccess.remoteDeviceControl = allowed
                            remoteDeviceControl = allowed
                        },
                        onRefreshRemoteUrls = { remoteUrls = remoteAccess.localUrls() },
                        onRefreshPlugins = ::refreshPlugins,
                        onInstallPlugin = ::installPlugin,
                        onUninstallPlugin = ::uninstallPlugin,
                        onTogglePlugin = ::togglePlugin,
                        onRefreshMemory = ::refreshMemory,
                        onDeleteMemoryEntry = { id ->
                            val message = runCatching {
                                operationMemory.execute(
                                    JSONObject().put("action", "forget").put("id", id),
                                    MemoryReadOnlyHost
                                ).message
                            }.getOrElse { "删除失败：${it.message ?: "操作记忆不可用"}" }
                            refreshMemory()
                            memoryStatusText = message
                        }
                    ),
                    shell = shellContext
                ),
                chrome = ShellChrome(
                    title = activeConversation.title.ifBlank { workspaceName },
                    subtitle = "Jarvis · " + if (config.rootMode == RootMode.Full) "完全模式" else "个人助理",
                    restrictionMode = config.restrictionMode,
                    onRestrictionChange = { update(config.copy(restrictionMode = !config.restrictionMode)) },
                    drawerOpen = showNavigation,
                    onDrawerOpenChange = { showNavigation = it },
                    railVisible = showLandscapeRail,
                    onRailVisibleChange = { showLandscapeRail = it }
                ),
                drawerContent = {
                    SessionRail(
                        tab = tab,
                        workspace = workspaceName,
                        workspaces = workspaces,
                        conversations = conversations,
                        activeConversationId = activeConversationId,
                        avatarRes = module.artwork.avatar,
                        story = module.story,
                        onTab = { tab = it; showNavigation = false },
                        onWorkspace = ::selectWorkspace,
                        onSelectConversation = ::selectConversation,
                        onNewConversation = ::createConversation,
                        // 抽屉内直接操作会话：改名/移动/归档/删除复用工作区屏已有的同一套回调。
                        onRenameConversation = { id, title -> renameConversation(id, title) },
                        onMoveConversation = { id, target -> moveConversation(id, target) },
                        onArchiveConversation = { id -> setConversationArchived(id, true) },
                        onUnarchiveConversation = { id -> setConversationArchived(id, false) },
                        onDeleteConversation = ::deleteConversation,
                        modifier = Modifier.fillMaxHeight()
                    )
                },
                dialogs = {
                    if (showRuntimeTools) {
                        RuntimeToolsDialog(
                            onDismiss = { showRuntimeTools = false },
                            onVoiceInput = {
                                showRuntimeTools = false
                                startSpeechInput()
                            }
                        )
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
            )
        }
    }
}

private suspend fun callJarvisModel(
    context: Context,
    config: JarvisConfig,
    workspaceProfile: WorkspaceProfile?,
    conversationId: String,
    history: List<ChatMessage>
): String = withContext(Dispatchers.IO) {
    DeviceTaskCoordinator.run("本机对话") {
    val prompt = history.lastOrNull { it.role == ChatMessageRole.USER }?.content.orEmpty()
    val operationMemory = com.jarvis.app.runtime.OperationMemory.at(context.filesDir)
    val memoryStatus = runCatching { operationMemory.begin(prompt); operationMemory.summary() }
        .getOrElse { "操作记忆暂不可用，请通过普通工具完成任务并如实报告记忆错误。" }
    val runtimeTools = runCatching { RuntimeToolManager(context).inspect() }.getOrDefault(emptyList())
    val gateway = OpenClawProcessManager.get(context)
    val started = gateway.start()
    if (!started.started) "OpenClaw 未就绪：${started.message}" else runCatching {
        val system = buildString {
            val persona = workspaceProfile?.resolvedMentorPrompt(config.mentorPrompt) ?: config.mentorPrompt
            val worldBook = workspaceProfile?.resolvedWorldBook(config.worldBook) ?: config.worldBook
            append(persona.ifBlank { "你是 Jarvis，运行在 Android 手机上的个人助理。需要访问设备时调用 Jarvis 设备工具，并如实报告结果。" })
            if (worldBook.isNotBlank()) append("\n\n世界书：\n$worldBook")
            append("\n\n" + com.jarvis.app.runtime.OperationMemory.instructions)
            append("\n" + memoryStatus)
            append("\n\n开发与媒体命令工具状态（可用 jarvis_runtime_tools action=list 重新检查，action=run 调用已安装工具）：")
            runtimeTools.forEach { status ->
                append("\n- ${status.label}: ")
                if (status.source == ToolSource.MISSING) append("未安装；可在 Jarvis 工具箱中查看获取方式。")
                else append("${status.source.label}，路径 ${status.path}")
            }
            if (config.rootMode == RootMode.Full) {
                append("\n完全 root 模式已开启；通过 jarvis_runtime_tools 调用已安装的构建、媒体、下载、数据库和压缩工具，并检查退出码与输出。Termux 工具位于当前 Android 用户的数据目录，执行时由工具桥配置 PREFIX、PATH、LD_LIBRARY_PATH、HOME、TMPDIR。构建 Android 原生项目还需要匹配的 NDK/ABI 工具链。")
            } else {
                append("\n运行命令需要用户在设置中开启完全 root 模式；未安装的工具不能假装可用。")
            }
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

@Composable
private fun SessionRail(
    tab: JarvisTab,
    workspace: String,
    workspaces: List<WorkspaceProfile>,
    conversations: List<ChatSession>,
    activeConversationId: String,
    /** 当前主题模块的头像图源与故事名：抽屉头部不再读旧枚举 LocalJarvisTheme。 */
    avatarRes: Int,
    story: String,
    onTab: (JarvisTab) -> Unit,
    onWorkspace: (String) -> Unit,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    /** 会话行的二级菜单动作，全部复用工作区屏的同名操作（只改库，不复制逻辑）。 */
    onRenameConversation: (String, String) -> Unit = { _, _ -> },
    onMoveConversation: (String, String) -> Unit = { _, _ -> },
    onArchiveConversation: (String) -> Unit = {},
    onUnarchiveConversation: (String) -> Unit = {},
    onDeleteConversation: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val compact = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var workspaceMenu by remember { mutableStateOf(false) }
    val expandedWorkspaces = remember { mutableStateMapOf<String, Boolean>() }
    // 抽屉内的会话操作状态：归档默认隐藏，需要时在「對話」标题行展开取回。
    var railShowArchived by remember { mutableStateOf(false) }
    var railNotice by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ChatSession?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf("") }
    var moveTarget by remember { mutableStateOf<ChatSession?>(null) }
    var deleteTarget by remember { mutableStateOf<ChatSession?>(null) }
    val sessionRows = if (railShowArchived) conversations else conversations.filterNot { it.isArchived }
    Column(modifier.background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(if (compact) 12.dp else 18.dp), horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = if (compact) 2.dp else 12.dp)) {
            JarvisBrandMark(avatarRes = avatarRes, size = if (compact) 30 else 42)
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text("Jarvis", style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (!compact) Text(story, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(if (compact) 4.dp else 20.dp))
        if (!compact) Text("工作区", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Box(Modifier.fillMaxWidth()) {
            TextButton(onClick = { workspaceMenu = true }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = if (compact) 0.dp else 12.dp), modifier = Modifier.fillMaxWidth().jarvisPressScale()) {
                Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(workspace, modifier = Modifier.weight(1f))
            }
            DropdownMenu(expanded = workspaceMenu, onDismissRequest = { workspaceMenu = false }) {
                workspaces.forEach { profile ->
                    key(profile.name) {
                        DropdownMenuItem(text = { Text(profile.name) }, onClick = { workspaceMenu = false; onWorkspace(profile.name) })
                    }
                }
            }
        }
        Spacer(Modifier.height(if (compact) 2.dp else 16.dp))
        if (!compact) Text("导航", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.height(if (compact) 2.dp else 8.dp))
        JarvisTab.entries.forEach { item ->
            key(item) {
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
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("對話", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f))
            val archivedCount = conversations.count { it.isArchived }
            if (archivedCount > 0) {
                TextButton(onClick = { railShowArchived = !railShowArchived },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(if (railShowArchived) "隐藏归档" else "归档 $archivedCount", fontSize = 11.sp)
                }
            }
            TextButton(onClick = onNewConversation, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), modifier = Modifier.jarvisPressScale()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("新建", fontSize = 12.sp)
            }
        }
        if (railNotice.isNotBlank()) {
            Text(railNotice, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
        }
        val groupNames = (workspaces.map { it.name } + sessionRows.map { it.workspace }).distinct()
        groupNames.forEach { groupName ->
            key(groupName) {
            val groupConversations = sessionRows.filter { it.workspace == groupName }.take(30)
            val expanded = expandedWorkspaces[groupName] ?: (groupName == workspace)
            Row(Modifier.fillMaxWidth().clickable { expandedWorkspaces[groupName] = !expanded }
                .padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(groupName, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("${groupConversations.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    // key 让每行按会话 id 复用，改名/归档重排时不会整列重组。
                    groupConversations.forEach { conversation ->
                        key(conversation.id) {
                            SessionRailRow(
                                conversation = conversation,
                                selected = conversation.id == activeConversationId && tab == JarvisTab.Chat,
                                otherWorkspaces = workspaces.filter { it.name != conversation.workspace },
                                onSelect = { onSelectConversation(conversation.id) },
                                onRename = { railNotice = ""; renameError = ""; renameDraft = conversation.title; renameTarget = conversation },
                                onMove = { railNotice = ""; moveTarget = conversation },
                                onToggleArchive = {
                                    if (conversation.isArchived) {
                                        onUnarchiveConversation(conversation.id)
                                        railNotice = "已还原「${conversation.title}」，会话回到列表"
                                    } else {
                                        onArchiveConversation(conversation.id)
                                        railNotice = "已归档「${conversation.title}」，数据保留"
                                    }
                                },
                                onDelete = { railNotice = ""; deleteTarget = conversation }
                            )
                        }
                    }
                }
            }
            }
        }
    }
    val renaming = renameTarget
    if (renaming != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名会话") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameDraft,
                        onValueChange = { renameDraft = it.take(60); renameError = "" },
                        singleLine = true,
                        isError = renameError.isNotBlank(),
                        label = { Text("会话名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (renameError.isNotBlank()) {
                        Text(renameError, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = renameDraft.trim()
                    if (clean.isEmpty()) renameError = "名称不能为空" else {
                        onRenameConversation(renaming.id, clean)
                        railNotice = "已重命名为「$clean」"
                        renameTarget = null
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } }
        )
    }
    val moving = moveTarget
    if (moving != null) {
        val targets = workspaces.filter { it.name != moving.workspace }
        AlertDialog(
            onDismissRequest = { moveTarget = null },
            title = { Text("移动到其他工作区") },
            text = {
                Column {
                    Text("当前工作区：${moving.workspace}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    if (targets.isEmpty()) {
                        Text("只有一个工作区，无法移动。先在工作区屏新建目标工作区。", fontSize = 13.sp)
                    } else {
                        targets.forEach { profile ->
                            TextButton(onClick = {
                                onMoveConversation(moving.id, profile.name)
                                railNotice = "已移动到「${profile.name}」"
                                moveTarget = null
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(profile.name, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { moveTarget = null }) { Text("关闭") } }
        )
    }
    val deleting = deleteTarget
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除会话") },
            text = {
                Text(
                    if (conversations.size <= 1) "「${deleting.title}」是最后一条会话；删除后会自动补一条空白会话，不会出现空列表。"
                    else "删除「${deleting.title}」后消息不可恢复，确认删除？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConversation(deleting.id)
                    railNotice = "已删除「${deleting.title}」"
                    deleteTarget = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

/**
 * 抽屉里的一行会话：主体点击进入，右侧 44dp 的「⋯」打开二级菜单（改名 / 移动 / 归档或还原 / 删除）。
 * 菜单状态留在行内，列表用 key 复用后不会因其他行变化而错位。
 */
@Composable
private fun SessionRailRow(
    conversation: ChatSession,
    selected: Boolean,
    otherWorkspaces: List<WorkspaceProfile>,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(start = 18.dp, bottom = 2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = onSelect).padding(horizontal = 12.dp, vertical = 9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(conversation.title.ifBlank { "未命名会话" }, maxLines = 1, fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    if (conversation.isArchived) {
                        Spacer(Modifier.width(6.dp))
                        Text("已归档", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val preview = conversation.messages.lastOrNull()?.content.orEmpty()
                if (preview.isNotBlank()) Text(preview, maxLines = 1, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(44.dp)) {
                    Text("⋯", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("改名") }, onClick = { menuOpen = false; onRename() })
                    DropdownMenuItem(
                        text = {
                            Text(if (otherWorkspaces.isEmpty()) "移动到其他工作区（仅一个工作区）" else "移动到其他工作区")
                        },
                        enabled = otherWorkspaces.isNotEmpty(),
                        onClick = { menuOpen = false; onMove() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (conversation.isArchived) "还原" else "归档") },
                        onClick = { menuOpen = false; onToggleArchive() }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RuntimeToolsDialog(onDismiss: () -> Unit, onVoiceInput: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember(context) { RuntimeToolManager(context) }
    val scope = rememberCoroutineScope()
    var statuses by remember { mutableStateOf<List<RuntimeToolStatus>>(emptyList()) }
    var checking by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf("") }

    fun refresh() {
        checking = true
        scope.launch {
            statuses = withContext(Dispatchers.IO) { manager.inspect() }
            checking = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Jarvis 工具箱") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("语音转文字", fontWeight = FontWeight.SemiBold)
                        Text("调用手机现有语音识别服务，把识别结果放入输入框供你检查；不会自动发送。离线识别是否可用取决于手机的语音服务和已下载语言包。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = onVoiceInput, modifier = Modifier.jarvisPressScale()) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("开始语音输入")
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("开发与媒体命令", fontWeight = FontWeight.SemiBold)
                    Text("CMake · FFmpeg · Python · Rust · Git · curl · SQLite · zip", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = ::refresh, enabled = !checking, modifier = Modifier.jarvisPressScale()) { Text("刷新") }
                }
                if (checking && statuses.isEmpty()) {
                    Text("正在检查设备与运行时…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    statuses.forEach { status ->
                        PluginToolRow(status) {
                            val result = manager.install(status)
                            notice = result.message
                            refresh()
                        }
                    }
                }
                if (notice.isNotBlank()) Text(notice, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("缺少命令时 Jarvis 可向 Termux 请求安装（需在 Termux 设置中允许外部应用运行命令）；命令在应用沙盒中是否可直接执行仍取决于 Android/SELinux 和 root 授权。", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.jarvisPressScale()) { Text("完成") } }
    )
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
private fun PluginToolRow(status: RuntimeToolStatus, onInstall: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Icon(Icons.Default.Extension, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(status.label, fontWeight = FontWeight.SemiBold)
            Text(status.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (status.source == ToolSource.MISSING) {
            TextButton(onClick = onInstall, modifier = Modifier.jarvisPressScale()) { Text("Termux安装") }
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
