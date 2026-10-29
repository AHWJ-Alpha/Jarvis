package com.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.jarvis.ui.tokens.JarvisShapes
import com.jarvis.ui.tokens.JarvisSize
import com.jarvis.ui.tokens.JarvisSpacing
import com.jarvis.ui.tokens.JarvisType

/** 掩码中段的固定填充，同时当作「这不是真 Key」的识别标记。 */
private const val MASK_FILL = "****…****"

/** 短于该长度的 Key 不给前后缀，整串星光号，避免拼出原文。 */
private const val MASK_MIN_LENGTH = 12

/**
 * API Key 的展示掩码：≥12 位保留前 4 + 后 4，其余全星号（空串仍为空串）。
 * 调用方只能拿它做展示 / 导出，绝不能回写进配置——真值永远从 state/prefs 走。
 */
fun maskApiKey(value: String): String {
    val key = value.trim()
    return when {
        key.isEmpty() -> ""
        key.length < MASK_MIN_LENGTH -> "*".repeat(key.length)
        else -> key.take(4) + MASK_FILL + key.takeLast(4)
    }
}

/** 判断传入串是否已经是掩码产物：导入 JSON 时据此忽略掩码，不让它覆盖本机真 Key。 */
fun isMaskedApiKey(value: String): Boolean = value.contains('*') || value.contains('…')

/**
 * 敏感字段（模型 / 识图 API Key）：默认掩码只读，眼睛按钮一次性揭示，点「编辑 Key」才进可编辑态。
 * 外观由调用方主题传色，布局与状态流五套主题共用，避免各家各写一份泄露面。
 */
@Composable
fun JarvisSecretField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    labelStyle: TextStyle = JarvisType.labelBadge,
    valueStyle: TextStyle = JarvisType.codeInline,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    showDivider: Boolean = false
) {
    var revealed by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    val blank = value.isBlank()
    Column(modifier.fillMaxWidth().padding(vertical = JarvisSpacing.space2)) {
        Text(label, style = labelStyle, color = labelColor)
        Spacer(Modifier.height(JarvisSpacing.space2))
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = JarvisSize.minTouch)
                .clip(JarvisShapes.sm)
                .background(containerColor)
                .padding(start = JarvisSpacing.space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (editing) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = valueStyle.copy(color = valueColor),
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { inner ->
                        Box {
                            if (blank && hint.isNotEmpty()) Text(hint, style = valueStyle, color = labelColor)
                            inner()
                        }
                    }
                )
                SecretTextAction("完成", accentColor) { editing = false }
            } else {
                Text(
                    text = if (revealed) value else maskApiKey(value),
                    modifier = Modifier.weight(1f),
                    style = valueStyle,
                    color = if (blank && hint.isNotEmpty()) labelColor else valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!blank) {
                    JarvisIconAction(
                        icon = if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (revealed) "隐藏 API Key" else "显示 API Key",
                        onClick = { revealed = !revealed },
                        tone = JarvisActionTone.ACCENT
                    )
                }
                SecretTextAction(if (blank) "设置 Key" else "编辑 Key", accentColor) {
                    revealed = false
                    editing = true
                }
            }
        }
        if (showDivider) {
            Spacer(Modifier.height(JarvisSpacing.space2))
            JarvisDivider()
        }
    }
}

/** 行内极小文字动作：无底色、无描边，触达高度靠外层行高兜住。 */
@Composable
private fun SecretTextAction(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = JarvisSize.minTouch).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = JarvisSpacing.space3),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = JarvisType.labelBadge, color = color)
    }
}
