# Decisions

### QZ-D8 · 2026-09 · QZ-5, QZ-8
- decision: A quiz fails only with a typed `QuizFailure` (Parse, Empty, Engine), and cutting the note to 2,000 characters is done once in `GenerateQuizUseCase`, not by each generator.
- why: Branching on an exception message breaks silently when wording changes, and the screen picks its reason text and retry buttons by the type. The prompt length is the first-response delay, so one limit in one place keeps the standard from scattering.
- rejected: Telling failures apart by message text; each generator cutting the text by its own rule.
- evidence: `core/domain` `QuizFailure`, `GenerateQuizUseCase`, `GenerateQuizUseCaseTest`.
