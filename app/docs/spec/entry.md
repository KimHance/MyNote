---
units: [app:startup, app:launch, app:share-in, app:download]
plans: [001-reader-foundation, 003-model-download, 005-stats, 007-repo-connect, 010-brand, 013-local-notes]
decisions: [D-44, RC-D23, BR-D1, BR-D10, BR-D12, BR-D14, LN-D17]
---
## startup — Startup tasks
### Behaviour
- `MyNoteApplication` is the Hilt application and the WorkManager configuration provider, using the Hilt worker factory; the manifest removes WorkManager's own initializer so this configuration is the one used.
- At process start it starts four things, in this order: token sync, the model wait notice (see `download`), study-log maintenance and insight schedule sync.
- Each of the four lives only because `MyNoteApplication` holds an injected field for it and calls `start()`; nothing else references it. Deleting a field compiles and runs but silently drops the behaviour (007-repo-connect: RC-K6).
- Every `start()` is idempotent: a second call does nothing.
- Token sync first runs connection recovery (`data:connection`), which finishes an interrupted disconnect or an unconfirmed connect attempt; any failure there is swallowed, cancellation is not (007-repo-connect: RC-17).
- Token sync then observes the stored token and pushes each distinct value into the network token holder (`network:token`) that the auth interceptor reads on every request; an I/O failure reading storage counts as no token, any other failure ends the observation (001-reader-foundation: RD-20).
- The same holder is the applied-token source whose change makes the notes ViewModel run sync again (001-reader-foundation: RD-2).
- Token sync forwards only the stored token and has no fallback to the build-embedded one; after disconnect or token removal the build token never comes back, it is used only by the seed connection (007-repo-connect: RC-22).
- Study-log maintenance runs the prune use case (`domain:study-log`) once per process start; stored events, quiz answer text and summaries are cut to their retention periods there (005-stats: ST-7).
- Insight schedule sync watches the model status and the study-record switch together; when the model is ready and records are on it schedules the unique daily insight work, otherwise it cancels it (005-stats: ST-17).
- Insight schedule sync reacts only to distinct values; scheduling again while unfinished work with the current spec tag exists does nothing (`ai:insight-worker`). If either source fails it cancels and stops observing until the next process start.
### Public API
- `MyNoteApplication` — the Hilt application class named in the manifest.
- `TokenSync.start()` — starts the token pipeline.
- `InsightScheduleSync.start()` — starts the schedule observer.
- `StudyLogMaintenance.start()` — runs the prune once.
### Not covered
- The schedule rule (`insightsWanted`) and the retention numbers — `domain:study-log`; recovery steps — `data:connection`; the worker itself — `ai:insight-worker`; the token holder — `network:token`.
- No test fails when one of the injected fields is removed; the wiring is checked by reading.

## launch — Main activity and splash
### Behaviour
- Before `super.onCreate` the activity turns edge-to-edge on with transparent system bars and light icons; the app has one dark theme while the XML theme's parent is a light platform theme, so this is fixed once here (010-brand: BR-K6).
- The window background before the first frame is the app background colour, so no white frame appears after the splash (010-brand: BR-4).
- Content is the title-font provider around `NotesRoute`; the theme itself starts inside the shell.
- The title font is decided once per process by a holder shared by all activity instances: the first creation starts the fetch of Gothic A1 from the font provider; recreation and warm starts reuse the decision (010-brand: BR-8).
- A font that arrives before the wait ends is used from the first frame; a late or failed fetch, no provider or offline means the device font for the whole process, never an error or a hang, and the provider finishes the fetch in the background for the next process (010-brand: BR-8).
- The wait ends at the later of the first drawable frame and the end of the lit animation: launch request time plus the theme's animation length times the animator scale. With animations off only a font that arrived by the first frame counts (010-brand: BR-8, BR-4).
- The launch request time is the process start request on a cold start (screen built within 5 s of it) and the screen creation time otherwise.
- The provider's answer is received on a separate looper thread, not the main thread, and the decision runs off the main thread; otherwise the answer queues behind the first composition and always loses (010-brand: BR-8).
- `setContent` runs at once so ViewModels and the start decision are not delayed; only drawing is held, by a pre-draw listener, until a composition with the decided font has been applied. The first blocked draw is the signal that the first frame is ready. A process that already decided opens on its first composition (010-brand: BR-8).
- No frame shows the title font changing; the pre-decision composition uses the device font and is never drawn.
- A debug build can name a non-existent font to reproduce the not-received case; release always uses the official name.
- The splash exit listener is installed only when no other `MainActivity` is alive in the same task; creation enters the task and destruction leaves it (010-brand: BR-4).
- The listener waits until the icon animation start plus its duration times the animator scale, then fades the splash view and its icon view to transparent over 180 ms with the exit easing and removes the view. A missing start or duration, or an end already past, lifts at once; scale 0 does not wait (010-brand: BR-4).
- The splash never waits for data; if the start decision is still open the reading screen follows (010-brand: BR-4) (007-repo-connect: RC-1).
- Without the listener, as in a warm start whose previous screen is still alive in the same task, the system lifts the splash by default; installing the listener there would lose the icon, because the earlier screen's destruction takes the handed-over icon surface and leaves only the empty night background (010-brand: BR-4).
- An intent with the open-settings extra pushes Settings at once, unthrottled, on create and on new intent; the extra is removed right after reading so a shared intent object cannot push twice (010-brand: BR-K7).
- Opening uses flags new-task, clear-top and single-top so a live activity gets a new intent instead of being recreated; pushing Settings while it is already on top changes nothing.
- The retry intent adds a retry extra; after pushing Settings the activity starts the model download once and removes the extra (003-model-download: MD-K11).
- Each notification intent has its own action string, so its pending intent differs from the launcher one of `:core:ai` and neither overwrites the other's extras.
- The activity is not recreated on rotation, split screen or keyboard change; predictive back is on, and the window resizes for the soft keyboard (001-reader-foundation: RD-18).
### Public API
- `MainActivity` — the single launcher activity.
- `MainActivity.settingsIntent(context)` — an intent that opens the app on Settings.
- `MainActivity.retryIntent(context)` — the same plus a model-download retry.
- `MainActivity.EXTRA_OPEN_SETTINGS`, `MainActivity.EXTRA_RETRY_DOWNLOAD` — the extra keys those intents carry.
### Guarantees
- `SplashWaitTest` (instrumented) proves the wait: none when start or length is unknown or the end has passed, remaining time up to the end scaled by the animator, none at scale 0.
- `LiveTasksTest` (instrumented) proves the listener is skipped while an earlier screen of the same task is alive (counted per screen), and installed once all earlier ones ended or for another task.
- `LauncherMaskTest` (instrumented) proves the arch top of the launcher mark is not clipped by circle, rounded or device masks in colour and themed form, and saves the renders to the app's external cache (010-brand: BR-1).
- `TitleFontScreensTest` (instrumented) proves the statistics summary sentence has the same line count in the device font and in Gothic A1, skipped without provider or network (010-brand: BR-8).
### Not covered
- The font decision and its fallback rules — `design-system:font`; the pixel assets of icon and splash — `app:build` resources.
- Which screen the settings push shows — `settings:settings`; the stack rules — `navigation:navigator`.

## share-in — Share receiver
### Behaviour
- The app appears in the share sheet for text and for one file: it accepts `text/*` and `application/octet-stream` sent by SEND; several items shared at once are not accepted (013-local-notes: LN-8).
- The trampoline has no UI, is kept out of recents and history, has no task affinity, and finishes at once after starting the main activity (013-local-notes: LN-8).
- It starts `MainActivity` with its own action and flags new-task, clear-top, single-top; the main activity never takes a share itself, because that would start a second instance in the sender's task sharing the singleton navigator (013-local-notes: LN-8).
- Shared text goes to the inbox (`ui:signal`), never into the intent, because a 512 KB text reaches the cross-process transfer limit (013-local-notes: LN-8).
- The subject is the subject extra, else the title extra; no text becomes an empty text, which the editor reports as nothing to import (013-local-notes: LN-8).
- A file attached to the share wins over the text: the text sent along with it is dropped, the subject is kept (013-local-notes: LN-8).
- A file is accepted only when its scheme is `content`, it has an authority, and the authority is not this app's own provider; a user-id prefix before `@` is ignored in that check (013-local-notes: LN-8).
- Own authority means equal to the application id, starting with it plus a dot, or resolved by the system to this app's provider; a `file://` address would let another app read device files with this app's rights, and an own authority would reach an unexported provider.
- An accepted file puts only its address in the inbox; the read grant is passed on to the main activity through the clip data because the grant held by the trampoline may end with it. The editor reads the file off the main thread.
- A refused file reads nothing; the inbox gets a refusal with the subject and the editor reports the unsupported kind (013-local-notes: LN-7).
- Only the first creation with a SEND action does anything; a recreation or another action just finishes.
### Public API
- `ShareReceiverActivity` — the exported trampoline named in the manifest.
### Guarantees
- `ShareRequestTest` proves a file wins over text, text becomes empty text without a body, non-content addresses are refused, and an own authority is refused (013-local-notes: LN-8).
### Not covered
- Reading, size and encoding checks and the five reject reasons — `domain:device-notes` (the rules) and `data:device-notes` (the file read); opening the editor — `editor:session`; the inbox type — `ui:signal`.

## download — Model download wiring
### Behaviour
- The module supplies the two pending intents the download notifications use: the body tap opens the app on Settings, the failure notification's `다시 시도` action opens Settings and restarts the download. Both are activity intents; the manifest has no receiver (003-model-download: MD-K11).
- Request codes are 4211 for retry and 4212 for open-settings, apart from the launcher intent code 0 of `:core:ai`; both pending intents are immutable and update-current.
- These intents fill an optional hole that `:core:ai` declares (`ai:wiring`). Without the binding the retry action silently disappears and a tap opens the launcher screen instead of Settings, while the download itself still works.
- Retry works because starting a download is a keep-existing unique work: a finished work takes the new request, a running one is left alone, so one start call serves first start and retry.
- An action tap is not auto-cancelled by the system; the failure notification disappears because the start clears the result slot.
- When the app stops, the wait notice asks the downloads to post a resting notification: waiting for Wi-Fi shows the waiting one, retrying shows the retry one, other states are left alone; coming back to the front does not remove them (003-model-download: MD-11, MD-9).
- It also watches the status and clears every download notification when the status is none or paused; while a work exists it clears nothing, because a late status could erase a result or retry notification just posted (003-model-download: MD-10).
- This observation is needed because the resting notification's cancel action goes straight to WorkManager and runs no app code; an action that wakes the process re-attaches at application start.
- The notice observer starts only from `MyNoteApplication`; a second `start()` does nothing.
### Public API
- `AppModelDownloadIntents` — `retry()` and `openSettings()` pending intents.
- `ModelWaitNotice` — `start()` begins the lifecycle and status observation.
### Not covered
- Choosing which notification a status shows, the download work and its retry rules — `ai:download-work`.
- The model card and pill texts — `settings:model` and `ui:model-status`. No automated test covers these two classes; they are checked by hand on a device.
