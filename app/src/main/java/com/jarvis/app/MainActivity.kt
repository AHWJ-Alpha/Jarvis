package com.jarvis.app

import android.content.Context
import android.os.Bundle
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
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class JarvisTab(val label: String) {
    Chat("对话"), Tasks("任务"), Workspace("工作区"), Settings("设置")
}

private enum class RootMode(val label: String) {
    Off("关闭"), Standard("标准"), Full("完全")
}

private data class JarvisConfig(
    val restrictionMode: Boolean = true,
    val rootMode: RootMode = RootMode.Standard,
    val commandChannelEnabled: Boolean = false,
    val commandPrefix: String = "/claw",
    val modelBaseUrl: String = "",
    val modelName: String = ""
)

private class ConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE)

    fun load(): JarvisConfig = JarvisConfig(
        restrictionMode = prefs.getBoolean("restrictionMode", true),
        rootMode = RootMode.entries.getOrElse(prefs.getInt("rootMode", 1)) { RootMode.Standard },
        commandChannelEnabled = prefs.getBoolean("commandChannelEnabled", false),
        commandPrefix = prefs.getString("commandPrefix", "/claw") ?: "/claw",
        modelBaseUrl = prefs.getString("modelBaseUrl", "") ?: "",
        modelName = prefs.getString("modelName", "") ?: ""
    )

    fun save(config: JarvisConfig) {
        prefs.edit()
            .putBoolean("restrictionMode", config.restrictionMode)
            .putInt("rootMode", config.rootMode.ordinal)
            .putBoolean("commandChannelEnabled", config.commandChannelEnabled)
            .putString("commandPrefix", config.commandPrefix)
            .putString("modelBaseUrl", config.modelBaseUrl)
            .putString("modelName", config.modelName)
            .apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JarvisApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JarvisApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { ConfigStore(context) }
    var config by remember { mutableStateOf(store.load()) }
    var tab by rememberSaveable { mutableStateOf(JarvisTab.Chat) }
    var draft by rememberSaveable { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf("Jarvis 已启动。你可以从这里开始描述任务。")) }
    var showRootWarning by remember { mutableStateOf(false) }
    var pendingRootMode by remember { mutableStateOf<RootMode?>(null) }

    fun update(next: JarvisConfig) {
        config = next
        store.save(next)
    }

    MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF176B87),
        secondary = Color(0xFFB56D3E),
        background = Color(0xFFF4F7F6),
        surface = Color.White,
        onSurface = Color(0xFF10212B)
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("Jarvis", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (config.rootMode == RootMode.Full) "完全模式" else "本地代理工作台",
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
                bottomBar = {
                    NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                        JarvisTab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { tab = item },
                                icon = {
                                    Icon(
                                        imageVector = when (item) {
                                            JarvisTab.Chat -> Icons.Default.Chat
                                            JarvisTab.Tasks -> Icons.Default.CheckCircle
                                            JarvisTab.Workspace -> Icons.Default.Home
                                            JarvisTab.Settings -> Icons.Default.Settings
                                        },
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            ) { padding ->
                when (tab) {
                    JarvisTab.Chat -> ChatScreen(
                        padding = padding,
                        messages = messages,
                        draft = draft,
                        onDraftChange = { draft = it },
                        onSend = {
                            val clean = draft.trim()
                            if (clean.isNotEmpty()) {
                                messages = messages + "你：$clean" + "Jarvis：已收到。工具执行和模型连接将在下一步接入。"
                                draft = ""
                            }
                        }
                    )
                    JarvisTab.Tasks -> TasksScreen(padding)
                    JarvisTab.Workspace -> WorkspaceScreen(padding)
                    JarvisTab.Settings -> SettingsScreen(
                        padding = padding,
                        config = config,
                        onRestrictionChange = { update(config.copy(restrictionMode = it)) },
                        onCommandChange = { update(config.copy(commandChannelEnabled = it)) },
                        onPrefixChange = { update(config.copy(commandPrefix = it)) },
                        onModelBaseUrlChange = { update(config.copy(modelBaseUrl = it)) },
                        onModelNameChange = { update(config.copy(modelName = it)) },
                        onRootModeRequest = {
                            pendingRootMode = it
                            if (it == RootMode.Full) showRootWarning = true else update(config.copy(rootMode = it))
                        }
                    )
                }
            }
        }
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
}

@Composable
private fun ChatScreen(
    padding: PaddingValues,
    messages: List<String>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        Text("主对话", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("自然语言入口。Jarvis 会在需要时生成计划并请求工具权限。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { message ->
                val isUser = message.startsWith("你：")
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isUser) Color(0xFFE1F0F1) else Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(message, modifier = Modifier.padding(14.dp)) }
            }
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(vertical = 12.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("描述你想让 Jarvis 完成的任务") },
                maxLines = 4
            )
            IconButton(onClick = onSend, modifier = Modifier.padding(start = 6.dp).size(52.dp)) {
                Icon(Icons.Default.Send, contentDescription = "发送", tint = MaterialTheme.colorScheme.primary)
            }
        }
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
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("工作区", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("每个工作区可以覆盖全局人格、世界书、示例和模型配置。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("默认工作区", fontWeight = FontWeight.SemiBold)
                Text("继承全局配置", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Extension, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("管理人格与世界书")
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    padding: PaddingValues,
    config: JarvisConfig,
    onRestrictionChange: (Boolean) -> Unit,
    onCommandChange: (Boolean) -> Unit,
    onPrefixChange: (String) -> Unit,
    onModelBaseUrlChange: (String) -> Unit,
    onModelNameChange: (String) -> Unit,
    onRootModeRequest: (RootMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        Text("设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("先配置基础策略，再逐步接入模型、QQ 和设备工具。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
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
                    Text("API Key 将在接入模型网关时保存到 Android Keystore。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
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
