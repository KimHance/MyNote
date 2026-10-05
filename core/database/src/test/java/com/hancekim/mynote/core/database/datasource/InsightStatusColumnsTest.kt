package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.domain.model.InsightFailureKind
import com.hancekim.mynote.core.domain.model.InsightResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InsightStatusColumnsTest {
    @Test
    fun `저장 문자열은 enum 이름이 아니라 고정 문자열이다`() {
        assertEquals(listOf("success", "failure", "skipped"), InsightResult.entries.map { it.toColumn() })
        assertEquals(listOf("parse", "engine", "timeout"), InsightFailureKind.entries.map { it.toColumn() })
    }

    @Test
    fun `되읽으면 같은 값이고 모르는 값은 null 이다`() {
        InsightResult.entries.forEach { assertEquals(it, it.toColumn().toInsightResult()) }
        InsightFailureKind.entries.forEach { assertEquals(it, it.toColumn().toFailureKind()) }
        assertNull("Success".toInsightResult())
        assertNull("oom".toFailureKind())
    }
}
