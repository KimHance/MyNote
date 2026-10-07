package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeroRepoTest {
    @Test
    fun `BR-6 연결이 없으면 저장소 줄이 없다`() {
        assertNull(NotesUiState(connection = ConnectionState.None).heroRepoName)
        assertNull(NotesUiState(connection = null).heroRepoName)
    }

    @Test
    fun `BR-6 저장소 줄은 저장된 정식 이름 그대로다`() {
        val saved = RepoConnection("Octo-Learner", "Android-Notes", "dev", "docs/index.json", isPublic = false)

        val state = NotesUiState(connection = ConnectionState.Connected(SourceId("s1"), saved))

        assertEquals("Octo-Learner/Android-Notes", state.heroRepoName)
    }
}
