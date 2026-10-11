package com.hancekim.mynote.core.ui.element

import com.hancekim.mynote.core.domain.model.ModelFailure
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.ui.R
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

        assertEquals(R.string.ui_model_failed_space, noSpace.detail)
        assertEquals(R.string.ui_model_help_space, noSpace.help)
        assertEquals(R.string.ui_model_failed_generic, other.detail)
        assertEquals(R.string.ui_model_help, other.help)
    }

    @Test
    fun `준비됨은 기기 밖으로 나가지 않는다는 도움말을 쓴다`() {
        assertEquals(R.string.ui_model_help_ready, modelCardUi(ModelStatus.Ready).help)
        assertEquals(listOf(ModelAction.Delete), modelCardUi(ModelStatus.Ready).actions)
    }

    @Test
    fun `MD-8 재시도 카드는 초 없이 이어 받는다고 말한다`() {
        val card = modelCardUi(ModelStatus.Retrying(RECEIVED, TOTAL))

        assertEquals(ModelTone.Warn, card.tone)
        assertEquals(R.string.ui_model_retrying, card.label)
        assertEquals(R.string.ui_model_retrying_desc, card.detail)
        assertEquals(null, card.detailArg)
        assertEquals(ModelProgressUi.Determinate(RECEIVED, TOTAL, ModelTone.Warn), card.progress)
        assertEquals(listOf(ModelAction.Cancel), card.actions)
    }

    @Test
    fun `MD-8 재시도 알약에는 초가 없다`() {
        val pill = modelPillUi(ModelStatus.Retrying(RECEIVED, TOTAL))

        assertEquals(R.string.ui_pill_retrying, pill.label)
        assertEquals(ModelTone.Warn, pill.tone)
        assertEquals(null, pill.percent)
        assertEquals(RECEIVED.toFloat() / TOTAL, pill.progress!!, 0.0001f)
        assertEquals(null, pill.icon)
    }

    @Test
    fun `MD-12 알림 꺼짐 안내는 작업 중에만 보인다`() {
        listOf(
            ModelStatus.WaitingForWifi,
            ModelStatus.Preparing,
            ModelStatus.Downloading(RECEIVED, TOTAL),
            ModelStatus.Retrying(RECEIVED, TOTAL),
            ModelStatus.Verifying,
        ).forEach { assertTrue(it.toString(), modelCardUi(it).notificationHint) }

        listOf(
            ModelStatus.Paused(RECEIVED, TOTAL),
            ModelStatus.Missing,
            ModelStatus.Ready,
            ModelStatus.Failed(ModelFailure.NO_SPACE),
        ).forEach { assertFalse(it.toString(), modelCardUi(it).notificationHint) }
    }

    @Test
    fun `MD-6 옛 no-plan 실패는 일반 실패 카드로 보인다`() {
        val card = modelCardUi(ModelStatus.Failed("no-plan"))

        assertEquals(R.string.ui_model_failed, card.label)
        assertEquals(R.string.ui_model_failed_generic, card.detail)
        assertEquals(R.string.ui_model_help, card.help)
        assertEquals(listOf(ModelAction.Retry), card.actions)
    }

    @Test
    fun `받는 중 알약만 퍼센트를 보조 글자로 쓴다`() {
        assertEquals(42, modelPillUi(ModelStatus.Downloading(RECEIVED, TOTAL)).percent)

        listOf(
            ModelStatus.WaitingForWifi,
            ModelStatus.Preparing,
            ModelStatus.Retrying(RECEIVED, TOTAL),
            ModelStatus.Verifying,
            ModelStatus.Ready,
            ModelStatus.Failed(ModelFailure.NO_SPACE),
        ).forEach { assertEquals(it.toString(), null, modelPillUi(it).percent) }
    }

    @Test
    fun `준비됨과 실패 알약은 색과 아이콘으로 가른다`() {
        val ready = modelPillUi(ModelStatus.Ready)
        val failed = modelPillUi(ModelStatus.Failed(ModelFailure.NO_SPACE))

        assertEquals(ModelTone.Correct, ready.tone)
        assertEquals(PillIcon.Check, ready.icon)
        assertEquals(ModelTone.Wrong, failed.tone)
        assertEquals(PillIcon.Warn, failed.icon)
    }

    @Test
    fun `작업 중인 상태만 바쁨으로 센다`() {
        listOf(
            ModelStatus.WaitingForWifi,
            ModelStatus.Preparing,
            ModelStatus.Downloading(1L, 2L),
            ModelStatus.Retrying(1L, 2L),
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

    private companion object {
        const val RECEIVED = 1_090_000_000L
        const val TOTAL = 2_588_147_712L
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
        assertEquals(R.string.ui_model_download, cta.chip)
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
        assertEquals(R.string.ui_model_retry, cta.chip)
    }
}
