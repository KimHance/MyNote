package com.hancekim.mynote.core.ai.download.decision

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStepTest {
    @Test
    fun `아무것도 없으면 처음부터 받는다`() {
        assertEquals(DownloadStep.Fetch(0L), nextStep(LocalState(), TOTAL))
    }

    @Test
    fun `자리에 크기가 맞는 파일이 있으면 받지 않고 검사부터 한다`() {
        val local = LocalState(installedBytes = TOTAL)

        assertEquals(DownloadStep.VerifyInstalled, nextStep(local, TOTAL))
    }

    @Test
    fun `자리 파일 크기가 다르면 검사하지 않고 처음부터 받는다`() {
        val local = LocalState(installedBytes = TOTAL - 1)

        assertEquals(DownloadStep.Fetch(0L), nextStep(local, TOTAL))
    }

    @Test
    fun `part 가 다 찼으면 받지 않고 검사한다`() {
        val local = LocalState(partBytes = TOTAL)

        assertEquals(DownloadStep.VerifyPart, nextStep(local, TOTAL))
    }

    @Test
    fun `part 가 전체보다 길면 버리고 처음부터 받는다`() {
        val local = LocalState(partBytes = TOTAL + 1)

        assertEquals(DownloadStep.Fetch(0L), nextStep(local, TOTAL))
    }

    @Test
    fun `part 가 중간까지면 그 자리부터 이어 받는다`() {
        val local = LocalState(partBytes = 1_100_000_000L)

        assertEquals(DownloadStep.Fetch(1_100_000_000L), nextStep(local, TOTAL))
    }

    @Test
    fun `자리 파일 검사가 part 이어받기보다 먼저다`() {
        val local = LocalState(installedBytes = TOTAL, partBytes = 1_000L)

        assertEquals(DownloadStep.VerifyInstalled, nextStep(local, TOTAL))
    }

    private companion object {
        const val TOTAL = 2_588_147_712L
    }
}
