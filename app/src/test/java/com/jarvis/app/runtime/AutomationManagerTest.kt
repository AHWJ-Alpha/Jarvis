package com.jarvis.app.runtime

import org.junit.Assert.*
import org.junit.Test

class AutomationManagerTest {
    @Test fun parsesBilibiliReplyRequestIntoPausedRuleDraft() {
        val draft = AutomationManager.parse("当哔哩哔哩上有人回复我就立刻回复")
        assertNotNull(draft)
        assertEquals(AutomationKind.BILIBILI_REPLY, draft?.kind)
        assertEquals("哔哩哔哩有人回复时自动回复", draft?.title)
    }

    @Test fun parsesPixivRequestAndAsksForArtistWhenMissing() {
        val draft = AutomationManager.parse("当 Pixiv 画师发布新作品就立刻下载")
        assertNotNull(draft)
        assertEquals(AutomationKind.PIXIV_NEW_WORK, draft?.kind)
        assertEquals("", draft?.artist)
    }

    @Test fun extractsAnExplicitPixivArtistId() {
        val draft = AutomationManager.parse("当 Pixiv 画师 ID 38200001 发布新作品时就下载")
        assertEquals("38200001", draft?.artist)
    }

    @Test fun ignoresRequestsThatAreNotSupportedAutomations() {
        assertNull(AutomationManager.parse("帮我下载 Pixiv 上的一张图"))
        assertNull(AutomationManager.parse("当哔哩哔哩有新视频就通知我"))
    }

    @Test fun bilibiliMatcherRequiresReplyToCurrentUserAndCorrectPackage() {
        assertTrue(AutomationManager.matchesNotification(
            AutomationKind.BILIBILI_REPLY, AutomationManager.BILIBILI_PACKAGE, "暗黑无界 回复了你在评论区的评论"
        ))
        assertFalse(AutomationManager.matchesNotification(
            AutomationKind.BILIBILI_REPLY, AutomationManager.BILIBILI_PACKAGE, "暗黑无界 回复了另一位用户"
        ))
        assertFalse(AutomationManager.matchesNotification(
            AutomationKind.BILIBILI_REPLY, "example.other.app", "暗黑无界 回复了你"
        ))
    }

    @Test fun pixivMatcherRequiresConfiguredArtistAndNewWorkText() {
        assertTrue(AutomationManager.matchesNotification(
            AutomationKind.PIXIV_NEW_WORK, AutomationManager.PIXIV_PACKAGE, "画师 38200001 发布了新作品", "38200001"
        ))
        assertFalse(AutomationManager.matchesNotification(
            AutomationKind.PIXIV_NEW_WORK, AutomationManager.PIXIV_PACKAGE, "推荐的新作品", "38200001"
        ))
        assertFalse(AutomationManager.matchesNotification(
            AutomationKind.PIXIV_NEW_WORK, AutomationManager.PIXIV_PACKAGE, "38200001 发布了新作品", ""
        ))
    }
}
