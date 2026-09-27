package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.domain.model.ModelFailure
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.feature.notes.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelStatusUiTest {
    @Test
    fun `모델이 없으면 받기 하나만 내민다`() {
        val card = modelCardUi(ModelStatus.Missing)

        assertEquals(listOf(ModelAction.Download), card.actions)
        assertEquals(ModelProgressUi.None, card.progress)
    }

    @Test
    fun `받는 중에는 취소만 내민다`() {
        val card = modelCardUi(ModelStatus.Downloading(received = 1_090_000_000L, total = 2_588_147_712L))

        assertEquals(listOf(ModelAction.Cancel), card.actions)
        assertEquals(42, (card.progress as ModelProgressUi.Determinate).percent)
    }

    @Test
    fun `멈췄으면 이어 받기와 지우기를 함께 내민다`() {
        val card = modelCardUi(ModelStatus.Paused(received = 1_090_000_000L, total = 2_588_147_712L))

        assertEquals(listOf(ModelAction.Resume, ModelAction.DiscardPartial), card.actions)
        assertEquals("1.09", card.detailArg)
    }

    @Test
    fun `공간 부족은 일반 실패와 다른 문구와 도움말을 쓴다`() {
        val noSpace = modelCardUi(ModelStatus.Failed(ModelFailure.NO_SPACE))
        val other = modelCardUi(ModelStatus.Failed("no-plan"))

        assertEquals(R.string.notes_model_failed_space, noSpace.detail)
        assertEquals(R.string.notes_model_help_space, noSpace.help)
        assertEquals(R.string.notes_model_failed_generic, other.detail)
        assertEquals(R.string.notes_model_help, other.help)
    }

    @Test
    fun `준비됨은 기기 밖으로 나가지 않는다는 도움말을 쓴다`() {
        assertEquals(R.string.notes_model_help_ready, modelCardUi(ModelStatus.Ready).help)
        assertEquals(listOf(ModelAction.Delete), modelCardUi(ModelStatus.Ready).actions)
    }

    @Test
    fun `재시도 대기 초가 없으면 세부 문구를 비운다`() {
        val withSeconds = modelCardUi(ModelStatus.Retrying(0L, 1L, inSeconds = 4))
        val without = modelCardUi(ModelStatus.Retrying(0L, 1L, inSeconds = null))

        assertEquals("4", withSeconds.detailArg)
        assertEquals(null, without.detail)
    }

    @Test
    fun `작업 중인 상태만 바쁨으로 센다`() {
        listOf(
            ModelStatus.WaitingForWifi,
            ModelStatus.Preparing,
            ModelStatus.Downloading(1L, 2L),
            ModelStatus.Retrying(1L, 2L, null),
            ModelStatus.Verifying,
        ).forEach { assertTrue(it.isBusy) }

        listOf(
            ModelStatus.Missing,
            ModelStatus.Ready,
            ModelStatus.Paused(1L, 2L),
            ModelStatus.Failed("x"),
        ).forEach { assertFalse(it.isBusy) }
    }

    @Test
    fun `기가바이트는 소수 둘째 자리까지 보여 준다`() {
        assertEquals("1.09", gigabytes(1_090_000_000L))
        assertEquals("2.59", gigabytes(2_588_147_712L))
        assertEquals("0.00", gigabytes(0L))
    }
}

class QuizCtaUiTest {
    @Test
    fun `준비됐을 때만 샤인을 켠다`() {
        assertTrue(quizCtaUi(ModelStatus.Ready).shine)
        listOf(
            ModelStatus.Missing,
            ModelStatus.Downloading(1L, 2L),
            ModelStatus.Paused(1L, 2L),
            ModelStatus.Failed("x"),
        ).forEach { assertFalse(quizCtaUi(it).shine) }
    }

    @Test
    fun `모델이 없어도 누를 수 있고 확인 다이얼로그로 간다`() {
        val cta = quizCtaUi(ModelStatus.Missing)

        assertEquals(CtaAction.ConfirmDownload, cta.action)
        assertEquals(R.string.notes_model_download, cta.chip)
    }

    @Test
    fun `멈춘 상태는 이어 받기로 바로 시작한다`() {
        val cta = quizCtaUi(ModelStatus.Paused(1_090_000_000L, 2_588_147_712L))

        assertEquals(CtaAction.StartDownload, cta.action)
        assertEquals("1.09", cta.subtitleArg)
    }

    @Test
    fun `받는 중에는 칩 없이 설정으로 보낸다`() {
        val cta = quizCtaUi(ModelStatus.Downloading(1_294_073_856L, 2_588_147_712L))

        assertEquals(CtaAction.OpenSettings, cta.action)
        assertEquals(null, cta.chip)
    }

    @Test
    fun `실패는 경고색으로 다시 시도를 내민다`() {
        val cta = quizCtaUi(ModelStatus.Failed("no-space"))

        assertTrue(cta.warn)
        assertEquals(R.string.notes_model_retry, cta.chip)
    }
}
