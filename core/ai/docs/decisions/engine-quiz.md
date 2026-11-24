# Decisions

### QZ-D1 · 2026-09 · QZ-6
- decision: There is one engine for the whole app. It is built on the first quiz request, kept afterwards (a quiz never closes it) and used only through one entry point that holds a mutex; the statistics worker takes it only without waiting and closes an engine it created itself when the app is in the background.
- why: Starting the engine takes seconds (3.5 to 12 s) and about 2.5 GB, so a person who never opens a quiz must not carry it, and a person who opens two quizzes must not pay twice. The library does not lock its conversation object, so serialising is the app's job and bypassing the entry point loses the guarantee.
- rejected: Building the engine per request (the GPU benefit is lost); building it at app start (memory for people who never use quiz).
- evidence: `core/ai` `ModelEngine`, `EngineSlot`; `EngineSlotTest` (`퀴즈가 올려 둔 엔진은 재사용만 하고 닫지 않는다`).

### QZ-D2 · 2026-09 · QZ-7, QZ-17
- decision: Generation runs with thinking off, greedy sampling and an output budget of 340 tokens per question; the retry that gets a person out of a failure is "fewer questions", not "same again".
- why: Turning thinking on adds about 50 seconds and turning the speculative decoding off about 27; JSON output wants temperature 0. A side effect is that the same note and count give almost the same output, so a same-conditions retry rarely helps. 220 tokens per question cut explanations mid-sentence (Korean carries few characters per token), and a short budget shows up as a Parse failure that looks like a model mistake.
- rejected: Thinking on; sampling with temperature; 220 tokens per question; same-conditions retry as the main way out.
- evidence: `LiteRtLmQuizGenerator` (`TOKENS_PER_QUESTION`), `QuizUiState.Failed.canRetryWithFewer`, `QuizGradingTest` (`형식이 깨져 실패했고 문항이 남아 있으면 줄여서 다시 낼 수 있다`).

### QZ-D3 · 2026-09 · QZ-7
- decision: The instructions are written in English and the model is told in one line to write the question, choices and explanation in Korean; the note text stays Korean; the prompt asks for four choices that differ in meaning.
- why: English instructions cost fewer tokens, but without the explicit Korean line the model answers in English. A choice that restates another with a synonym or an extra word passed the parser's duplicate removal because the letters differ, so only the prompt can stop it. Whether the English prompt really saved tokens was not measured.
- rejected: Korean instructions; relying on the parser alone to remove near-duplicate choices.
- evidence: `LiteRtLmQuizGenerator.prompt`; `QuizJsonParserTest` shows what the parser does and does not catch.

### QZ-D4 · 2026-09 · QZ-8, QZ-9
- decision: The parser takes the text between the first `[` and the last `]`, reads it leniently and salvages question by question. Duplicate removal first keeps the answer as text, removes duplicates, then looks the answer up again; a one-based answer number is corrected only when it equals the choice count; when cutting to four choices removes the answer's place the last slot goes to the answer.
- why: A two-billion-parameter model prefixes chatter and code fences, so strict decoding fails every time. The answer number refers to the list before removal, so removing first points the answer at a wrong choice, silently. Guessing an answer from an out-of-range number would grade wrongly; putting the answer always at slot 0 would let a reader pass without reading.
- rejected: Strict deserialisation; correcting any out-of-range answer number; always placing the kept answer at the first choice; removing duplicates before keeping the answer text.
- evidence: `core/ai` `QuizJsonParser`; `QuizJsonParserTest` (16 tests, checked by mutation of `distinctBy`).

### QZ-D5 · 2026-09 · QZ-10
- decision: Every generation goes through `sendCancellable`, which calls `cancelProcess()` when the coroutine is cancelled or times out; the 60-second timeout wraps the whole generation in the view model.
- why: `sendMessage()` is a blocking native call, so coroutine cancellation alone does not stop decoding; the engine mutex stayed held and the next quiz hung at the entry point.
- rejected: Relying on `withTimeoutOrNull` and coroutine cancellation alone.
- evidence: `core/ai` `ConversationExt.kt`; `CancellableTest` (`타임아웃도 cancelProcess 를 부른다`).

### D-55 · 2026-10 · ST-18
- decision: The worker takes the engine only with a non-waiting try-lock and gives up when it is busy, the app is in the foreground or a download runs; an engine it created is closed at the end if the app is in the background, while the quiz's engine is never closed by the worker.
- why: A user who taps quiz must win the engine; queueing the quiz behind the worker could hit its 60-second timeout; 2.6 GB must not linger in a background process.
- rejected: Waiting with the shared `use` call; always closing or never closing the engine.
- evidence: `core/ai/.../EngineSlot.kt`, `ModelEngine.tryUse`, `EngineSlotTest`.

### DEC-30 · 2026-10 · ai:engine
- decision: The LiteRT-LM dependency stays at 0.17.0.
- why: A bump to 0.18.0 was tried and reverted: on the emulator neither version brings the quiz engine up, so 0.18 cannot be verified; quizzes can only be confirmed on a real device.
- rejected: Upgrading to 0.18.0 without a device check.
- evidence: gradle/libs.versions.toml litertlm = 0.17.0; one lint GradleDependency warning remains. Still open until a real-device check.
