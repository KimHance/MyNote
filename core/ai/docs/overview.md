---
id: ai
name: On-device AI
aliases: [AI 모듈, 온디바이스 LLM, LiteRT-LM, 퀴즈 엔진, 모델 내려받기, 통계 워커]
uses: [common, domain]
units:
  - {id: engine, name: Model engine, aliases: [엔진, ModelEngine, 취소 가능한 생성], code: [src/main/java/com/hancekim/mynote/core/ai/ConversationExt.kt, src/main/java/com/hancekim/mynote/core/ai/EngineSlot.kt, src/main/java/com/hancekim/mynote/core/ai/ModelEngine.kt, src/test/java/com/hancekim/mynote/core/ai/EngineSlotTest.kt, src/test/java/com/hancekim/mynote/core/ai/CancellableTest.kt]}
  - {id: quiz, name: Quiz generation, aliases: [퀴즈 생성, 퀴즈 파서, 4지선다], code: [src/main/java/com/hancekim/mynote/core/ai/LiteRtLmQuizGenerator.kt, src/main/java/com/hancekim/mynote/core/ai/QuizJsonParser.kt, src/test/java/com/hancekim/mynote/core/ai/QuizJsonParserTest.kt]}
  - {id: insight-generation, name: Insight generation, aliases: [요약 생성, 패턴 생성, 가드, 통계 문장], code: [src/main/java/com/hancekim/mynote/core/ai/insight/InsightGenerator.kt, src/main/java/com/hancekim/mynote/core/ai/insight/InsightGuards.kt, src/main/java/com/hancekim/mynote/core/ai/insight/InsightJsonParser.kt, src/main/java/com/hancekim/mynote/core/ai/insight/InsightPrompts.kt, src/test/java/com/hancekim/mynote/core/ai/insight/]}
  - {id: insight-worker, name: Insight worker, aliases: [통계 워커, 주간 요약 워커, 예약], code: [src/main/java/com/hancekim/mynote/core/ai/insight/StudyInsightWorker.kt, src/main/java/com/hancekim/mynote/core/ai/insight/StudyInsightWork.kt, src/main/java/com/hancekim/mynote/core/ai/insight/AppVisibility.kt, src/main/java/com/hancekim/mynote/core/ai/insight/InsightWork.kt]}
  - {id: download, name: Model download, aliases: [모델 내려받기, 모델 상태, QuizModelSource, 모델 삭제], code: [src/main/java/com/hancekim/mynote/core/ai/download/ModelDownloads.kt, src/main/java/com/hancekim/mynote/core/ai/download/ModelSource.kt, src/main/java/com/hancekim/mynote/core/ai/download/DownloadWork.kt]}
  - {id: download-decision, name: Download decisions, aliases: [받기 판단, 상태 판정, 응답 판정], code: [src/main/java/com/hancekim/mynote/core/ai/download/decision/, src/test/java/com/hancekim/mynote/core/ai/download/decision/]}
  - {id: download-transfer, name: Download transfer, aliases: [받기 전송, 이어받기, 해시 검증, 설치], code: [src/main/java/com/hancekim/mynote/core/ai/download/transfer/, src/test/java/com/hancekim/mynote/core/ai/download/transfer/]}
  - {id: download-work, name: Download work and notices, aliases: [내려받기 알림, 내려받기 작업, 포그라운드 알림], code: [src/main/java/com/hancekim/mynote/core/ai/download/work/, src/test/java/com/hancekim/mynote/core/ai/download/work/, src/main/res/]}
  - {id: wiring, name: Hilt wiring, aliases: [AiModule, 바인딩], code: [src/main/java/com/hancekim/mynote/core/ai/di/]}
---
## Overview
`ai` runs the language model on the device and downloads it. It owns the one LiteRT-LM engine that the quiz and the statistics worker share, quiz generation and the repair of the model's answer, the daily statistics worker with the writing and guarding of weekly summaries and wrong-answer patterns, and the model download (decision, transfer, work, notifications).
It implements contracts that `domain` owns (`QuizGenerator`, `QuizModelSource`, `InsightScheduler`) and has no screen. It does not cut the note text (the quiz use case in `domain` does), compute statistics or decide what the worker should write (`domain` stats), store results (`database`), or show the model card and quiz screens (`settings`, `quiz`, `ui`). Quiz timeouts and the quiz screen's failure kinds are the caller's.
## Structure
- `engine` holds the single engine and the only way to use it; `quiz` and `insight-generation` borrow it through `engine.use` (quiz) or through the worker's `tryUse`, and send every generation through the cancellable call.
- `quiz` turns a note into questions and repairs the reply; `insight-generation` turns a digest or wrong answers into one JSON object and rejects bad output before any screen sees it.
- `insight-worker` plans, calls `insight-generation`, saves through the `domain` repositories and is scheduled by `StudyInsightWork`; it also reads the download work by name to stay out of its way.
- `download` composes the model status for `domain` from the work state and the files; `download-decision` holds its pure rules, `download-transfer` fetches and verifies the file, `download-work` runs it in a foreground worker and keeps one notification.
- `wiring` binds the three contracts and the download HTTP client; `:app` puts the module in the graph and supplies the notification tap intents.
- Outside facts it depends on: the two native-library lines in the `:app` manifest decide GPU use; `:app` instrumented evaluation tests drive the real model.
```mermaid
flowchart LR
  quiz --> engine
  insight-worker --> insight-generation --> engine
  insight-worker --> engine
  download --> download-decision
  download-work --> download-transfer --> download-decision
  download-work --> download-decision
  download --> engine
  download --> download-work
  wiring --> quiz
  wiring --> download
  wiring --> insight-worker
```
