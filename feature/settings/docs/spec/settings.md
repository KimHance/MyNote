---
units: [settings:settings, settings:repo, settings:model]
plans: [001-reader-foundation, 002-quiz, 003-model-download, 005-stats, 006-sync, 007-repo-connect, 012-feature-modules]
decisions: []
---
## settings — Settings screen
### Behaviour
- Settings is one scrolling column, capped at the shared content width, with the sections in this order: reading, study log, quiz model, notes repository; the repository card has no section label (007-repo-connect: RC-K5).
- The column pads for the keyboard and the navigation bar, so a focused token field stays reachable.
- Reading holds one switch row "본문 글자 크게" ("노트 본문을 16에서 18로 키웁니다") and a "미리보기" sample sentence in body style; the switch sets the body text to 16 or 18 sp and the preview changes at once (001-reader-foundation: RD-19).
- The text size is read from the stored setting; a failing read shows normal (001-reader-foundation: RD-19).
- Each switch row is at least 68 dp high, toggles from anywhere on the row and is announced as a switch (001-reader-foundation: RD-23).
- Study log holds the switch "학습 기록 남기기" ("읽은 시간, 퀴즈 결과, 북마크를 이 기기에만 저장합니다"), on by default and shown on when the stored value cannot be read (005-stats: ST-5).
- Under the switch, "기록 지우기" opens a confirmation "학습 기록을 지울까요?" whose body names reading records, quiz results, bookmark history and weekly summaries and says it cannot be undone; "지우기" confirms, "취소" closes (005-stats: ST-6).
- On confirm the view model first raises the study-log-cleared signal, so a pending bookmark undo leaves records alone, then clears and shows "학습 기록을 지웠습니다" (005-stats: ST-6) (012-feature-modules: FM-6).
- The help line under the study log card says the statistics screen is built from these records and records older than 365 days are removed automatically (005-stats: ST-7).
- An open confirmation dialog survives rotation and folding.
- Snackbar notices are one-shot: each has an id, and once the shell reports it shown it does not appear again after recreation (012-feature-modules: FM-K11).
- An exception while handling an intent is logged through the injected failure log and the screen stays as it was (001-reader-foundation: RD-21).
- The view model reads the last sync result from the shared holder and never writes it.
### Public API
- `settingsDestinations(state, onIntent, onEnableNotifications)` — registers the `Settings` destination for the shell.
- `SettingsViewModel` — Hilt view model obtained with the Activity as owner; state `SettingsUiState`, input `SettingsIntent`.
- `SettingsUiState` — masked token and token flag, token-edit state, connection, last sync, disconnect dialog, text size, study-log flag, model status, notifications flag (filled by the shell) and the current notice; derives `connectedRepo`, `tokenEditing` and `repoCard`.
- `SettingsIntent` — text size, study-log switch and clear, model download actions, token edit actions, open connect, disconnect request, confirm and dismiss, and `SignalShown`.
### Guarantees
- The view model reads neither the clock, strings nor logging directly and runs in JVM tests; a failing disconnect is logged through the injected log (`SettingsViewModelTest`).
- Clearing the study log before a bookmark undo leaves the records alone (`FeatureSignalsTest` in `app`).
### Not covered
- Storing the text size and the study-log flag, and what clearing deletes or reschedules — `domain` and the data modules.
- The repository card and token editor — see `repo`; the model card and dialogs — see `model`.
- The top bar, back and the snackbar host — the `app` shell.

## repo — Repository card and token
### Behaviour
- The card is titled "노트 저장소" and shows one status line (dot, label, optional description) from the shared repository-card decision; when a token is stored and a repository connected the masked token follows the label and is read as "끝자리 XXXX" (007-repo-connect: RC-13).
- Without a connection the card shows only "저장소 연결", which opens the connect screen (007-repo-connect: RC-13).
- With a connection the card shows "owner/repo · 브랜치 b · 노트 위치 folder", or "· 기본 브랜치 ·" when no branch is stored; the folder is the list file's folder, "/" at the root (007-repo-connect: RC-13).
- The last-sync line reads "최근 불러오기 성공" or "최근 불러오기 실패 · <kind>" in the warning colour; there is no line until the first sync of this process ends (006-sync: SY-5).
- "저장소 바꾸기" pushes the connect screen; "연결 끊기" counts that connection's notes and bookmarks and then asks (007-repo-connect: RC-14, RC-17).
- The disconnect dialog "연결을 끊을까요?" says how many notes loaded from this repository and bookmarks and the stored token will be removed and that study records stay, or, with nothing to remove, that only the token goes; "끊기" is destructive (007-repo-connect: RC-17).
- A failed disconnect is logged and shows "연결을 끊지 못했습니다. 잠시 뒤 다시 시도하세요." (007-repo-connect: RC-17).
- The token block shows "액세스 토큰" with the masked token, or the card's no-token line; with a token it offers "바꾸기" (primary when the card asks for a token, otherwise secondary, read as "토큰 바꾸기") and "지우기"; without one "토큰 입력" (007-repo-connect: RC-18).
- The mask keeps the first 4 and last 4 characters around 8 bullets; 8 characters or fewer show only bullets; surrounding spaces are not part of the token; the bullet count never reveals the length (001-reader-foundation: RD-20) (007-repo-connect: RC-K3).
- The editor shows the token guidance, "토큰 만들기" (opens the token page in a browser, a snackbar when no browser exists), the token field (focused on open and scrolled into view when the keyboard appears) and "이 기기에만 저장됩니다." (007-repo-connect: RC-8).
- The editor shows a failure box for the last failed check, a warning for a classic `ghp_` token and the line "토큰 확인 중" while checking (007-repo-connect: RC-8, RC-18).
- The editor's buttons are "지우기" (when a token exists and no check runs), "취소" (always enabled) and "저장", which reads "확인 중" while checking and is enabled only with non-blank input (007-repo-connect: RC-18).
- Saving trims the input; blank input does nothing; the token is checked against the current connection's link (bare repository without a branch, the branch tree for the default list location, the list file otherwise) (007-repo-connect: RC-18).
- A passing check saves the token (the save is not cancelled once it starts), closes the editor, drops the input and shows "토큰을 저장했습니다"; the applied token change then starts a sync elsewhere (007-repo-connect: RC-18).
- A failing check keeps the editor open with the input and shows the failure; the stored token is untouched; no connection or an unexpected error reads as a transient failure (007-repo-connect: RC-18).
- Check failures map nine to seven: needs token and rejected → rejected; repository not visible → not visible; list inaccessible and no list → no list; bad list → bad list; rate limit → limit; offline → offline; transient → transient (007-repo-connect: RC-7, RC-18).
- While a check runs the keyboard closes and focus clears, "저장소 바꾸기" and "연결 끊기" are disabled and their intents blocked, and the lock is written to the shared signal so the notes side cannot open the connect screen (007-repo-connect: RC-18) (012-feature-modules: FM-6).
- "취소" or back while checking cancels only the request; the editor and its input stay (007-repo-connect: RC-18).
- Back while editing cancels the edit and drops the input instead of leaving settings (012-feature-modules: FM-10).
- Each opening of settings closes an open editor, cancels a running check and drops the input; a freshly created view model ignores openings from before it existed (012-feature-modules: FM-6).
- "지우기" clears the stored token at once with no confirmation and shows "토큰을 지웠습니다" with undo; undo restores the previous value, which is kept only in memory and dropped when a new token is saved (001-reader-foundation: RD-20).
- Clearing does nothing when no token is stored or while a check runs (007-repo-connect: RC-18).
- The raw token never enters the state: the draft lives in the shell's drafts holder, is cleared when the editor closes and is not restored after process death (012-feature-modules: FM-K7).
### Public API
- `TokenEditState` — editing, verifying, failure kind and undoable; `Viewing` is the closed state; `locksRepoActions` is true while verifying.
- `TokenEditEvent`, `TokenEditEffect`, `TokenEditTransition` and `reduceTokenEdit(state, event)` — the pure token-edit transition table.
- `maskToken(token)` — the masked form or null; `maskedTokenTail(token)` — the last 4 characters to read aloud, or null for short tokens.
- `connectionLinkOf(connection)` — the GitHub link a token check uses for a stored connection.
- `tokenFailureOfProbe(failure)` — the token failure kind for a connect-check failure.
- `DisconnectDialogUi` — note and bookmark counts for the disconnect dialog.
### Guarantees
- The transition table, including the lock while checking and cancel keeping the input, is proven without a screen (`TokenEditTest`).
- All nine check failures map to a token failure kind and the seven kinds have distinct texts (`ConnectIssueTest` in this module); the settings mapping and the connect screen's mapping stay the same (`ConnectIssueTest` in `app`).
- The mask hides length and short tokens (`TokenMaskTest`); the check link per connection shape (`ConnectConfirmTest`).
- Clear then undo restores the stored token (`SettingsViewModelTest`); back during an edit cancels the edit (`BackOrderTest` in `app`).
### Not covered
- Deciding the status label, description, badge and no-token line from (connection, token, last sync) — the shared repository-card decision in `ui` (a stored token on a public connection shows no warning there).
- The check itself, saving and applying the token, the disconnect order and resume — `domain` and the data modules.
- The connect screen opened by change — `connect`.
- Token encryption, expiry display and several repositories — not done.

## model — Quiz model card
### Behaviour
- The "퀴즈 모델" card shows "Gemma 4 E2B", a status dot, label and optional detail, a progress bar and the actions of the current model state; a help line sits under the card.
- The progress bar, shown in every state except missing, ready and failed, is either determinate with "x.xx / y.yy GB" and the percentage, or indeterminate.
- The notifications-off hint "알림이 꺼져 있어 앱을 나가면 진행 상황이 보이지 않습니다." with "켜기" shows only when the state asks for it (the five working states) and system notifications are off; "켜기" opens the system notification settings (003-model-download: MD-12).
- "받기", "이어 받기" and "다시 시도" start the download; "취소" cancels; "받던 파일 지우기" discards the piece; "삭제" asks first (003-model-download: MD-K14).
- The action buttons wrap to the next line instead of cutting a label (003-model-download: MD-13).
- Cancel shows "받기를 멈췄습니다. 나중에 이어 받을 수 있습니다" when the state is downloading with bytes received, and "받기를 취소했습니다" in every other state, even when a piece exists.
- Discarding shows "받던 파일을 지웠습니다"; starting shows nothing.
- The delete dialog "퀴즈 모델을 삭제할까요?" says it frees 2.6GB and the model must be downloaded again; on confirm the model is deleted and then "퀴즈 모델을 삭제했습니다" shows; the open dialog survives rotation.
- The download confirm dialog "퀴즈 모델을 받을까요?" says Gemma 4 E2B is 2.6GB, downloads only on Wi-Fi and the app stays usable meanwhile, with "나중에" and "받기"; the shell shows it when the reader-end card is tapped with no model (002-quiz: QZ-2).
- The download confirm and delete dialogs are glass cards at most 360 dp wide with a 26 dp radius; the study-log clear dialog of `settings` uses the same frame.
- A failing model-status stream reads as missing.
### Public API
- `ModelConfirmDialog(onDismiss, onConfirm, backdrop)` — the download confirm dialog, called by the `app` shell.
### Not covered
- Mapping a model state to tone, label, detail, progress, actions, help and the hint flag — the shared `modelCardUi` in `ui`.
- Downloading, verifying, notifications and the permission request — `ai` and the `app` root.
- The study-log clear dialog's behaviour — see `settings`.
