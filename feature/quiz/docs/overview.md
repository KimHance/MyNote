---
id: quiz
name: Quiz
aliases: [퀴즈, 퀴즈 오버레이, 퀴즈 풀기, quiz overlay]
uses: [common, design-system, domain, navigation, ui]
units:
  - {id: session, name: Quiz session, aliases: [퀴즈 진행, 채점, 자동 진행, 그만두기 되돌리기, 퀴즈 기록, QuizViewModel], code: [src/]}
  - {id: overlay, name: Quiz overlay, aliases: [퀴즈 화면, 퀴즈 만들기 대화상자, 생성 중 화면, 풀기 화면, 결과 화면, 실패 화면], code: [src/main/java/com/hancekim/mynote/feature/quiz/QuizOverlay.kt, src/main/java/com/hancekim/mynote/feature/quiz/screen/, src/main/java/com/hancekim/mynote/feature/quiz/component/]}
  - {id: cta, name: Reader-end quiz card, aliases: [퀴즈 카드, 이 노트로 퀴즈 풀기, 리더 끝 CTA, quiz call-to-action], code: [src/main/java/com/hancekim/mynote/feature/quiz/component/QuizCtaCard.kt]}
---
## Overview
Owns the quiz a reader takes at the end of a note: the card that starts it, the setup dialog, the generating, solving, result and failure screens that cover the reader, and the view model that holds the quiz in memory, grades answers, runs the auto-advance timer and the quit undo, and records a finished attempt for statistics.
The quiz is an overlay, not a back-stack destination: it has no destination key and no registration file.
It does not generate questions (the on-device model and prompt live in `ai`, the use case in `domain`), does not download the model (`ai`, the settings model card in `settings`), and does not decide where a card tap leads, where the overlay sits in the back order, or which notes are readable (`app` shell and `notes`).
## Structure
- `session` is `QuizViewModel` with its state (`QuizOverlayState`, `QuizUiState`) and intents; it is obtained with the Activity as owner by the `app` shell and talks to other features only through `ui` signals (setup request, close, quiz covers reader).
- `overlay` draws whatever `session` holds over the reader: the setup dialog over a scrim, or one full-cover screen per state; every button sends a `QuizIntent` back to `session`.
- `cta` is the card at the end of the reader; the `app` shell puts it into the notes reader-end slot and routes its action (setup through `session`, download through `settings`, or a confirm dialog).
```mermaid
flowchart LR
  cta -- action --> app[app shell]
  app -- OpenSetup --> session
  overlay -- intents --> session
  session -- state --> overlay
  session -- covers reader / attempt --> ui[ui signals and domain]
```
