# Model download

Source: old feature document `docs/spec/model-download.md` as of commit c8c54350 (download built 2026-09-27, slimmed 2026-10-07) and the owner's earlier planning notes (untracked).

## Plan
- why: The 2.6GB quiz model is not in the APK, so the app downloads it itself; the first version asked the remote before every download, kept a plan file and left stale notifications behind, and the slimming removed that.
- flow: The user taps download (note-end card or settings card), the app waits for Wi-Fi, downloads the body, resumes after interruptions, checks size and sha256 and then the model is usable; cancel or failure leaves exactly one matching notification.
- not doing: No parallel segments, no change to the 416 branch, no replacement of an already installed old model when the model commit changes, no change of notification channel or importance, no retry cap, no seconds shown while retrying.

## Items
| ID | What | Accept |
|---|---|---|
| MD-1 | The download asks the remote for nothing first and requests the body straight away; it does not rely on hub-only headers | `ModelInstallerTest`: one install records only GET requests (HEAD 0) and finishes with none of the three hub headers present |
| MD-2 | The final check uses the size and sha256 built into the app; both must match before the file takes its place, else the piece is dropped and the download restarts, at most 8 rounds then `too-many-rounds`; an existing file is checked the same way | `ModelInstallerTest` (match, mismatch, 8 rounds) and `ExpectedModelTest` (64-char lowercase hex, 2,588,147,712 bytes) |
| MD-3 | The space check runs before the first request with the built-in size minus the piece already held; too little space fails with `no-space` and sends no request | `ModelInstallerTest`: zero requests when short; a 10-byte piece lowers the need by 10 |
| MD-4 | A piece left by the earlier asking version resumes from where it stopped; the old plan file is never read and is gone after install or discarding the piece; total size is progress, else the built-in size | `ModelInstallerTest`, `ModelFilesTest`, `DownloadFactsTest`: first body request has `Range: bytes=10-`, no plan file afterwards |
| MD-5 | A piece from a different model commit never ends up in the installed file; the final check catches it and the download restarts | `ModelInstallerTest`: foreign 5-byte piece, installed file equals the expected body after 2 body requests |
| MD-6 | Failure reasons are `no-space`, `no-progress`, `too-many-rounds`, `HTTP n` and `unknown`; the old `no-plan` is gone and a stored one shows as a plain failure with `다시 시도` | `ModelStatusUiTest` and `DownloadNotificationsTest`: `Failed("no-plan")` gives the generic card and notification body |
| MD-7 | 408, 429, 500, 502, 503, 504 and connection errors go to retry; the server's `Retry-After` is not read; the interval is the work scheduler's exponential backoff from 30 seconds; every other status, 404 and 403 among them, fails as `HTTP n` | `ModelInstallerTest`: 429 with and without `Retry-After` give the same retry |
| MD-8 | The retrying state has no remaining time; card, pill and notification show no seconds and promise no moment or trigger, only: connection broke, resuming from what was received, received bar, `취소` | `DownloadFactsTest` and `ModelStatusUiTest`: retrying card text has no argument; no pill seconds string |
| MD-9 | The notification shown is decided by the state and there is one at a time (waiting, preparing, downloading, retrying, verifying, ready, failed); the retry notification has a still icon, titles never claim downloading, waiting and retry notifications stay when the app comes forward | `DownloadNoticesTest`: state to notification mapping for all nine states, raising one clears the other slots |
| MD-10 | No notification is left behind when the state changes: new start clears results, cancel clears all progress notifications, success leaves one result, failure leaves one, model delete clears all, a work start before foreground leaves no preparing notification | `DownloadNoticesTest` (the `MD-10` cases: retry clears the failure, cancel while resting or downloading, retry then success leaves one, delete after success, a work start before foreground) all end with the expected count |
| MD-11 | While resting the matching notification appears: when the app goes to the background in waiting or retrying, when a connection error schedules a retry, and when downloading stops for a reason other than user cancel | `DownloadNoticesTest`: a stop that is not the user's leaves the retry notification; background in waiting shows the waiting notification |
| MD-12 | The download runs with notifications off; the card shows the notifications-off hint with `켜기` only in the five working states | `ModelStatusUiTest`: hint true for waiting, preparing, downloading, retrying, verifying and false for paused, none, ready, failed |
| MD-13 | The two buttons of the paused card wrap to the next line when they do not fit, and no label is cut | Compose preview of the paused card at 360dp and text scale 1.0 and 1.3 shows both labels whole (manual; no automated test) |
| MD-K1 | Resuming uses `Range` from the held piece and appends; a 200 answer to a resume request appends nothing | `ModelDownloaderTest`: resume, Range header, 200 writes no byte |
| MD-K2 | The 206, 200 and 416 verdicts hold (416 ends only when the reported length equals the requested position) | `ResponseVerdictTest` |
| MD-K3 | A half-downloaded file never has the final name; it moves into place only after the check passes | `ModelFilesTest` (working file has a different name) and `ModelInstallerTest` (hash mismatch leaves nothing in place) |
| MD-K4 | A download that receives no byte does not loop forever (`no-progress`) | `ModelInstallerTest` |
| MD-K5 | The verifying step is reported before hashing starts | `ModelInstallerTest` |
| MD-K6 | After a 302 to another host `Range` survives and `Authorization` is dropped | `OkHttpRedirectProbeTest` |
| MD-K7 | The nine-state decision and cancel leaving the piece (paused) hold | `DownloadFactsTest` |
| MD-K8 | Percent rounds down and gigabytes show two decimals | `DownloadProgressTest` |
| MD-K9 | The quiz call-to-action follows the model state; the five working states read `받는 중 · NN%` and lead to settings | `ModelStatusUiTest` |
| MD-K10 | Work setup: unique work `quiz-model-download` with KEEP, Wi-Fi (unmetered) and storage not low, expedited, foreground dataSync, exponential backoff from 30 seconds | Read `DownloadWork.kt` and `ModelDownloadWorker.kt`; no automated test |
| MD-K11 | A notification tap opens settings without stacking it twice; progress `취소` leaves the piece; failure `다시 시도` opens settings and restarts the download | Manual on a device; no automated test |
| MD-K12 | The pill shows while working, 3.2 seconds after becoming usable, and for a failure while settings is not open | Compose preview with fake states; no automated test |
| MD-K13 | Tapping download, resume or retry on Android 13+ without the permission asks first and starts regardless of the answer | Manual on a device; no automated test |
| MD-K14 | The usable state shows `사용 가능 · 2.6GB 사용 중` with `삭제` | Manual on an emulator with the model file present; no automated test |
