package com.hancekim.mynote.core.ai.download

internal object ModelSource {
    const val FILE_NAME = "gemma-4-E2B-it.litertlm"
    const val REPO = "litert-community/gemma-4-E2B-it-litert-lm"
    const val COMMIT = "b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1"
    const val TOTAL_BYTES = 2_588_147_712L

    val url: String = "https://huggingface.co/$REPO/resolve/$COMMIT/$FILE_NAME"
}
