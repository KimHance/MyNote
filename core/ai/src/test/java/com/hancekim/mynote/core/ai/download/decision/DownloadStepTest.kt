package com.hancekim.mynote.core.ai.download.decision

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStepTest {
    @Test
    fun `아무것도 없으면 처음부터 받는다`() {
        assertEquals(DownloadStep.Fetch(0L), nextStep(LocalState(), REMOTE))
    }

    @Test
    fun `자리에 크기가 맞는 파일이 있으면 받지 않고 검사부터 한다`() {
        val local = LocalState(installedBytes = TOTAL)

        assertEquals(DownloadStep.VerifyInstalled, nextStep(local, REMOTE))
    }

    @Test
    fun `자리 파일 크기가 다르면 검사하지 않고 처음부터 받는다`() {
        val local = LocalState(installedBytes = TOTAL - 1)

        assertEquals(DownloadStep.Fetch(0L), nextStep(local, REMOTE))
    }

    @Test
    fun `part 가 다 찼으면 받지 않고 검사한다`() {
        val local = LocalState(partBytes = TOTAL, savedPlan = REMOTE)

        assertEquals(DownloadStep.VerifyPart, nextStep(local, REMOTE))
    }

    @Test
    fun `part 가 전체보다 길면 버리고 처음부터 받는다`() {
        val local = LocalState(partBytes = TOTAL + 1, savedPlan = REMOTE)

        assertEquals(DownloadStep.Fetch(0L), nextStep(local, REMOTE))
    }

    @Test
    fun `part 가 중간까지면 그 자리부터 이어 받는다`() {
        val local = LocalState(partBytes = 1_100_000_000L, savedPlan = REMOTE)

        assertEquals(DownloadStep.Fetch(1_100_000_000L), nextStep(local, REMOTE))
    }

    @Test
    fun `plan 의 커밋이 다르면 part 를 믿지 않는다`() {
        val local = LocalState(partBytes = 1_000L, savedPlan = REMOTE.copy(commit = OTHER_COMMIT))

        assertEquals(DownloadStep.Fetch(0L), nextStep(local, REMOTE))
    }

    @Test
    fun `plan 의 해시가 다르면 part 를 믿지 않는다`() {
        val local = LocalState(partBytes = 1_000L, savedPlan = REMOTE.copy(sha256 = OTHER_SHA))

        assertEquals(DownloadStep.Fetch(0L), nextStep(local, REMOTE))
    }

    @Test
    fun `plan 없이 part 만 있으면 믿지 않는다`() {
        val local = LocalState(partBytes = 1_000L, savedPlan = null)

        assertEquals(DownloadStep.Fetch(0L), nextStep(local, REMOTE))
    }

    @Test
    fun `자리 파일 검사가 part 이어받기보다 먼저다`() {
        val local = LocalState(installedBytes = TOTAL, partBytes = 1_000L, savedPlan = REMOTE)

        assertEquals(DownloadStep.VerifyInstalled, nextStep(local, REMOTE))
    }

    private companion object {
        const val TOTAL = 2_588_147_712L
        const val OTHER_COMMIT = "0000000000000000000000000000000000000000"
        const val OTHER_SHA = "ffff38105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
        val REMOTE =
            DownloadPlan(
                commit = "b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1",
                sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
                totalBytes = TOTAL,
            )
    }
}
