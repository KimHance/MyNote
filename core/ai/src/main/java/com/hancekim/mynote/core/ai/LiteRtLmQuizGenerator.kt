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

/**
 * LiteRT-LM 을 직접 호출하는 [QuizGenerator] 구현.
 *
 * 툴을 쓰지 않는다. 출제할 노트가 이미 정해져 있어 모델이 "무엇을 읽을지" 고를 필요가 없고,
 * 툴 왕복은 한 번에 10초 이상이라 순수 손해다.
 */
class LiteRtLmQuizGenerator
    @Inject
    constructor(
        private val engine: ModelEngine,
    ) : QuizGenerator {
        override suspend fun isAvailable(): Boolean = engine.isAvailable()

        override suspend fun generate(
            noteTitle: String,
            noteText: String,
            count: Int,
        ): Result<List<QuizQuestion>> =
            // 취소는 실패가 아니다 — 사용자가 '그만두기' 를 누른 것이라 Failed 화면으로 떨어뜨리면 안 된다.
            runCatchingCancellable {
                engine.use { e ->
                    // 퀴즈는 서로 독립이어야 한다. 대화를 재사용하면 앞 노트 내용이 다음 문제에 샌다.
                    e.createConversation(conversationConfig(count)).use { conversation ->
                        val started = System.currentTimeMillis()
                        val raw = conversation.sendMessage(prompt(noteTitle, noteText, count)).text()
                        Log.i(TAG, "생성 ${System.currentTimeMillis() - started}ms · ${raw.length}자")
                        raw
                    }
                }
            }.fold(
                onSuccess = { QuizJsonParser.parse(it) },
                // 여기까지 온 예외는 전부 엔진·모델 쪽이다. 파싱 실패는 위 parse 가 따로 구분해 준다.
                onFailure = { Result.failure(QuizFailure.Engine(it)) },
            )

        private fun conversationConfig(count: Int) =
            ConversationConfig(
                systemInstruction = Contents.of(SYSTEM),
                // thinking 은 끈다. 켜면 사용자에게 안 보이는 글을 수천 자 쓰느라 수십 초가 더 걸린다.
                thinkingConfig = ThinkingConfig(enableThinking = false),
                // 구조화 출력이라 창의성이 필요 없다. 형식이 흔들리면 파싱이 깨진다.
                samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0),
                maxOutputToken = TOKENS_PER_QUESTION * count,
            )

        private fun prompt(
            noteTitle: String,
            noteText: String,
            count: Int,
        ): String =
            """
            다음 학습 노트를 읽고 4지선다 문제를 정확히 ${count}개 만들어라.

            제목: $noteTitle
            본문:
            $noteText

            규칙:
            - 노트에 실제로 있는 내용만 묻는다. 노트에 없으면 만들지 마라.
            - answerIndex 는 choices 의 0부터 시작하는 위치다.
            - 설명은 한 문장.
            - JSON 배열만 출력하고 다른 말은 쓰지 마라.

            형식:
            [{"question":"...","choices":["...","...","...","..."],"answerIndex":0,"explanation":"..."}]
            """.trimIndent()

        /** 응답이 Message 라 텍스트만 꺼낸다. */
        private fun Message.text(): String =
            contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }

        private companion object {
            const val TAG = "MyNoteAi"
            const val SYSTEM = "너는 학습 노트로 문제를 내는 출제자다. 요청받은 JSON 형식만 출력한다."

            /**
             * 문제 하나에 잡는 출력 예산.
             *
             * 220 은 **모자랐다** — 3문제 실측이 972자로 정확히 상한에 걸려 마지막 해설이 문장
             * 중간에서 잘렸다. 한국어는 토큰당 글자 수가 적어(대략 1.5자) 질문+선택지 4개+해설
             * 한 문장이 220 토큰을 넘긴다. 잘린 해설은 없는 것만 못하므로 여유를 준다.
             * 디코드가 초당 200자쯤이라 문제당 100 토큰을 더 줘도 체감은 1초 안쪽이다.
             */
            const val TOKENS_PER_QUESTION = 340
        }
    }
