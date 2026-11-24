---
units: [ui:model-status, ui:repo-card, ui:token]
plans: [003-model-download, 006-sync, 007-repo-connect]
decisions: [SC-D5]
---
## model-status — Quiz model status models
### Behaviour
- Three pure functions turn a quiz model status into what is drawn: the settings card, the floating pill, and the reader-end call to action. The status is the domain's nine states: missing, waiting for Wi-Fi, preparing, downloading, retrying, verifying, paused, ready, failed.
- Card actions: missing offers `받기` only; each of the five working states offers `취소` only; paused offers `이어 받기` and the discard-partial action together; ready offers `삭제`; failed offers `다시 시도`.
- Card tone: ready is correct, failed is wrong, waiting and retrying are warn, preparing, downloading and verifying are accent, missing and paused are neutral.
- The progress bar is determinate for waiting (empty, warn), downloading, retrying and paused, indeterminate for preparing and verifying, and absent for missing, ready and failed. A determinate fraction is received over total clamped to 0..1 and is 0 when the total is not positive; the percent rounds down.
- Retrying says only that the connection broke and the download resumes; the card detail has no argument, the card and the pill show no seconds and promise no moment (003-model-download: MD-8).
- Paused shows the received size in decimal gigabytes with two decimals (`1.23`), formatted in the US locale so the separator is a dot.
- Failed gives the no-space text and its help line only for the `no-space` reason; every other reason, including a stored legacy `no-plan`, is the generic failure with `다시 시도` (003-model-download: MD-6).
- The notifications-off hint is on exactly for waiting, preparing, downloading, retrying and verifying, and off for missing, paused, ready and failed (003-model-download: MD-12).
- Help text: ready says the model runs only on the device, no-space says to free 2.6GB, everything else says it downloads on Wi-Fi only.
- The pill: waiting and retrying are warn (retrying carries the fraction, no percent); downloading is accent with the percent as secondary text and the fraction; ready is correct with a check icon; failed is wrong with a warning icon. Any other state uses the downloading label, accent, with a fraction only when paused (003-model-download: MD-8).
- The call to action: ready opens quiz setup and shines; missing asks to confirm a download with a `받기` chip; paused starts the download at once with an `이어 받기` chip and the received size; failed starts the download with a `다시 시도` chip in warn; the five working states show the progress text and send the user to settings, with no chip (003-model-download: MD-K9).
- A status counts as busy in the five working states.
- `ModelTone.color()` maps tones to theme colors (neutral text-3, accent, warn, wrong, correct); `ModelPreviewFrame` is a 360 dp preview wrapper with an optional 1.3 font scale, for previews only.
### Public API
- `modelCardUi(status)` returning `ModelCardUi`; `ModelAction`, `ModelTone`, `ModelProgressUi` (`None`, `Indeterminate`, `Determinate` with `fraction` and `percent`).
- `modelPillUi(status)` returning `ModelPillUi`; `PillIcon`.
- `quizCtaUi(status)` returning `QuizCtaUi`; `CtaAction` (`OpenSetup`, `ConfirmDownload`, `OpenSettings`, `StartDownload`).
- `ModelStatus.fractionOrNull()` · `ModelStatus.isBusy` · `ModelStatus.Failed.isNoSpace` · `gigabytes(bytes)`.
- `ModelTone.color()` and `ModelPreviewFrame(largeFont, ...)` in the screen package.
### Guarantees
- Card actions, help text, retry wording, the notification hint, legacy failure, pill shape, busy states, gigabyte format and each call-to-action branch are fixed by `ModelStatusUiTest` and `QuizCtaUiTest` (the second class lives in the same file).
### Not covered
- Downloading and verifying — `ai:download-transfer`; notifications — `ai:download-work`; the status itself — `domain:quiz-model`.
- Drawing the card and its two-line button wrap — `settings:model`; drawing the pill — `app:shell-chrome` and `design-system:glass-nav`; drawing the reader-end card — `quiz:cta`.
- The `최대 N문제` subtitle argument and the percent argument are supplied by the drawing screens.

## repo-card — Repository card state
### Behaviour
- `repoCardUi(connection, hasToken, last)` decides the settings repository card. No connection is state `None`: muted dot, `연결 없음` with the line `노트 저장소를 연결하세요.`, no badge (007-repo-connect: RC-13).
- With a connection, "token needs check" wins when the last sync was a token rejection, or the connection is private and the last sync was access denied, or the connection is private and holds no token. It shows a warn dot, the badge, and `토큰 확인 필요` (007-repo-connect: RC-13).
- A public connection is never "needs check" because of access denied or a missing token; only a token rejection makes it so (007-repo-connect: RC-13).
- Otherwise, with no sync result yet since app start the state is `불러오기 전` (muted dot); with a token it is `토큰 사용 중`; without a token it is `공개 저장소` with `토큰 없이 읽습니다` (007-repo-connect: RC-13).
- When no token is stored the card carries a token line: `토큰 없음 · 공개 저장소는 토큰 없이 읽습니다` for a public connection, and the private variant naming the need. With a token, or without a connection, there is no such line.
- The last sync result is none, succeeded, or failed with one of six kinds: no internet, token rejected, not accessible, rate limit, server, bad list format, each with its own label. An unknown exception is the server kind; a Result converts with `lastSyncOf` (006-sync: SY-5).
### Public API
- `repoCardUi(connection, hasToken, last)` returning `RepoCardUi`; `RepoCardState`, `RepoDot`.
- `LastSync` (`None`, `Succeeded`, `Failed(kind)`), `SyncFailureKind`, `lastSyncOf(result)`, `syncFailureKindRes(kind)`.
### Guarantees
- The state mapping, badge rule, token line and the six distinct kinds are fixed by `RepoCardTest` (007-repo-connect: RC-13) (006-sync: SY-5).
### Not covered
- Drawing the card, `바꾸기` and `끊기` — `settings:repo`.
- Holding the last result across screens — `ui:signal` (`LastSyncHolder`).
- Failure kinds of the connect flow — `connect:issues`.

## token — Token draft and failures
### Behaviour
- `TokenDraft` holds the typed token text and whether it is revealed. It is empty and hidden at the start; `clear()` resets both; blank means only whitespace.
- A token starting with `ghp_` after trimming is classic and the screen warns that its scope is wide (007-repo-connect: RC-8).
- `secret()` hands the text out as `SecretText`; the draft's and the secret's string forms never contain the token, so logs and error messages cannot leak it (007-repo-connect: RC-8).
- Drafts live in memory only: they survive rotation and folding because the shell holds them, and are not restored after process death (007-repo-connect: RC-24).
- `NotesDrafts` holds two drafts, one for the connect screen and one for the settings token editor.
- `consumeReset(count)` clears the connect draft once per new reset count: a count equal to the last handled one does nothing (so the starting count 0 does nothing at first), and any different count clears again. A token typed after a clear is not wiped by a repeated call (007-repo-connect: RC-24).
- Token failure kinds are rejected, not visible, no list, limit, offline, transient and bad list, each mapped to one `ui_*` sentence; a token reason is either needed or limit.
### Public API
- `TokenDraft` — `text`, `revealed`, `isBlank`, `isClassic`, `secret()`, `clear()`.
- `SecretText` — `value`; `NotesDrafts` — `connectToken`, `settingsToken`, `consumeReset(count)`.
- `TokenFailureKind`, `TokenReason`, `tokenFailureRes(kind)`.
### Guarantees
- No token in string forms, classic warning and clearing are fixed by `TokenDraftTest`; the once-per-reset rule by `NotesDraftsTest`.
### Not covered
- Drawing the field and guidance — `ui:parts`; deciding failures — `connect:issues` and `settings:repo`.
- Storing a token — `data:records` and `database:prefs`; masking a saved token — `settings:repo`.
