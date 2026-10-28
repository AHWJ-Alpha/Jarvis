package com.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jarvis.ui.tokens.JarvisShapes
import com.jarvis.ui.tokens.JarvisSize
import com.jarvis.ui.tokens.JarvisSpacing

/** 提示语气：INFO 说明、SUCCESS 成功、ERROR 失败。 */
enum class NoticeTone { INFO, SUCCESS, ERROR }

/** 徽标语气：NEUTRAL 中性、ACTIVE 生效中、WARNING 需注意、ERROR 异常。 */
enum class BadgeTone { NEUTRAL, ACTIVE, WARNING, ERROR }

/**
 * 单行提示：bodySmall 一字号，ERROR 用 error、SUCCESS 用 primary、INFO 用 onSurfaceVariant；
 * 文本允许换行，不截断，长度由调用方保证语义完整。
 */
@Composable
fun NoticeLine(
    text: String,
    modifier: Modifier = Modifier,
    tone: NoticeTone = NoticeTone.INFO
) {
    val color = when (tone) {
        NoticeTone.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        NoticeTone.SUCCESS -> MaterialTheme.colorScheme.primary
        NoticeTone.ERROR -> MaterialTheme.colorScheme.error
    }
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = color
    )
}

/**
 * 状态徽标：labelSmall 一字号 + pill 圆角（徽标允许单行截断）。
 * 底色只取 colorScheme 槽位：NEUTRAL=surfaceContainerHigh、ACTIVE=primaryContainer（即主题的 accentSoft）、
 * WARNING=errorContainer、ERROR=error 实底配 onError，四种语气不依赖主题额外配置就能分开。
 */
@Composable
fun StatusBadge(
    label: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier
) {
    val container: Color
    val content: Color
    when (tone) {
        BadgeTone.NEUTRAL -> {
            container = MaterialTheme.colorScheme.surfaceContainerHigh
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
        BadgeTone.ACTIVE -> {
            container = MaterialTheme.colorScheme.primaryContainer
            content = MaterialTheme.colorScheme.primary
        }
        BadgeTone.WARNING -> {
            container = MaterialTheme.colorScheme.errorContainer
            content = MaterialTheme.colorScheme.onErrorContainer
        }
        BadgeTone.ERROR -> {
            container = MaterialTheme.colorScheme.error
            content = MaterialTheme.colorScheme.onError
        }
    }
    Text(
        text = label,
        modifier = modifier
            .clip(JarvisShapes.pill)
            .background(container)
            .padding(horizontal = JarvisSpacing.space3, vertical = JarvisSpacing.space2),
        style = MaterialTheme.typography.labelSmall,
        color = content,
        maxLines = 1,
        textAlign = TextAlign.Center
    )
}

/** 危险操作按钮：TextButton 文字色固定为 colorScheme.error，触达高度下限取 [JarvisSize.minTouch]（44dp）。 */
@Composable
fun DangerActionLabel(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = JarvisSize.minTouch),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1
        )
    }
}

/** 加载行：18dp 细环 + bodySmall 文案；非交互元素，不需要 48dp 触达。 */
@Composable
fun JarvisLoading(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp
        )
        Spacer(Modifier.width(JarvisSpacing.space3))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
