package com.jarvis.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.ui.tokens.JarvisShapes
import kotlinx.coroutines.flow.first
import com.jarvis.ui.tokens.JarvisSize
import com.jarvis.ui.tokens.JarvisSpacing

/**
 * 行高下限取 [JarvisSize.rowMinHeight]（= [JarvisSize.denseRowHeight]，48dp）：用户反馈层与层之间空隙太大，
 * 60 -> 52 -> 48dp。用 heightIn 表达，系统大字号放大文本时行会自然长高而不是被裁掉；
 * 可点行的触达高度仍是 48dp，≥ [JarvisSize.minTouch]（44dp）。
 */
private val ListRowMinHeight = JarvisSize.rowMinHeight

/**
 * 列表行：leading / title / subtitle / trailing 四段，title=titleSmall、subtitle=bodySmall。
 * 行本身不带左右内边距，放在 JarvisCard 里由卡片 contentPadding 决定；onClick 非空时整行可点。
 */
@Composable
fun ListRow(
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interactive = if (onClick == null) {
        Modifier
    } else {
        Modifier
            .clip(JarvisShapes.sm)
            .clickable(role = Role.Button, onClick = onClick)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(interactive)
            .heightIn(min = ListRowMinHeight)
            .padding(vertical = JarvisSpacing.space3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(JarvisSpacing.space3))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(JarvisSpacing.space3))
            trailing()
        }
    }
}

/**
 * 开关行：整行可点（Role.Switch + toggleable 语义），Switch 自身不再接管点击，避免行与开关各响应一次。
 * 关闭态由 enabled=false 表达，视觉由主题的 disabled 透明规则决定。
 */
@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(JarvisShapes.sm)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .heightIn(min = ListRowMinHeight)
            .padding(vertical = JarvisSpacing.space3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(JarvisSpacing.space3))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

/**
 * 单选 chip 行：Row + horizontalScroll 实现横向滚动（普通 Row，可被 IntrinsicSize 求高），
 * 单选用 Role.RadioButton + selected 语义；chip 视觉高度取 [JarvisSize.chipHeight]（40dp），
 * 触达仍由 Material3 的最小交互尺寸保证 ≥ [JarvisSize.minTouch]。
 * selectedIndex 变化时按实测 chip 偏移把该项滚进可视区（已完整可见则不动），末项选中也不会留在首屏外。
 */
@Composable
fun JarvisChipRow(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val chipPositions = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }
    val edgeMargin = with(LocalDensity.current) { JarvisSpacing.space3.roundToPx() }
    LaunchedEffect(selectedIndex, items.size) {
        if (items.isEmpty()) return@LaunchedEffect
        val target = selectedIndex.coerceIn(0, items.lastIndex)
        // 等布局报出目标 chip 的实测位置；首帧尚未布局时挂起而不是猜宽度。
        val (chipStart, chipWidth) = snapshotFlow { chipPositions[target] }.first { it != null }!!
        val visibleStart = scrollState.value
        val visibleEnd = visibleStart + scrollState.viewportSize
        if (chipStart < visibleStart || chipStart + chipWidth > visibleEnd) {
            scrollState.animateScrollTo(
                (chipStart - edgeMargin).coerceIn(0, scrollState.maxValue)
            )
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(index) },
                border = BorderStroke(0.dp, Color.Transparent),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                },
                modifier = Modifier
                    .heightIn(min = JarvisSize.chipHeight)
                    .onGloballyPositioned { coordinates ->
                        val next = coordinates.positionInParent().x.toInt() to coordinates.size.width
                        if (chipPositions[index] != next) chipPositions[index] = next
                    }
                    .semantics {
                        selected = isSelected
                        role = Role.RadioButton
                    }
            )
            if (index != items.lastIndex) {
                Spacer(Modifier.width(JarvisSpacing.space3))
            }
        }
    }
}

/**
 * 空态面板：可选插画、图标、标题、副标题、操作各出现一次，整体垂直居中。
 * 传 modifier = Modifier.fillMaxWidth().weight(1f)（或 fillMaxSize）才会真的居中，否则 Column 会缩到内容高度。
 * illustration 传 R.drawable 引用；插画与图标都是装饰性的，语义由 title 承担，故 contentDescription 给 null 以免重复播报。
 * 内边距默认 [JarvisSpacing.space5]（16dp，原 20dp）并开成参数：空态四周留白不随卡片内边距缩放，孤立内容才不显得贴边。
 */
@Composable
fun EmptyStatePanel(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    illustration: Int? = null,
    icon: ImageVector? = null,
    contentPadding: Dp = JarvisSpacing.space5,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (illustration != null) {
            Image(
                painter = painterResource(id = illustration),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .clip(JarvisShapes.xl)
            )
            Spacer(Modifier.height(JarvisSpacing.space5))
        }
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(JarvisSpacing.space4))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Spacer(Modifier.height(JarvisSpacing.space2))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(Modifier.height(JarvisSpacing.space5))
            action()
        }
    }
}
