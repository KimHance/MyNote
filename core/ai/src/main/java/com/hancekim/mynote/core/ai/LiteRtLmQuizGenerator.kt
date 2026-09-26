package com.hancekim.mynote.core.ai

import android.util.Log
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.hancekim.mynote.core.common.result.runCatchingCancellable
import com.hancekim.mynote.core.domain.model.QuizFailure
import com.hancekim.mynote.core.domain.model.QuizQuestion
import com.hancekim.mynote.core.domain.repository.QuizGenerator
import javax.inject.Inject

class LiteRtLmQuizGenerator @Inject constructor(
    private val engine: ModelEngine,
) : QuizGenerator {
    override suspend fun isAvailable(): Boolean = engine.isAvailable()

    override suspend fun generate(
        noteTitle: String,
        noteText: String,
        count: Int,
    ): Result<List<QuizQuestion>> =
        runCatchingCancellable {
            engine.use { e ->
                e.createConversation(conversationConfig(count)).use { conversation ->
                    val started = System.currentTimeMillis()
                    val raw = conversation.sendMessage(prompt(noteTitle, noteText, count)).text()
                    Log.i(TAG, "생성 ${System.currentTimeMillis() - started}ms · ${raw.length}자")
                    raw
                }
            }
        }.fold(
            onSuccess = { QuizJsonParser.parse(it) },
            onFailure = { Result.failure(QuizFailure.Engine(it)) },
        )

    private fun conversationConfig(count: Int) =
        ConversationConfig(
            systemInstruction = Contents.of(SYSTEM),
            thinkingConfig = ThinkingConfig(enableThinking = false),
            samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0),
            maxOutputToken = TOKENS_PER_QUESTION * count,
        )

    private fun prompt(
        noteTitle: String,
        noteText: String,
        count: Int,
    ): String =
        """
        Write exactly $count multiple-choice questions from the study note below.

        TITLE: $noteTitle
        NOTE:
        $noteText

        Rules:
        - Ask only what the note actually states. Invent nothing.
        - Exactly 4 choices. Never more, never fewer.
        - The 4 choices must differ in meaning, not just in wording. Do not repeat a
          choice, and do not restate one with a synonym or an extra word.
        - Wrong choices must look plausible but be clearly wrong to a careful reader.
        - answerIndex is the 0-based position in choices.
        - explanation: one sentence, why the answer is right.
        - Write question, choices and explanation in KOREAN.
        - Output the JSON array only. No prose, no code fence.

        Format:
        [{"question":"...","choices":["...","...","...","..."],"answerIndex":0,"explanation":"..."}]
        """.trimIndent()

    private fun Message.text(): String =
        contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }

    private companion object {
        const val TAG = "MyNoteAi"
        const val SYSTEM =
            "You write quiz questions from study notes. " +
                "You reply with the requested JSON array and nothing else."
        const val TOKENS_PER_QUESTION = 340
    }
}
