---
units: [ai:engine, ai:quiz]
plans: [002-quiz, 005-stats]
decisions: [QZ-D1, QZ-D2, QZ-D3, QZ-D4, QZ-D5, D-55, DEC-30]
---
## engine — Model engine
### Behaviour
- One engine serves the whole app: the quiz and the statistics worker share it (002-quiz: QZ-6).
- The engine is not built at app start. The first request builds it, later requests reuse it; a user who never opens a quiz never loads the 2.6GB model (002-quiz: QZ-6).
- Building takes seconds (about 3.5 to 12 s measured, depending on file cache); the log line records the milliseconds. The build runs to the end even if the caller is cancelled, and the finished engine is kept in the slot, so leaving a quiz during the build does not waste it (002-quiz: QZ-6).
- A failed build leaves no engine in the slot; the next request tries again.
- Every use goes through one lock: only one generation runs at a time, and a second request waits (quiz) or is refused at once (worker). The library does not lock a conversation, so this lock is the only protection against concurrent generation (002-quiz: QZ-6).
- The quiz path (`use`) never closes the engine. It lives until the process ends or the model is deleted (`release`) (002-quiz: QZ-6).
- The worker path (`tryUse`) returns busy at once when the lock is taken, and does not queue. When it had to build the engine itself, it closes it when the work ends if the caller's check says so (the worker asks "is the app in the background"); if the app came forward meanwhile the engine stays for the next quiz. An engine that was already loaded is reused and never closed by this path (005-stats: ST-18).
- `release` waits for a running generation, then closes the engine; it is how model deletion frees the 2.6GB.
- Speculative decoding is switched on before the engine is built, because the setting is read at build time; reordering silently turns it off. The benchmark flag is on only in a debuggable build and is for logs only: no decision reads it.
- The engine asks for the GPU backend; the code cannot tell whether the GPU was really used. It falls back to the CPU without error or log unless the `:app` manifest declares the two native libraries (`libvndksupport.so`, `libOpenCL.so`, not required). The log guesses the backend from the first-token time: under 3 s counts as GPU. Measured on a Galaxy S25: first token 0.6 to 0.9 s on GPU, 6 to 7 s on CPU.
- The shader cache lives in the app cache folder; clearing it makes the next build slow again. The context length is the library default (not set here).
- Every generation is sent through one cancellable call: when the waiting coroutine is cancelled or times out, the native decode is cancelled too, so the lock is released within about a second instead of after the whole generation (002-quiz: QZ-10).
- A native failure that follows such a cancel is rethrown as the cancellation, never recorded as an engine failure (002-quiz: QZ-10).
- Reading a reply takes only the text parts of the message, never its string form. Heavy calls run on the default dispatcher, never on the main thread.
- The model file is looked up as `gemma-4-E2B-it.litertlm` in the app's external files folder. Availability is only "the file exists"; a damaged or foreign file fails at the first generation. Nothing in the app calls the availability check except the instrumented evaluation tests.
- No streaming: a reply arrives whole, because the JSON must be parsed as a whole.
### Public API
- `ModelEngine` — singleton holding the one engine; `use(block)` runs a block with the engine under the lock (builds it if needed), `tryUse(shouldRelease, block)` the same without waiting, `release()` closes it, `isAvailable()` and `modelFile()` locate the model file.
- `EngineUse` — result of `tryUse`: `Busy`, or `Done(value, initMillis, released)` where `initMillis` is null when the engine was already loaded.
### Guarantees
- A quiz-built engine is never closed by the quiz path, and a build cancelled midway stays in the slot; `tryUse` closes only an engine it built, when asked, and gives busy while the lock is taken (`EngineSlotTest`).
- Cancelling a generation cancels the native call, a timeout does the same, and a non-cancel failure is rethrown as it is (`CancellableTest`).
### Not covered
- Cutting the note to 2,000 characters before it reaches the model — `domain`, the quiz use case.
- The 60-second quiz timeout and what the screen shows for each failure — `quiz:quiz` in the feature module.
- Prompts and generation settings — `ai:quiz` and `ai:insight-generation`.
- Downloading or deleting the file — `ai:download`.
### Unconfirmed
- The model file is looked up only in the external files folder, while the download falls back to the internal folder when external storage is missing; what happens then is not checked.

## quiz — Quiz generation
### Behaviour
- A request is a note title, the note text and a count; the reply is a list of questions, or a failure (002-quiz: QZ-7).
- Every call opens a fresh conversation with a fixed system line, thinking off, greedy sampling (top-k 1, temperature 0) and an output budget of 340 tokens per asked question (002-quiz: QZ-7).
- The budget is a real limit: with too few tokens the JSON is cut off, the parser finds no array and the result is a Parse failure that looks like the model talking nonsense. A smaller budget (220) cut explanations mid-sentence (002-quiz: QZ-7).
- Because sampling is greedy, the same note and count give almost the same output; a retry with unchanged conditions rarely helps, a retry with fewer questions can (002-quiz: QZ-7).
- The instructions are in English and the answer is asked in Korean by one line of the prompt; removing that line makes answers come out in English (002-quiz: QZ-7).
- The prompt asks exactly the count, four choices that differ in meaning, plausible wrong choices, a zero-based answer index, a one-sentence explanation, JSON array only (002-quiz: QZ-7).
- The count is a request, not a check: the result holds whatever questions survive parsing, which may be fewer (the quiz screen shows a warning band) or, if the model over-delivers, more (002-quiz: QZ-14).
- Anything thrown while loading the engine or generating becomes an Engine failure; cancellation is rethrown as cancellation (002-quiz: QZ-8).
- The reply is read leniently: text before and after the JSON array, code fences and unknown keys are ignored; only the text from the first `[` to the last `]` is read (002-quiz: QZ-8).
- No array, or an array that does not decode, is a Parse failure; an array with no usable question is an Empty failure (002-quiz: QZ-8).
- A broken question is dropped and the rest are kept. A question is dropped when its text is blank, it has fewer than two choices, or its answer is blank (002-quiz: QZ-8).
- Answer number: a zero-based number inside the choices is taken as is; a number equal to the choice count is read as one-based and moved back by one; any other number drops the question. The answer is never guessed (002-quiz: QZ-9).
- Choices are trimmed, blank ones removed, and ones equal ignoring case and runs of spaces collapse to the first one. The answer is found by its text first, then its position is looked up again in the surviving list, so removing a duplicate never moves the answer to a wrong choice (002-quiz: QZ-9).
- A question left with two or three choices stays as a two- or three-choice question; with fewer than two it is dropped (002-quiz: QZ-9).
- More than four choices are cut to four; if the answer would be cut, the last slot is given to the answer, so the answer is never forced into a fixed position (002-quiz: QZ-9).
- Choices that differ in wording but mean the same (`실제 데이터 값` against `실제 값`) are not caught by the parser; only the prompt rule discourages them.
- The log records milliseconds, characters and, in a debuggable build, tokens and the guessed backend.
### Public API
- `LiteRtLmQuizGenerator` — the `QuizGenerator` implementation: `generate(noteTitle, noteText, count)` returns the questions or a `QuizFailure` (Parse, Empty, Engine); `isAvailable()` only checks that the model file exists.
### Guarantees
- The repair rules above (leniency, answer-number fix, duplicate removal order, four-choice cap, the three failure kinds) are fixed by `QuizJsonParserTest`; read it before changing the parser.
### Not covered
- Timeout failures, the 60-second limit and cancelling on leave — `quiz:quiz` in the feature module; this unit only makes the cancel reach the native call (`ai:engine`).
- Building the plain text of a note and cutting it to 2,000 characters — `domain`.
- Whether the model file is present or downloading — `ai:download`.
