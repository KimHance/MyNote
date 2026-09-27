package com.hancekim.mynote.core.ai.download.transfer

import com.hancekim.mynote.core.ai.download.decision.DownloadPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelFilesTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun files() = ModelFiles(temp.root, MODEL)

    @Test
    fun `받는 중 파일은 본명과 다른 이름을 쓴다`() {
        val files = files()

        assertEquals(MODEL, files.target.name)
        assertEquals("$MODEL.part", files.part.name)
        assertEquals("$MODEL.plan", files.plan.name)
    }

    @Test
    fun `plan 을 저장하고 그대로 다시 읽는다`() {
        val files = files()

        files.savePlan(PLAN)

        assertEquals(PLAN, files.savedPlan())
    }

    @Test
    fun `plan 이 없으면 null 이다`() {
        assertNull(files().savedPlan())
    }

    @Test
    fun `plan 이 깨져 있으면 던지지 않고 null 이다`() {
        val files = files()
        files.plan.writeText("{ 이건 JSON 이 아니다")

        assertNull(files.savedPlan())
    }

    @Test
    fun `part 가 없으면 받은 바이트는 0 이다`() {
        assertEquals(0L, files().partBytes())
    }

    @Test
    fun `part 의 길이가 곧 받은 바이트다`() {
        val files = files()
        files.part.writeBytes(ByteArray(1500))

        assertEquals(1500L, files.partBytes())
    }

    @Test
    fun `part 를 버려도 plan 은 남는다`() {
        val files = files()
        files.part.writeBytes(ByteArray(10))
        files.savePlan(PLAN)

        files.discardPart()

        assertFalse(files.part.exists())
        assertEquals(PLAN, files.savedPlan())
    }

    @Test
    fun `promote 는 part 를 본명으로 옮기고 plan 을 지운다`() {
        val files = files()
        files.part.writeBytes(CONTENT)
        files.savePlan(PLAN)

        files.promote()

        assertFalse(files.part.exists())
        assertFalse(files.plan.exists())
        assertArrayEqualsBytes(CONTENT, files.target.readBytes())
    }

    @Test
    fun `promote 는 이미 자리에 있는 파일을 대체한다`() {
        val files = files()
        files.target.writeBytes(ByteArray(99))
        files.part.writeBytes(CONTENT)

        files.promote()

        assertArrayEqualsBytes(CONTENT, files.target.readBytes())
    }

    @Test
    fun `sha256 은 알려진 값을 낸다`() {
        val file = temp.newFile("hello.txt").apply { writeText("hello") }

        assertEquals(HELLO_SHA256, ModelFiles.sha256Of(file))
    }

    @Test
    fun `크기가 다르면 plan 과 맞지 않는다`() {
        val files = files()
        files.target.writeBytes(ByteArray(CONTENT.size - 1))

        assertFalse(files.matches(files.target, planFor(CONTENT)))
    }

    @Test
    fun `크기는 같고 해시가 다르면 맞지 않는다`() {
        val files = files()
        files.target.writeBytes(ByteArray(CONTENT.size))

        assertFalse(files.matches(files.target, planFor(CONTENT)))
    }

    @Test
    fun `크기와 해시가 모두 같으면 맞다`() {
        val files = files()
        files.target.writeBytes(CONTENT)

        assertTrue(files.matches(files.target, planFor(CONTENT)))
    }

    @Test
    fun `없는 파일은 plan 과 맞지 않는다`() {
        val files = files()

        assertFalse(files.matches(files.target, planFor(CONTENT)))
    }

    private fun planFor(bytes: ByteArray): DownloadPlan {
        val probe = temp.newFile("probe-${bytes.size}-${bytes.hashCode()}").apply { writeBytes(bytes) }
        return DownloadPlan(
            commit = COMMIT,
            sha256 = ModelFiles.sha256Of(probe),
            totalBytes = bytes.size.toLong(),
        )
    }

    private fun assertArrayEqualsBytes(expected: ByteArray, actual: ByteArray) =
        assertEquals(expected.toList(), actual.toList())

    private companion object {
        const val MODEL = "gemma-4-E2B-it.litertlm"
        const val COMMIT = "b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1"
        const val HELLO_SHA256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
        val CONTENT = "핸스노트 퀴즈 모델".toByteArray()
        val PLAN =
            DownloadPlan(
                commit = COMMIT,
                sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
                totalBytes = 2_588_147_712L,
            )
    }
}
