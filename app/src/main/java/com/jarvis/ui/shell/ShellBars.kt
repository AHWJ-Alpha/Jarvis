package com.jarvis.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jarvis.ui.tokens.JarvisScrim

/**
 * 首尾栏与内容之间的过渡。
 *
 * 口径：遮罩、栏面、渐变过渡允许半透明，卡片底色仍须实心。栏面用 [JarvisScrim.barAlpha] 半透明，
 * 栏沿再压一条 [JarvisScrim.fadeLength] 的渐变，内容滚到栏下时是淡出而不是被硬切。
 * 渐变与栏面都来自 ui/tokens 的 [JarvisScrim]，外壳只负责贴边位置。
 */

/** 栏面色：主题背景色按 [JarvisScrim.barAlpha] 半透明。 */
@Composable
internal fun shellBarColor(): Color =
    MaterialTheme.colorScheme.background.copy(alpha = JarvisScrim.barAlpha)

/** 顶栏下沿的淡出带：背景色@barAlpha → 透明，压在内容上，让内容滚到栏下时淡出。 */
@Composable
internal fun ShellTopFade(background: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(JarvisScrim.fadeLength)
            .background(JarvisScrim.topBarBrush(background))
    )
}

/** 内容区底部的淡入带：透明 → 背景色@barAlpha，向上接主题自绘底栏，避免交界出现硬线。 */
@Composable
internal fun ShellBottomFade(background: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(JarvisScrim.fadeLength)
            .background(JarvisScrim.bottomBarBrush(background))
    )
}
