---
units: [ai:wiring]
plans: []
decisions: []
---
## wiring — Hilt wiring
### Behaviour
- Three singleton bindings connect the contracts of `domain` to this module: `QuizGenerator` to the quiz generator, `QuizModelSource` to the model downloads, `InsightScheduler` to the insight scheduling.
- The notification intents are an optional binding: without `:app` supplying them the download still runs, and notifications fall back to opening the launcher and drop the retry action.
- The download HTTP client is a separate qualified client: no overall call timeout (a 2.6GB body may take long), 20 s to connect, 60 s per read, retry on connection failure.
- `:app` owns the dependency edge that puts this module into the graph; the module itself does not start anything at app launch.
### Public API
- `AiModule` — the three bindings; `ModelDownloadIntentsModule` — the optional intents binding; `ModelDownloadModule` — provides the download client; `ModelDownload` — qualifier of that client.
### Not covered
- Scheduling or cancelling the statistics work from model and settings state — `:app` (`InsightScheduleSync`).
- Manifest lines that decide GPU use — `:app` manifest; see `ai:engine`.
