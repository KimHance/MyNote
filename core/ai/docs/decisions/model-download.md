# Decisions

### MD-D1 · 2026-10 · MD-1, MD-2, MD-4
- decision: The download does not ask the remote for size or hash first; it requests the body at once and checks the result against the size and sha256 built into the app. The plan file and remote change detection are removed.
- why: The download address is pinned to one commit, so the remote file never changes; the asking step only added a failure reason (`no-plan`) and a dependency on hub-only headers.
- rejected: A HEAD request before every download that builds a plan file from `x-linked-size`, `x-linked-etag` and `x-repo-commit` and fails as a whole when one header is missing.
- evidence: `core/ai/.../download/ModelSource.kt` (constants), `ModelInstaller.kt`, `ModelInstallerTest` (`MD-1 받기는 HEAD 없이 본문 요청부터 한다`, `MD-1 허브 헤더가 없어도 받는다`), `ExpectedModelTest`.

### MD-D4 · 2026-10 · MD-9, MD-10
- decision: Download notifications are decided by the state and only one is shown at a time; raising one clears the other slots.
- why: Progress, result and retry notifications plus the app's own waiting notification did not clear each other, so cancelled, retried or succeeded downloads left stale ones behind.
- rejected: Keeping the three independent notification branches and the separate waiting notification.
- evidence: `core/ai/.../download/work/DownloadNotices.kt`, `DownloadNoticesTest` (`MD-9 상태마다 알림이 하나로 정해진다`, `MD-9 하나를 올리면 다른 자리는 비운다`).

### MD-D5 · 2026-10 · MD-K1, MD-K2
- decision: Resumable download (piece plus `Range`), final sha256 check, Wi-Fi condition, progress notification with cancel, space check, unique work with KEEP, exponential backoff and the 416 branch all stay; the 416 branch is not touched.
- why: Owner decision: resuming is kept and the 416 branch is left as it is.
- rejected: Removing resume and the 416 handling as part of the slimming.
- evidence: `ModelDownloader.kt`, `ResponseVerdict.kt`, `ResponseVerdictTest`, `ModelDownloaderTest`.

### MD-D6 · 2026-10 · MD-K1
- decision: The download is one stream, not parallel segments; there never were segments in the code.
- why: A measurement showed one stream was faster than four in parallel.
- rejected: Parallel segment download.
- evidence: `ModelDownloader.kt` (single request loop), `ModelDownloaderTest`.

### MD-D8 · 2026-10 · MD-5
- decision: When the model commit is raised later, an old piece is left to the final check; the piece name does not carry the commit.
- why: There is one commit today and the work that raises it decides; adding code now would go against the slimming.
- rejected: Putting the commit into the piece file name so that pieces of other commits are never reused.
- evidence: `ModelFiles.kt`, `ModelInstallerTest` (`MD-5 다른 파일의 조각은 섞이지 않고 처음부터 다시 받는다`).

### MD-D13 · 2026-10 · MD-8, MD-9
- decision: Retry wording promises no moment or trigger (no soon, no when connected) and shows no seconds.
- why: Retrying also covers waiting out a backoff after a server error with Wi-Fi connected, where "when connected" is false; the seconds were computed once and never counted down.
- rejected: Phrases such as "soon" and "when connected", and a seconds countdown.
- evidence: `DownloadFactsTest` (`MD-8 재시도 상태는 남은 시간 없이 받은 만큼만 안다`), `ModelStatusUiTest` (`MD-8 재시도 카드는 초 없이 이어 받는다고 말한다`).

### MD-D16 · 2026-10 · MD-9
- decision: The title of progress-type notifications does not claim that a download is running (`퀴즈 모델 받기`).
- why: A "downloading" title is false while resting and contradicts the body of the waiting and retry notifications.
- rejected: A title saying the model is being downloaded.
- evidence: string `ai_model_download_title` in `core/ai`, `DownloadNoticesTest`.

### MD-D18 · 2026-10 · MD-10
- decision: A work start clears only result and retry notifications; the progress notification appears only after the work became foreground, and the app never posts a progress-slot notification itself.
- why: A preparing notification posted by the app before the work went foreground was never removed when the work stopped or failed first.
- rejected: The app posting a preparing notification into the progress slot before the work starts.
- evidence: `ModelDownloadWorker.kt`, `DownloadNoticesTest` (`MD-10 포그라운드가 되기 전에 멈추거나 실패해도 준비 알림이 남지 않는다`).

### MD-D19 · 2026-10 · MD-10
- decision: The reconciler clears all remaining notifications whenever the state is none or paused, so a failure notification disappears when the work record is pruned (default one day).
- why: In those states the notification table says none; leaving one would contradict the card (paused or resumable).
- rejected: Keeping a day-old failure notification until the user dismisses it.
- evidence: `DownloadNotices.kt`, `DownloadNoticesTest` (`MD-10 쉬는 중 취소해도 알림이 남지 않는다`).

### BR-D9 · 2026-10 · ai:download-work, BR-K8
- decision: The model download notification keeps its system small icon; it is not changed to the brand mark.
- why: The moving icon while downloading or waiting for Wi-Fi tells the state (see the model-download decisions).
- rejected: Using the brand mark as the notification's small icon.
- evidence: `core/ai/src/main/java/com/hancekim/mynote/core/ai/download/work/DownloadNotifications.kt`; `core/ai/src/test/java/com/hancekim/mynote/core/ai/download/work/DownloadNoticesTest.kt` (`MD-9 상태마다 알림이 하나로 정해진다`)

### DEC-22 · 2026-10 · MD-1, MD-2
- decision: Model commit, byte size and sha256 are three constants in ModelSource; the downloader asks the hub for nothing before the body request. The installer takes the expected values as arguments and the final verification deletes an installed file that does not match.
- why: The download URL is pinned to a commit, so the remote file cannot change; the old HEAD preflight read hub-specific headers into a .plan, and a renamed header failed the whole download as "no-plan".
- rejected: A HEAD request before downloading to learn size and hash.
- evidence: ModelSource.kt (COMMIT, TOTAL_BYTES, SHA256) in core/ai download; the values were checked against the hub and a local shasum, and a pinned-values test guards their shape.

### DEC-23 · 2026-10 · MD-9, MD-10
- decision: A pure function (DownloadNotices) decides the notification row for each state or event (slot, text, bar, action, small icon) and posting one clears the other slots. Because cancelling from a resting-state notification action runs no app code, an app-side coordinator (ModelWaitNotice) watches the status and clears leftover slots when the state is none or stopped; no new manifest component is added.
- why: core/ai has no Robolectric, so the notification decision had to sit outside NotificationManager to be unit-tested; stale notifications after action-cancel, failed retry and system stop came from the previous structure.
- rejected: Deciding inside the notification-posting code; a new receiver or service to clear leftovers.
- evidence: core/ai download/work/DownloadNotices.kt with DownloadNoticesTest; app download/ModelWaitNotice.kt referenced from MyNoteApplication.

### DEC-24a · 2026-10 · MD-5
- decision: Partial download files are named by model file only, without the commit. After a model commit bump, an old-commit partial is left to the final verification: a mixed file never reaches the install location and the download restarts.
- why: Putting the commit in the piece name adds a rule for little gain; the end check already makes the outcome safe.
- rejected: Encoding the commit in the partial file name.
- evidence: ModelFiles(dir, ModelSource.FILE_NAME) in ModelDownloads.kt; backup rules exclude the .part file.

### DEC-36d · 2026-10 · MD-7
- decision: If the worker cannot be promoted to a foreground service, the run continues as an ordinary background job instead of failing. A user-initiated data transfer job (Android 14+) is deferred.
- why: A failed foreground start used to fail the whole download.
- rejected: Ending the download as failed when foreground promotion fails.
- evidence: ModelDownloadWorker catches the IllegalStateException around setForeground and logs it.
