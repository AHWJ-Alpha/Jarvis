package com.jarvis.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Keep the order stable: existing installs persist the ordinal in SharedPreferences.
internal enum class JarvisTheme(
    val label: String, val story: String, val description: String,
    val primary: Color, val background: Color, val surface: Color,
    val container: Color, val text: Color, val muted: Color,
    val outline: Color, val artwork: Int, val avatar: Int, val dark: Boolean = false
) {
    Deep("深海", "潮汐来信", "海蓝 · 流动插画",
        Color(0xFF216A88), Color(0xFFF2F7FA), Color(0xFFFBFDFF),
        Color(0xFFDCEEF5), Color(0xFF183746), Color(0xFF587482),
        Color(0xFFD5E3EB), R.drawable.theme_deep, R.drawable.theme_avatar_deep),
    Graphite("石墨", "秩序之城", "雾灰 · 纸雕建筑",
        Color(0xFF505C70), Color(0xFFF3F4F6), Color(0xFFFCFCFD),
        Color(0xFFE4E7ED), Color(0xFF292F3A), Color(0xFF687181),
        Color(0xFFDDE0E7), R.drawable.theme_graphite, R.drawable.theme_avatar_graphite),
    Forest("松林", "林间手记", "鼠尾草绿 · 自然水彩",
        Color(0xFF3E7053), Color(0xFFF3F7F1), Color(0xFFFCFEFA),
        Color(0xFFE0EEDC), Color(0xFF253D2D), Color(0xFF627B64),
        Color(0xFFDCE6D6), R.drawable.theme_forest, R.drawable.theme_avatar_forest),
    Ink("墨黑", "山月无声", "银墨 · 山水夜色",
        Color(0xFFBBC7E0), Color(0xFF111319), Color(0xFF1A1D26),
        Color(0xFF2B3244), Color(0xFFE4E8F1), Color(0xFF9EA9BF),
        Color(0xFF333B4D), R.drawable.theme_ink, R.drawable.theme_avatar_ink, dark = true),
    Anime("樱影", "暮色来信", "樱粉 · 动画电影感",
        Color(0xFFAD4E70), Color(0xFFFFF8F8), Color(0xFFFFFDFD),
        Color(0xFFF9E4EB), Color(0xFF303643), Color(0xFF706A78),
        Color(0xFFF0DCE4), R.drawable.theme_anime, R.drawable.theme_avatar_anime);

    fun colorScheme(): ColorScheme {
        val base = if (dark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = primary, onPrimary = if (dark) background else Color.White,
            primaryContainer = container, onPrimaryContainer = text,
            inversePrimary = if (dark) Color(0xFF505C70) else container,
            secondary = primary, onSecondary = if (dark) background else Color.White,
            secondaryContainer = container, onSecondaryContainer = text,
            tertiary = primary, onTertiary = if (dark) background else Color.White,
            tertiaryContainer = container, onTertiaryContainer = text,
            background = background, onBackground = text,
            surface = surface, onSurface = text,
            surfaceVariant = container, onSurfaceVariant = muted,
            surfaceTint = primary, outline = muted, outlineVariant = outline,
            inverseSurface = text, inverseOnSurface = background,
            surfaceBright = surface, surfaceDim = background,
            surfaceContainer = surface, surfaceContainerLow = surface,
            surfaceContainerLowest = background, surfaceContainerHigh = container,
            surfaceContainerHighest = container
        )
    }
}

internal val LocalJarvisTheme = staticCompositionLocalOf { JarvisTheme.Deep }

/**
 * 品牌头像。
 *
 * @param avatarRes 头像图源。外壳必须按当前主题模块（JarvisThemeModule.artwork.avatar）传入；
 *   缺省回退旧枚举 [LocalJarvisTheme]，只服务尚未改写的旧调用点，不再作为主题视觉来源。
 */
@Composable
internal fun JarvisBrandMark(
    modifier: Modifier = Modifier,
    size: Int = 40,
    avatarRes: Int = LocalJarvisTheme.current.avatar
) {
    Image(painterResource(avatarRes), contentDescription = null,
        modifier = modifier.size(size.dp).clip(RoundedCornerShape((size * .32f).dp)),
        contentScale = ContentScale.Crop)
}

@Composable
internal fun ThemeIllustration(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val theme = LocalJarvisTheme.current
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = colors.surface, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Box(Modifier.height(if (compact) 116.dp else 184.dp)) {
            Image(painterResource(theme.artwork), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(
                colors.surface.copy(alpha = .98f), colors.surface.copy(alpha = .90f),
                colors.surface.copy(alpha = .15f), colors.surface.copy(alpha = .04f)
            ))))
            Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.63f).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp), horizontalAlignment = Alignment.Start) {
                Text(theme.label + " / " + theme.story, color = colors.primary,
                    style = MaterialTheme.typography.labelMedium)
                Text(if (compact) title.replace("\n", "") else title, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                if (!compact) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun ThemePicker(selected: JarvisTheme, onSelect: (JarvisTheme) -> Unit) {
    // Vertical cards stay readable at large font scales and keep every label left aligned.
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        JarvisTheme.entries.forEach { theme ->
            val active = selected == theme
            Surface(shape = RoundedCornerShape(18.dp),
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
                border = BorderStroke(if (active) 2.dp else 1.dp,
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().clickable(
                    role = androidx.compose.ui.semantics.Role.RadioButton,
                    onClickLabel = "切换到${theme.label}主题", onClick = { onSelect(theme) }
                ).then(Modifier.semanticsSelected(active))) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(theme.artwork), contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(width = 84.dp, height = 68.dp).clip(RoundedCornerShape(11.dp)))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(theme.label + " · " + theme.story, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(theme.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf(theme.primary, theme.container, theme.background).forEach { color ->
                                Surface(color = color, shape = CircleShape,
                                    border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.size(10.dp)) {}
                            }
                        }
                    }
                    if (active) Icon(Icons.Default.CheckCircle, contentDescription = "当前主题",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp).size(20.dp))
                }
            }
        }
    }
}

private fun Modifier.semanticsSelected(value: Boolean): Modifier =
    this.then(Modifier.semantics { selected = value })
