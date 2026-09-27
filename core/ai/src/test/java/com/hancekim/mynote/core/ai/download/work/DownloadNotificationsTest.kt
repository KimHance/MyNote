package com.hancekim.mynote.core.ai.download.work

import com.hancekim.mynote.core.ai.R
import com.hancekim.mynote.core.ai.download.transfer.ModelInstaller
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadNotificationsTest {
    @Test
    fun `용량이 모자라 실패하면 용량 문구를 쓴다`() {
        assertEquals(R.string.ai_model_download_no_space, failureBodyRes(ModelInstaller.REASON_NO_SPACE))
    }

    @Test
    fun `그 밖의 실패는 기본 문구를 쓴다`() {
        assertEquals(R.string.ai_model_download_failed_body, failureBodyRes(ModelInstaller.REASON_NO_PLAN))
    }
}
