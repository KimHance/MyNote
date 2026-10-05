package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.model.BookmarkEventKind
import com.hancekim.mynote.core.domain.model.FontScale
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.NoteFacts
import com.hancekim.mynote.core.domain.model.QuizAnswer
import com.hancekim.mynote.core.domain.model.QuizAttempt
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.StudyWindow
import com.hancekim.mynote.core.domain.model.WrongAnswer
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

internal class FakeSettings(
    enabled: Boolean,
) : SettingsRepository {
    val studyLog = MutableStateFlow(enabled)

    override fun observeFontScale(): Flow<FontScale> = MutableStateFlow(FontScale.Normal)

    override suspend fun setFontScale(scale: FontScale) = Unit

    override fun observeStudyLogEnabled(): Flow<Boolean> = studyLog

    override suspend fun setStudyLogEnabled(enabled: Boolean) {
        studyLog.value = enabled
    }
}

internal class FakeStudyLog : StudyLogRepository {
    val calls = mutableListOf<String>()
    var pruneBefore: Long? = null
    var pruneAnswersBefore: Long? = null

    override suspend fun record(session: ReadSession) {
        calls += "session"
    }

    override suspend fun record(attempt: QuizAttempt) {
        calls += "attempt"
    }

    override suspend fun record(event: BookmarkEvent): Long {
        calls += "event"
        return 7L
    }

    override suspend fun deleteBookmarkEvent(id: Long) {
        calls += "delete:$id"
    }

    override fun observeWindow(
        from: Long,
        until: Long,
    ): Flow<StudyWindow> = emptyFlow()

    override fun observeNoteFacts(): Flow<NoteFacts> = emptyFlow()

    override fun observeFirstRecordAt(): Flow<Long?> = emptyFlow()

    override suspend fun wrongAnswers(
        since: Long,
        limit: Int,
    ): List<WrongAnswer> = emptyList()

    override suspend fun countWrongAnswers(since: Long): Int = 0

    override suspend fun clear() {
        calls += "clear"
    }

    override suspend fun prune(before: Long) {
        pruneBefore = before
    }

    override suspend fun pruneAnswers(before: Long) {
        pruneAnswersBefore = before
    }
}

private val session = ReadSession("a", "android", 0L, 40_000L, 40_000L, 50)
private val attempt = QuizAttempt("a", "android", 0L, 1L, listOf(QuizAnswer("q", listOf("x", "y", "z"), 0, 1)))
private val event = BookmarkEvent("a", "android", BookmarkEventKind.Added, 0L)

class StudyLogUseCaseTest {
    @Test
    fun `기록이 꺼져 있으면 저장소를 한 번도 부르지 않는다`() =
        runTest {
            val log = FakeStudyLog()
            val settings = FakeSettings(enabled = false)

            RecordReadSessionUseCase(log, settings)(session)
            RecordQuizAttemptUseCase(log, settings)(attempt)
            val id = RecordBookmarkEventUseCase(log, settings)(event)

            assertEquals(emptyList<String>(), log.calls)
            assertNull(id)
        }

    @Test
    fun `기록이 켜져 있으면 세 종류 모두 쓰고 북마크 이벤트는 행 id 를 돌려준다`() =
        runTest {
            val log = FakeStudyLog()
            val settings = FakeSettings(enabled = true)

            RecordReadSessionUseCase(log, settings)(session)
            RecordQuizAttemptUseCase(log, settings)(attempt)
            val id = RecordBookmarkEventUseCase(log, settings)(event)

            assertEquals(listOf("session", "attempt", "event"), log.calls)
            assertEquals(7L, id)
        }

    @Test
    fun `닫는 순간의 설정으로 판단한다`() =
        runTest {
            val log = FakeStudyLog()
            val settings = FakeSettings(enabled = false)
            val record = RecordReadSessionUseCase(log, settings)

            settings.setStudyLogEnabled(true)
            record(session)

            assertEquals(listOf("session"), log.calls)
        }

    @Test
    fun `답이 없는 시도는 쓰지 않는다`() =
        runTest {
            val log = FakeStudyLog()

            RecordQuizAttemptUseCase(log, FakeSettings(enabled = true))(attempt.copy(answers = emptyList()))

            assertEquals(emptyList<String>(), log.calls)
        }

    @Test
    fun `정리는 지금으로부터 365일 전보다 오래된 것을 지운다`() =
        runTest {
            val log = FakeStudyLog()
            val now = 400L * 86_400_000L

            PruneStudyLogUseCase(log, FakeInsights(), { now }, { ZoneOffset.UTC })()

            assertEquals(35L * 86_400_000L, log.pruneBefore)
        }

    @Test
    fun `문항 텍스트는 90일 넘은 것만 지우고 시도는 365일까지 남긴다`() =
        runTest {
            val log = FakeStudyLog()
            val day = 86_400_000L
            val now = 400L * day

            PruneStudyLogUseCase(log, FakeInsights(), { now }, { ZoneOffset.UTC })()

            assertEquals(310L * day, log.pruneAnswersBefore)
            assertEquals(35L * day, log.pruneBefore)
            val hundredDaysAgo = now - 100L * day
            val eightyNineDaysAgo = now - 89L * day
            assertTrue(hundredDaysAgo < log.pruneAnswersBefore!!)
            assertTrue(eightyNineDaysAgo >= log.pruneAnswersBefore!!)
            assertTrue(hundredDaysAgo >= log.pruneBefore!!)
        }

    @Test
    fun `정리는 이번 주 포함 26주 치 요약만 남긴다`() =
        runTest {
            val insights = FakeInsights()
            val now =
                LocalDate
                    .of(2026, 10, 2)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()

            PruneStudyLogUseCase(FakeStudyLog(), insights, { now }, { ZoneOffset.UTC })()

            assertEquals(LocalDate.of(2026, 4, 6), insights.prunedBefore)
        }

    @Test
    fun `기록 지우기는 워커 취소 다음 epoch 를 올리고 기록과 상태를 지운 뒤 조건이 맞으면 다시 예약한다`() =
        runTest {
            val calls = mutableListOf<String>()
            val log = FakeStudyLog()
            val insights = FakeInsights(calls)
            val scheduler = FakeScheduler(calls)

            ClearStudyLogUseCase(log, insights, scheduler, FakeModelSource(ModelStatus.Ready), FakeSettings(true))()

            assertEquals(listOf("cancel", "epoch", "clearStatus", "schedule"), calls)
            assertEquals(listOf("clear"), log.calls)
        }

    @Test
    fun `모델이 없으면 기록을 지운 뒤 다시 예약하지 않는다`() =
        runTest {
            val calls = mutableListOf<String>()

            ClearStudyLogUseCase(
                FakeStudyLog(),
                FakeInsights(calls),
                FakeScheduler(calls),
                FakeModelSource(ModelStatus.Missing),
                FakeSettings(true),
            )()

            assertEquals(listOf("cancel", "epoch", "clearStatus"), calls)
        }

    @Test
    fun `기록이 꺼져 있으면 다시 예약하지 않는다`() =
        runTest {
            val calls = mutableListOf<String>()

            ClearStudyLogUseCase(
                FakeStudyLog(),
                FakeInsights(calls),
                FakeScheduler(calls),
                FakeModelSource(ModelStatus.Ready),
                FakeSettings(false),
            )()

            assertEquals(listOf("cancel", "epoch", "clearStatus"), calls)
        }
}
