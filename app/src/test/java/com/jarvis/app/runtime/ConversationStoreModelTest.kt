package com.jarvis.app.runtime

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ConversationStoreModelTest {
    @Test
    fun conversationJsonRoundTripPreservesMessagesAndWorkspace() {
        val original = ChatSession(
            title = "Pixiv 下载",
            workspace = "创作",
            messages = listOf(
                ChatMessage(role = ChatMessageRole.USER, content = "帮我查找新作品"),
                ChatMessage(role = ChatMessageRole.ASSISTANT, content = "找到了两件作品。")
            )
        )

        val restored = ChatSession.fromJson(JSONObject(original.toJson().toString()))

        assertNotNull(restored)
        assertEquals(original.id, restored?.id)
        assertEquals(original.title, restored?.title)
        assertEquals(original.workspace, restored?.workspace)
        assertEquals(original.messages.map { it.content }, restored?.messages?.map { it.content })
        assertEquals(original.messages.map { it.role }, restored?.messages?.map { it.role })
    }
}
