package com.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.jarvis.ui.tokens.JarvisShapes
import com.jarvis.ui.tokens.JarvisSize
import com.jarvis.ui.tokens.JarvisSpacing

/**
 * 动作语气：NEUTRAL 中性（默认）、ACCENT 主操作、DANGER 危险/删除。
 * 只影响图标着色，不引入任何底色或透明度叠加——图标动作件一律无底、无描边。
 */
enum class JarvisActionTone { NEUTRAL, ACCENT, DANGER }

/** 语气 -> 图标色；禁用态统一压到 onSurface 的 38%（Material3 禁用标准），不额外造色值。 */
@Composable
private fun actionTint(tone: JarvisActionTone, enabled: Boolean): Color {
    val scheme = MaterialTheme.colorScheme
    if (!enabled) return scheme.onSurface.copy(alpha = 0.38f)
    return when (tone) {
        JarvisActionTone.NEUTRAL -> scheme.onSurfaceVariant
        JarvisActionTone.ACCENT -> scheme.primary
        JarvisActionTone.DANGER -> scheme.error
    }
}

/**
 * 纯图标动作：用户反馈「中英文标签挤占太多空间」，绝大多数按钮说明改成图标。
 *
 * 视觉 [JarvisSize.iconAction]（36dp）+ 图标 [JarvisSize.toolbarIcon]（20dp）+ 零文字；
 * 触达由外层 [JarvisSize.minTouch]（44dp）方框承担，视觉框居中其中，所以点得到但不显得胖。
 * [contentDescription] 必填且非空——没有文字就没有可见语义，无障碍完全靠它。
 */
@Composable
fun JarvisIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: JarvisActionTone = JarvisActionTone.NEUTRAL
) {
    require(contentDescription.isNotBlank()) { "图标动作必须带 contentDescription，否则无障碍丢失语义" }
    Box(
        modifier = modifier
            .size(JarvisSize.minTouch)
            .clip(JarvisShapes.pill)
            .clickable(role = Role.Button, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(JarvisSize.iconAction)
                .clip(JarvisShapes.pill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = actionTint(tone, enabled),
                modifier = Modifier.size(JarvisSize.toolbarIcon)
            )
        }
    }
}

/**
 * 图标动作横排：间距固定 [JarvisSpacing.space3]（8dp），行高取 [JarvisSize.denseRowHeight]。
 * actions 是 RowScope 尾随 lambda，写成 `JarvisIconRow { JarvisIconAction(...) }`；
 * 触达方框外扩到 44dp 后相邻图标不会互相压住，8dp 间距是「视觉」间距。
 */
@Composable
fun JarvisIconRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(JarvisSpacing.space3),
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = actions
    )
}

/**
 * 图标 + 极短文字：只在必须保留文字的场合用（单位、状态、数值），其余一律 [JarvisIconAction]。
 * 图标是装饰性的（contentDescription = null），语义由 [text] 承担，避免读屏念两遍。
 */
@Composable
fun JarvisGlyphLabel(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    tone: JarvisActionTone = JarvisActionTone.NEUTRAL
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = actionTint(tone, enabled = true),
            modifier = Modifier.size(JarvisSize.toolbarIcon)
        )
        Spacer(Modifier.width(JarvisSpacing.space2))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = actionTint(tone, enabled = true),
            maxLines = 1
        )
    }
}
