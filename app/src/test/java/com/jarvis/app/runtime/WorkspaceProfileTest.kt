package com.jarvis.app.runtime

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceProfileTest {
    @Test
    fun customPersonaAndWorldBookRoundTrip() {
        val original = WorkspaceProfile("创作", false, "你是画师助手", "角色设定")
        val restored = WorkspaceProfile.fromJson(JSONObject(original.toJson().toString()))
        assertEquals(original, restored)
        assertFalse(restored!!.useGlobalPersona)
    }

    @Test
    fun oldWorkspaceWithoutPersonaFieldsUsesGlobalSettings() {
        val restored = WorkspaceProfile.fromJson(JSONObject().put("name", "旧工作区"))
        assertEquals(true, restored?.useGlobalPersona)
        assertEquals("", restored?.mentorPrompt)
        assertNull(WorkspaceProfile.fromJson(JSONObject().put("name", " ")))
    }

    @Test
    fun emptyCustomFieldsFallBackIndependentlyToGlobalSettings() {
        val profile = WorkspaceProfile("研究", false, "自定义人格", "")
        assertEquals("自定义人格", profile.resolvedMentorPrompt("共同人格"))
        assertEquals("共同世界书", profile.resolvedWorldBook("共同世界书"))
        val shared = WorkspaceProfile("共享")
        assertEquals("共同人格", shared.resolvedMentorPrompt("共同人格"))
        assertEquals("共同世界书", shared.resolvedWorldBook("共同世界书"))
    }
}
