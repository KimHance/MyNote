# Decisions

### RC-D1 · 2026-10 · RC-2, RC-3
- decision: The accepted inputs are a GitHub repository link (`owner/repo`, `github.com/...`, `/tree/<branch>[/<path>]`) and a link to a list file named `index.json` (`/blob/...` or raw); both become the same connection data (repository, branch, list location). The list file name is fixed.
- why: Owner decision. A link to a single note file must not be accepted as a list and rejected later.
- rejected: Accepting any file name as the list.
- evidence: `core/domain/.../connect/ConnectLinkParser.kt`, `ConnectLinkParserTest`, `ConnectFlowTest` (`RC-3 받지 않는 링크는 요청 없이 이유 7종이 다르다`).

### RC-D2 · 2026-10 · RC-4, RC-5
- decision: Public or private is never asked; the first try is without a token (a stored token is not attached to that request), and only an unauthenticated 404 opens token input; public repositories store no token.
- why: Owner decision. GitHub answers 404 for both a private and a missing repository, so the app cannot know in advance.
- rejected: Asking the user whether the repository is private, or attaching a stored token to the first check.
- evidence: `ConnectProbeTest` (`RC-4 무인증 확인에는 저장된 토큰을 싣지 않는다`), `ConnectFlowTest`, `core/network/.../api/ConnectProbeApi.kt`.

### RC-D10 · 2026-10 · RC-7
- decision: A public repository that hits the unauthenticated limit (60 per hour) switches the same screen to token input at that spot; a token is not recommended from the start; sync keeps its apply-nothing-on-failure contract.
- why: A public repository with about 55 or more notes cannot finish its first sync without a token, so the token that lifts the limit is offered where the user is blocked.
- rejected: Recommending a token on every connect.
- evidence: `ConnectFlowTest` (`RC-7 한도로 첫 동기화가 실패하면 토큰 권유 상태다`), `ConnectIssueTest`.

### RC-D25 · 2026-10 · RC-2
- decision: The link parser outputs a candidate list (branch and list-location pairs, longest prefix first, at most 3); the check step decides which is right; owner allows only letters, digits and `-`, a first segment with `.` is a host, SSH form is read as a repository and every scheme other than https is rejected.
- why: A branch name with `/` cannot be split from the string alone, so the expected values must come from a table test.
- rejected: A single split chosen by the parser.
- evidence: `ConnectLinkParserTest`, `ConnectLinkParser.kt`.

### RC-D27 · 2026-10 · RC-2
- decision: The whole path read as a branch (list location `notes/index.json`) is added as the last candidate, duplicates merge and the cap stays 3 (it is not added when the cap would be exceeded).
- why: Pasting a link whose branch contains `/` is common, failing to connect costs more than one extra candidate, and a right first candidate adds no request.
- rejected: Candidates from the prefix split only.
- evidence: `ConnectLinkParserTest` (`tree/main/docs` gives 2 candidates, `tree/feature/x` ends on the whole-path candidate).

### RC-D35 · 2026-10 · RC-6
- decision: When confirm ends in an exception, the connect flow re-reads the real connection state: already confirmed for this attempt means connected (home), otherwise a transient error that can be retried with input kept; discard is not called.
- why: The user only needs to know whether it worked, and one end state is removed.
- rejected: Discarding the attempt on a confirm exception.
- evidence: `ConnectFlowTest`, `ConnectionRepositoryTest` (`RC-15 · SY-1 confirm 이 예외로 끝나도 시도 중 표시가 풀린다`).

### RC-D54 · 2026-10 · RC-26
- decision: The template repository address is one build value (`owner/repo`) from a new key in the committed `gradle.properties` (empty before publishing), the same for debug and release; no default string in code; a missing or malformed value means none and the entry section and the RC-9 box's ways are not drawn.
- why: The template is not public yet; a committed value does not reach other clones through the ignored local properties file, and the name can be changed without code and without a private repository name in the public APK.
- rejected: Reading it from `local.properties`, and a constant in code.
- evidence: `core/domain/.../connect/TemplateRepo.kt`, `TemplateRepoTest`, `app/build.gradle.kts` (`TEMPLATE_REPO`).

### RC-D23 · 2026-10 · domain:connection, RC-22
- decision: On a seed connection the debug build's embedded token is written to the token store and applied before the sync, and a token that is already stored is not overwritten; the fallback path that read the embedded token directly is removed.
- why: A fallback would revive the token after a disconnect, and loading it into the holder without writing it would let the stored value overwrite it right away and break a private seed.
- rejected: A fallback read of the embedded token, and placing it only in the in-memory holder.
- evidence: `ConnectionUseCaseTest` (`RC-22 내장 토큰은 시드 연결에만 쓴다`, `RC-22 시드 연결은 토큰이 적용된 뒤에 동기화한다`, `RC-22 이미 저장된 토큰을 시드가 덮어쓰지 않는다`).
