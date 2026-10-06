package com.hancekim.mynote.core.ai.download

import com.hancekim.mynote.core.ai.download.decision.ExpectedModel

internal object ModelSource {
    const val FILE_NAME = "gemma-4-E2B-it.litertlm"
    const val REPO = "litert-community/gemma-4-E2B-it-litert-lm"
    const val COMMIT = "b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1"
    const val TOTAL_BYTES = 2_588_147_712L
    const val SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"

    val url: String = "https://huggingface.co/$REPO/resolve/$COMMIT/$FILE_NAME"
    val expected = ExpectedModel(totalBytes = TOTAL_BYTES, sha256 = SHA256)
}
