package com.jarvis.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.jarvis.ui.tokens.JarvisShapes
import com.jarvis.ui.tokens.JarvisSpacing
import com.jarvis.ui.tokens.JarvisStroke
import com.jarvis.ui.tokens.jarvisPressScale

/** 屏级统一内边距：左右 [JarvisSpacing.pageHorizontal]、上下 [JarvisSpacing.pageVertical]（20 -> 16dp）；只调用一次，放在每个屏的根容器上。 */
@Composable
fun Modifier.jarvisScreenPadding(): Modifier =
    padding(horizontal = JarvisSpacing.pageHorizontal, vertical = JarvisSpacing.pageVertical)

/**
 * 卡片：surface 底 + 圆角（默认 lg，便当类主题可传 sm）；onClick 非空时整卡可点并带按压反馈。
 *
 * 默认**不画描边**：卡片边界由 surface 与 background 的明度差承担，描边只是可选的柔化辅助。
 * 需要描边时显式传 [border]（宽度不超过 JarvisElevation.level1，颜色取 borderSubtle），默认 null 时不留任何外框，
 * 因此完全不改既有调用点的观感契约——它们得到的就是「无描边卡片」。
 */
@Composable
fun JarvisCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = JarvisSpacing.cardPadding,
    onClick: (() -> Unit)? = null,
    shape: Shape = JarvisShapes.lg,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactive = if (onClick == null) {
        Modifier
    } else {
        Modifier
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .jarvisPressScale()
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (border == null) Modifier else Modifier.border(border, shape))
            .then(interactive)
            .padding(contentPadding),
        content = content
    )
}

/**
 * 内嵌卡片：实心 surfaceContainer 底 + 圆角（默认 md，可传 sm/xs），不用任何透明度叠加，深色主题下也能与卡片分出层次。
 * 与 [JarvisCard] 一样默认不画描边，需要时显式传 [border]。
 */
@Composable
fun JarvisInsetCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = JarvisSpacing.insetCardPadding,
    shape: Shape = JarvisShapes.md,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (border == null) Modifier else Modifier.border(border, shape))
            .padding(contentPadding),
        content = content
    )
}

/**
 * 卡片头与卡片内容之间的间距：统一走 [JarvisSpacing.rowGap]（12 -> 8dp），
 * 调用方不要再自己写 12/16dp 字面量，否则「层与层之间空隙太大」会从调用点回潮。
 */
@Composable
fun JarvisCardHeaderGap(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.height(JarvisSpacing.rowGap))
}

/**
 * 分隔线语义色注入点：阶段 3 在主题层给出 ThemePalette.divider，未注入时 JarvisDivider 回落 outlineVariant。
 * 只影响 JarvisDivider，与卡片外框无关（卡片外框由 JarvisCard(border = ...) 显式给出，默认没有）。
 */
val LocalJarvisDividerColor = staticCompositionLocalOf<Color?> { null }

/**
 * 分隔线：宽度取 [JarvisStroke.hairline]（不属于卡片描边），优先取 LocalJarvisDividerColor，
 * 未注入时用 outlineVariant；需要缩进时由调用方加 horizontal padding。
 */
@Composable
fun JarvisDivider(modifier: Modifier = Modifier, thickness: Dp = JarvisStroke.hairline) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = LocalJarvisDividerColor.current ?: MaterialTheme.colorScheme.outlineVariant
    )
}
