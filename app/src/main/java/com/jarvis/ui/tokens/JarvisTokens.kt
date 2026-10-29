package com.jarvis.ui.tokens

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 4dp 网格间距。旧界面 134 处 .dp 字面量里 8dp 41 处、6dp 15 处、12dp 14 处、18dp 11 处，
 * 7/15/22dp 是离群值；这里把全部间距收敛到 4 的倍数，页面级留白另有语义别名。
 */
object JarvisSpacing {
    val space2 = 4.dp
    val space3 = 8.dp
    val space4 = 12.dp
    val space5 = 16.dp
    val space6 = 20.dp
    val space7 = 24.dp
    val space8 = 32.dp

    /** 页面左右安全边距。 */
    val pageHorizontal = 20.dp

    /** 页面上下内边距：用户反馈「层与层之间空隙太大」，20 -> 16dp，仍是 4dp 倍数。 */
    val pageVertical = 16.dp

    /** 两个区块之间的呼吸距离：28 -> 20dp（上一轮放松过头，层与层之间空隙太大）。 */
    val sectionGap = 20.dp

    /** 卡片内边距：20 -> 16dp，与 [space5] 同值但语义独立（只控制卡片内容四周）。 */
    val cardPadding = 16.dp

    /** 内嵌卡片（[JarvisInsetCard]）内边距：16 -> 12dp，比外层卡片再紧一档。 */
    val insetCardPadding = 12.dp

    /** 相邻卡片之间的间隔：16 -> 12dp，比 [sectionGap] 紧、比 [rowGap] 松。 */
    val cardGap = 12.dp

    /** 同一张卡片内相邻行之间的间隔，也是卡片头与内容的间距：12 -> 8dp。 */
    val rowGap = 8.dp
}

/** 圆角阶梯；[materialShapes] 把阶梯交给 Material3 组件使用。 */
object JarvisShapes {
    val xs = RoundedCornerShape(8.dp)
    val sm = RoundedCornerShape(12.dp)
    val md = RoundedCornerShape(16.dp)
    val lg = RoundedCornerShape(20.dp)
    val xl = RoundedCornerShape(24.dp)
    val pill = CircleShape

    fun materialShapes(): Shapes = Shapes(
        extraSmall = xs,
        small = sm,
        medium = md,
        large = xl,
        extraLarge = xl
    )
}

/** 动效时长与按压反馈参数。时长单位毫秒，可直接交给 tween。 */
object JarvisMotion {
    const val Fast = 150
    const val Base = 220
    const val Slow = 300

    /** 按下时的目标缩放。 */
    const val PressScale = 0.96f

    /** 全局唯一的按压弹簧，保证任何控件手感一致。 */
    val pressSpec: FiniteAnimationSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    /** 1f -> 0.96f 的按压缩放状态；与 [jarvisPressScale] 使用同一套弹簧。 */
    @Composable
    fun pressScale(pressed: Boolean): State<Float> = animateFloatAsState(
        targetValue = if (pressed) PressScale else 1f,
        animationSpec = pressSpec,
        label = "press-scale"
    )
}

/**
 * 高度阶梯。
 *
 * 分层改由底色明度差（surface / surfaceContainer 阶梯）承担，描边默认不画；阴影只用于浮层（弹窗、浮窗、下拉）。
 * [level1] 既不是圆角也不是阴影，而是「卡片描边宽度上限」：需要描边时不得超过它，且 JarvisCard 的默认值是 null（不画）。
 */
object JarvisElevation {
    val level0 = 0.dp

    /** 卡片描边宽度上限；默认不画描边，组件只在调用方显式传 BorderStroke 时用到它。 */
    val level1 = 1.dp
    val level2 = 2.dp
    val level3 = 4.dp
    val level4 = 12.dp
}

/**
 * 线宽两档。分隔线用 [hairline]；块与块之间需要「框」出来时用 [emphasis]。
 * 描边宽度整体压细：1dp 的线在深色主题上会抢色块，0.5dp 只勾一道发丝，边界还在但不显眼。
 */
object JarvisStroke {
    /** 极细小边框 / 分隔线：0.5dp。 */
    val hairline = 0.5.dp

    /** 强调块边框：1dp，只给需要从同色区域里框出来的那一块。 */
    val emphasis = 1.dp
}

/**
 * 两种区分手段——「边框改细之后，靠什么把两块分开」的唯一口径。
 *
 * ① 极细线：[JarvisStroke.hairline] 宽 + borderSubtle 色（[hairlineBorder]）。只勾一条发丝线，
 *    视觉增量最小，适合列表行之间、内嵌块边缘、输入框外框。
 * ② 色块：完全不画线，用底色差成块（surfaceContainerHigh vs surface；JarvisCard 用 surface、
 *    JarvisInsetCard 用 surfaceContainer）。对比更强，适合卡片与分组。
 *
 * 组件默认走 ②（JarvisCard 默认 border = null，边界由底色明度差承担）；只有底色相近分不开、
 * 或需要强调某一块时才用 ①。两者不叠加：同一块要么有线，要么有色差。
 */
object JarvisContrast {
    /** ① 极细小边框：宽度 [JarvisStroke.hairline]，颜色建议 borderSubtle。 */
    fun hairlineBorder(color: Color): BorderStroke = BorderStroke(JarvisStroke.hairline, color)

    /** ① 的强调版：宽度 [JarvisStroke.emphasis]，用于需要更明确边界的那一块。 */
    fun emphasisBorder(color: Color): BorderStroke = BorderStroke(JarvisStroke.emphasis, color)
}

/**
 * 栏面与操作件尺寸收敛。
 *
 * 用户反馈「上下边框太占地方、操作按钮偏大」：顶栏/底栏 64 -> 52dp，芯片 48 -> 40dp，
 * 工具栏图标 24 -> 20dp，列表行 60 -> 52 -> 48dp。[minTouch] 是硬下限，任何可点元素的可点区域
 * 不得小于 44dp（芯片的 48dp 触达仍由 Material3 的最小交互尺寸兜住）。
 */
object JarvisSize {
    /** 外壳顶栏内容高度（不含状态栏 inset）。 */
    val topBarHeight = 52.dp

    /** 主题自绘底栏高度，也是外壳在底栏上方预留的高度。 */
    val bottomBarHeight = 52.dp

    /** 发送按钮直径。 */
    val sendButton = 44.dp

    /** 顶栏 / 工具栏图标边长。 */
    val toolbarIcon = 20.dp

    /** 芯片（筛选 chip）视觉高度。 */
    val chipHeight = 40.dp

    /** 紧凑行高下限：48dp（列表行 60 -> 52 -> 48dp）。 */
    val denseRowHeight = 48.dp

    /** 列表行最小高度：跟随紧凑行高，52 -> 48dp。 */
    val rowMinHeight = denseRowHeight

    /** 图标动作按钮的视觉直径（20dp 图标 + 8dp 四周留白）；触达仍为 [minTouch]。 */
    val iconAction = 36.dp

    /** 纯图标按钮视觉边长，比 [iconAction] 再小一档，用于卡片内联的次要动作。 */
    val iconOnlyButton = 32.dp

    /** 可点元素触达下限。 */
    val minTouch = 44.dp
}

/**
 * 首尾栏半透明过渡。
 *
 * 栏面取 background 的 [barAlpha] 不透明度而不是全不透明，再朝内容方向用一个 [fadeLength] 长的渐变淡出，
 * 消除栏与滚动内容之间的硬切边。这里只产出 Brush，贴边位置与栏布局仍由外壳（ui/shell）自己决定。
 * 渐变末端用 background.copy(alpha = 0f) 而不是 Color.Transparent：后者是透明黑，浅色主题上会发灰。
 */
object JarvisScrim {
    /** 栏面不透明度：足以盖住滚到栏下的内容，又留 8% 透光让栏与页面同源。 */
    const val barAlpha = 0.92f

    /** 渐变过渡长度，从栏边朝内容方向算起。 */
    val fadeLength = 24.dp

    /** 顶栏贴边渐变：上端 background@[barAlpha]，下端透明。 */
    fun topBarBrush(background: Color): Brush = Brush.verticalGradient(
        colors = listOf(
            background.copy(alpha = barAlpha),
            background.copy(alpha = 0f)
        )
    )

    /** 底栏贴边渐变：上端透明，下端 background@[barAlpha]。 */
    fun bottomBarBrush(background: Color): Brush = Brush.verticalGradient(
        colors = listOf(
            background.copy(alpha = 0f),
            background.copy(alpha = barAlpha)
        )
    )
}

/** 8 级排版阶梯。字号/行高/字重全部固定，行高必须显式给出（旧界面 lineHeight 全为 0）。 */
object JarvisType {
    /** 页级大标题。 */
    val displaySection = TextStyle(
        fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold
    )

    /** 屏标题。 */
    val titleScreen = TextStyle(
        fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold
    )

    /** 区块标题。 */
    val titleBlock = TextStyle(
        fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold
    )

    /** 列表项标题。 */
    val titleItem = TextStyle(
        fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold
    )

    /** 唯一正文。 */
    val bodyBase = TextStyle(
        fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal
    )

    /** 元信息 / 辅助说明。 */
    val bodyMeta = TextStyle(
        fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal
    )

    /** 徽标、计数、极小标签。 */
    val labelBadge = TextStyle(
        fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium
    )

    /** URL / 命令 / 配对码等等宽文本。 */
    val codeInline = TextStyle(
        fontSize = 12.sp, lineHeight = 18.sp, fontFamily = FontFamily.Monospace
    )
}

/**
 * Material3 入口。8 级阶梯落到 15 个 M3 槽位，任何槽位都不再回落到 M3 自带基线排版：
 * displayLarge/displayMedium/displaySmall/headlineLarge/headlineMedium -> displaySection
 * headlineSmall/titleLarge -> titleScreen
 * titleMedium -> titleBlock
 * titleSmall/labelLarge -> titleItem
 * bodyLarge/bodyMedium -> bodyBase
 * bodySmall -> bodyMeta
 * labelMedium -> codeInline
 * labelSmall -> labelBadge
 */
val JarvisTypography: Typography = Typography(
    displayLarge = JarvisType.displaySection,
    displayMedium = JarvisType.displaySection,
    displaySmall = JarvisType.displaySection,
    headlineLarge = JarvisType.displaySection,
    headlineMedium = JarvisType.displaySection,
    headlineSmall = JarvisType.titleScreen,
    titleLarge = JarvisType.titleScreen,
    titleMedium = JarvisType.titleBlock,
    titleSmall = JarvisType.titleItem,
    bodyLarge = JarvisType.bodyBase,
    bodyMedium = JarvisType.bodyBase,
    bodySmall = JarvisType.bodyMeta,
    labelLarge = JarvisType.titleItem,
    labelMedium = JarvisType.codeInline,
    labelSmall = JarvisType.labelBadge
)

/**
 * 与旧 `Modifier.jarvisPressScale()`（MainActivity.kt:177-198）等价的可插拔实现：
 * 同样的 1f -> 0.96f、同样的 MediumBouncy/Medium 弹簧、同样的按压手势，可直接替换。
 */
fun Modifier.jarvisPressScale(): Modifier = composed {
    val pressed = remember { mutableStateOf(false) }
    val scale by JarvisMotion.pressScale(pressed.value)
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
