package com.hancekim.mynote.shell

import com.hancekim.mynote.core.domain.model.ModelStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelPillTest {
    @Test
    fun `받는 중이던 모델이 준비되면 알약을 띄운다`() {
        assertTrue(readyJustArrived(ModelStatus.Verifying, ModelStatus.Ready))
        assertTrue(readyJustArrived(ModelStatus.Downloading(1L, 2L), ModelStatus.Ready))
    }

    @Test
    fun `앱을 켤 때 이미 준비된 모델은 알약을 띄우지 않는다`() {
        assertFalse(readyJustArrived(null, ModelStatus.Ready))
        assertFalse(readyJustArrived(ModelStatus.Missing, ModelStatus.Ready))
    }

    @Test
    fun `준비되지 않았으면 알약을 띄우지 않는다`() {
        assertFalse(readyJustArrived(ModelStatus.Downloading(1L, 2L), ModelStatus.Verifying))
    }
}
