---
units: [domain:quiz, domain:quiz-model]
plans: [002-quiz, 003-model-download, 009-note-format, 013-local-notes]
decisions: [QZ-D8]
---
## quiz — Quiz generation
### Behaviour
- Generating takes only a note id and a question count (3 unless the caller says otherwise); the caller does not need to hold the note.
- The note is found in the cached category tree (the first value it emits), never over the network. A note that is not found fails as an empty-input failure and the generator is not called (002-quiz: QZ-5).
- The note's blocks become plain text, which is cut to its first 2,000 characters; the cut is made here so every generator can assume the text is already short, and a cut may fall in the middle of a line (002-quiz: QZ-5).
- A plain text that is blank fails as an empty-input failure and the engine is not woken (002-quiz: QZ-5).
- Otherwise the generator receives the note title, the cut text and the count, and its result is returned unchanged: no retry, no filtering of questions, no timeout.
- Plain text follows one rule set for every block: headings, paragraphs and quotes are one line of their text; code is included line by line so questions can be asked about code; dividers and spacers are dropped (009-note-format: NF-10) (002-quiz: QZ-5).
- A list item is one line starting `- `, `- [x] ` for a checked box or `- [ ] ` for an unchecked box, in numbered lists too (numbers are not written); an item whose text is empty writes no line but its child blocks still do (009-note-format: NF-10).
- A table writes its header row first and then each row as one line, cells joined by ` | ` (009-note-format: NF-10).
- Child blocks of a list item are indented two spaces per depth, every line of multi-line text carries the indent, and a block with no text writes no line (009-note-format: NF-10).
- Strike-through text stays as `~~text~~` and adjacent struck runs merge into one pair, so the model does not read deleted text as fact. Bold, italic and code marks are not written (009-note-format: NF-10).
- An unsupported block contributes only its salvaged text, never its original source, because the salvaged text is what the box shows on screen and a question must be answerable from the visible note (002-quiz: QZ-5).
- Device notes are handled exactly like repository notes: same entry, same text, same recorded attempt (013-local-notes: LN-14).
- The failure types are three: parse, empty input and engine (with the cause). Their messages are English diagnostics for developers; screens branch on the type, never on the message.
- A time-out is not a domain failure: the quiz view model wraps the call in its own 60-second limit, which is why the screen has a time-out kind and this module does not.
- Whether the quiz can be used is judged from the model state (unit `quiz-model`), not from the generator's availability check, which no app code calls.
### Public API
- `GenerateQuizUseCase` — `(noteId, count)` gives a `Result` of questions.
- `QuizQuestion` — question, choices, zero-based answer index, explanation.
- `QuizFailure` — `Parse(detail)`, `Empty(detail)`, `Engine(cause)`.
- `QuizGenerator` — `isAvailable()` and `generate(noteTitle, noteText, count)`; implemented in `ai`.
### Guarantees
- The plain-text rules (new block kinds, indentation, strike marks, salvaged-text-only), the not-found and blank cases and the device-note entry are covered by `GenerateQuizUseCaseTest`.
### Not covered
- The prompt, output format, sampling, the engine and the lenient reading of the model reply — `ai`.
- The question count choice, the 60-second limit, the quiz screens and the failure wording — the quiz feature module.
- Recording an attempt — unit `study-log`.

## quiz-model — Quiz model state
### Behaviour
- The model has nine states: missing, paused (bytes received and total), waiting for Wi-Fi, preparing, downloading (received and total), retrying (received and total), verifying, ready and failed (a reason).
- Ready means the installed model file is present; the quiz entry follows this state, and feature modules read it through the observe command rather than injecting the generator.
- A failure reason is plain text. Three values are defined here: `no-space`, `no-progress` and `too-many-rounds`; others (an HTTP status, unknown) come from the download code, and a reason a screen does not know reads as an ordinary failure with retry (003-model-download: MD-6).
- Five commands act on the model: start, cancel, discard the partial file, delete the model (the only suspending one) and observe the state. Each is a one-line pass-through; no rule, throttle or confirmation is added here.
### Public API
- `ModelStatus` — the nine states; `ModelFailure` — the three reason constants.
- `QuizModelSource` — `status()`, `start()`, `cancel()`, `discardPartial()`, `deleteModel()`; implemented in `ai`.
- `StartModelDownloadUseCase`, `CancelModelDownloadUseCase`, `DiscardPartialModelUseCase`, `DeleteModelUseCase`, `ObserveModelStatusUseCase`.
### Not covered
- The download, verification, retry and notifications and what each command does to the files — `ai`.
- The model card, pill and wording — the settings and quiz feature modules.
- Starting or stopping the weekly worker when the state changes — units `study-log` and `insights`.
