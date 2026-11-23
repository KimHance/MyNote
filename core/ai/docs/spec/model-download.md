---
units: [ai:download, ai:download-decision, ai:download-transfer, ai:download-work]
plans: [003-model-download]
decisions: [MD-D1, MD-D4, MD-D5, MD-D6, MD-D8, MD-D13, MD-D16, MD-D18, MD-D19, BR-D9, DEC-22, DEC-23, DEC-24a, DEC-36d]
---
## download — Model download
### Behaviour
- The quiz model is one file, `gemma-4-E2B-it.litertlm`, 2,588,147,712 bytes, fetched from a Hugging Face URL pinned to one commit of the litert-community model. Its size and sha256 are built into the app and are the only reference of the final check; a wrong value would make the check delete the user's installed file (003-model-download: MD-2).
- The installed file goes in the app's external files folder, or the internal files folder when there is none. A half-downloaded file has another name (`.part`) and never has the final name (003-model-download: MD-K3).
- Status is derived, not stored, from the download work's state and the files on disk (003-model-download: MD-K7).
- Status: no work and the file in place is Ready; no work and a piece left is Paused (received, total); neither is Missing. Ready means only that a file with the final name exists; it is not hashed again when the status is read.
- A queued work that never ran is waiting for Wi-Fi; a queued work that already ran (run attempt above 0) is Retrying with the piece's size; a running work is Preparing until it reports progress, then Downloading, then Verifying while the verify phase is set (003-model-download: MD-K7).
- A succeeded or cancelled work reads as the resting state of the files. A failed work with the file in place reads as Ready; otherwise Failed with its reason, or `unknown` when none was stored (003-model-download: MD-6, MD-K7).
- Total size is the progress total when positive, else the built-in size; no plan file is read (003-model-download: MD-4).
- `start` clears the result notification and queues the download; `cancel` stops it and clears every notification, leaving the piece (the status becomes Paused) (003-model-download: MD-10).
- `discardPartial` deletes the piece and any plan file and refreshes the status.
- `deleteModel` first cancels the statistics work, releases the engine, deletes the installed file, clears all notifications and refreshes the status (003-model-download: MD-10).
- The status flow follows the first unfinished work, else the latest one, and emits only on change.
- When the app goes to the background, `notifyResting` posts the notification for a resting state (waiting, retrying). The app coordinator calls `settleNotices` to remove notifications that no longer match a status (003-model-download: MD-11).
- No retry cap here; the notification permission is asked by the screen that starts the download, not by this unit.
### Public API
- `ModelDownloads` — the `QuizModelSource` implementation: `status()`, `start()`, `cancel()`, `discardPartial()`, `deleteModel()`; plus `settleNotices(status)` and `notifyResting()` for the app's coordinator.
### Guarantees
- Mapping from work and file facts to a status is fixed by `DownloadFactsTest` (in `ai:download-decision`). There is no unit test of `ModelDownloads` itself.
### Not covered
- The model card, the quiz card's download chip and the permission request — `settings`, `ui`.
- Fetching and verifying — `ai:download-transfer`; the worker — `ai:download-work`.

## download-decision — Download decisions
### Behaviour
- Pure rules, no Android calls, so they run in plain JVM tests.
- Next step from the local state: an installed file of exactly the expected size is verified first, before anything else; a piece of exactly the expected size is verified; a piece between 1 byte and the full size is resumed from its length; anything else (none, or a piece longer than the total) starts from zero (003-model-download: MD-4).
- A request asks for no compression and, when resuming, a `Range` from the held length; a start from zero sends no `Range` (003-model-download: MD-K1).
- Verdict on a response: 206 whose `Content-Range` starts where asked appends; 206 elsewhere or without the header starts over; 200 to a from-zero request appends from zero; 200 to a resume request starts over (appends nothing); 416 completes only when the reported total equals the requested position, otherwise starts over (003-model-download: MD-K1, MD-K2).
- 408, 429, 500, 502, 503 and 504 are retried later; the server's `Retry-After` is never read. Every other status, including 403 and 404, gives up as `HTTP <code>` (003-model-download: MD-7).
- Space is enough when the allocatable bytes cover the total minus the piece already held (003-model-download: MD-3).
- Percent rounds down, is 0 when the total is unknown and never passes 100; gigabytes are decimal (1,000,000,000 bytes) with two decimals (003-model-download: MD-K8).
- Failure reasons are `no-space`, `no-progress`, `too-many-rounds`, `HTTP n` and `unknown`; `no-plan` is not produced (003-model-download: MD-6).
- Retrying carries no remaining time (003-model-download: MD-8).
### Public API
- None — every declaration is internal; callers reach the rules through `ModelDownloads` and the worker.
### Guarantees
- `DownloadFactsTest`, `DownloadStepTest`, `ResponseVerdictTest`, `StorageSpaceTest`, `DownloadProgressTest`, `HubHeadersTest` and `ExpectedModelTest` (64 lowercase hex characters, 2,588,147,712 bytes) fix the rules above.
### Not covered
- Doing the requests or touching files — `ai:download-transfer`.

## download-transfer — Download transfer
### Behaviour
- An install starts by checking space against the built-in size minus the piece held; too little fails as `no-space` before any request (003-model-download: MD-3).
- It then loops up to 8 rounds over the next step; after 8 rounds without finishing it fails as `too-many-rounds` (003-model-download: MD-2).
- The first request is the body itself: nothing asks the remote first, no hub-only header is relied on (003-model-download: MD-1).
- A leftover piece resumes from where it stopped; a plan file next to it is never read and is deleted when the piece is discarded or the install ends (003-model-download: MD-4).
- Bytes are appended in 64 KB chunks; progress is the whole-file count so far. A cancel closes the call and leaves the piece as it is (003-model-download: MD-K1).
- A dropped connection, even mid-body, keeps the bytes already written and ends the round as retry-later (003-model-download: MD-7).
- A round that adds no byte fails as `no-progress`, so a download that receives nothing cannot loop (003-model-download: MD-K4).
- A start-over verdict discards the piece and the next round begins from zero (003-model-download: MD-K1).
- Before hashing, the verifying step is reported. The file is accepted only when size and sha256 both match; a mismatching piece is deleted and the download restarts, a mismatching installed file is deleted the same way (003-model-download: MD-2, MD-K5).
- Hashing checks for cancellation every 64 KB chunk; cancelling during verification leaves the piece and puts nothing in place.
- A verified piece is moved into place by one atomic move, replacing any file there, so a half file never has the final name. A piece from another model commit never survives: the final check catches it and the download restarts (003-model-download: MD-5, MD-K3).
- An existing file of the right size is verified without any request (003-model-download: MD-2).
- Across a redirect to another host the `Range` header is kept and `Authorization` is dropped, even for another port on the same host (003-model-download: MD-K6).
### Public API
- None — internal; the worker is the only caller.
### Guarantees
- `ModelInstallerTest` (one GET and no HEAD, resume, space, mismatch, 8 rounds, 429, cancel during verification), `ModelDownloaderTest` (Range, 200 and 206 handling, mid-body drop), `ModelFilesTest` (names, promote, hash) and `OkHttpRedirectProbeTest` (redirect headers, a probe of the HTTP library, not of app code).
### Not covered
- Scheduling, constraints, notifications — `ai:download-work`.
- Network type and storage constraints — set when the work is queued, in `ai:download-work`.

## download-work — Download work and notices
### Behaviour
- The download is one unique work named `quiz-model-download`, queued with keep-existing: starting, resuming and retrying are the same call and never stack a second one (003-model-download: MD-K10).
- It needs an unmetered network and storage not low, is expedited with a fallback to ordinary work, runs as a foreground data-sync service and backs off exponentially from 30 seconds (003-model-download: MD-K10, MD-7).
- A restart where the foreground service cannot start (app in the background after a Wi-Fi return, a retry backoff, or the data-sync time running out) continues as ordinary work; a cancellation is still rethrown (003-model-download: MD-K10).
- Progress is published at most every 200 ms (later reports inside the window are dropped); the notification updates only when the whole percent changes.
- Installed ends with success and a ready notification; a retry verdict ends with a retry and the retry notification (the work scheduler retries with its backoff); a failure ends with a failure carrying the reason (003-model-download: MD-7, MD-6).
- The download runs with notifications turned off by the user; the notifications are only a view (003-model-download: MD-12).
- At most one notification is up, chosen by state in three slots: progress (4201), result (4202), retry (4203). Raising one removes the other two (003-model-download: MD-9).
- State to notification: waiting for Wi-Fi, preparing, downloading and verifying are progress notices with a cancel action, except verifying which has none; downloading shows percent and gigabytes with a determinate bar; others show a moving bar; retrying is a retry-slot notice with a still icon, a bar at the received share and a cancel action and text that promises no time; ready is a dismissible result; failed is a result with an expanded body and a retry action (003-model-download: MD-8, MD-9).
- Failed with `no-space` says to free 2.6GB; every other reason, including a stored `no-plan`, uses the generic "try again later" text (003-model-download: MD-6).
- A tap on download removes the old result notification; when the worker starts it removes result and retry notifications; cancel and model delete remove all; success leaves one result; failure leaves one; a stop before the foreground notice leaves no preparing notice (003-model-download: MD-10).
- When the work is stopped for any reason other than the app's own cancel (network lost, system), the retry notification stays; an app cancel leaves nothing (003-model-download: MD-11).
- Waiting and retry notifications are posted when the app goes to the background and stay when it comes forward; the app removes notifications only when the status says there is nothing to show (missing or paused) (003-model-download: MD-9, MD-11).
- Notifications use the low-importance channel `quiz_model_download`; working ones are ongoing and silent, results are not ongoing and close on tap. A tap opens settings when the app supplied that intent, else the launcher; the cancel action needs the work's cancel intent and the retry action the app's retry intent, and an action is left out when its intent is absent (003-model-download: MD-K11).
- The retry notification's small icon is a still Material Icons `downloading` (Apache-2.0); the platform download icon, which animates, is used for waiting and downloading (003-model-download: MD-9).
### Public API
- `ModelDownloadIntents` — the app supplies `retry()` and `openSettings()` pending intents for the notification; optional (the module works without it).
### Guarantees
- State to notification, one at a time, no leftover notifications and the stop reasons are fixed by `DownloadNoticesTest`; failure wording by `DownloadNotificationsTest`. The worker and the notification builder are not unit-tested; they are checked on a device.
### Not covered
- Deciding what to download and verifying it — `ai:download-transfer`.
- The pill, the model card and their wording — `settings`, `ui`.
- The notification permission request — the screen that starts a download.
