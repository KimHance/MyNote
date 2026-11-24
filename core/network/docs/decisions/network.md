# Decisions

### SY-D7 · 2026-10 · SY-5
- decision: Failure causes reach the screen as six kinds (no internet, token rejected, not accessible, limit, server, format); a limit is 429, or 403 with remaining 0, or 403 with `retry-after`.
- why: One sentence for every failure left the user not knowing what to fix; secondary limits block even when the remaining count is not 0.
- rejected: A single failure sentence for all causes, and treating only 403 with remaining 0 as a limit.
- evidence: `SyncFailureMappingTest` (`SY-5 실패 응답이 원인 종류로 갈린다`, `SY-5 한도 규칙 3경우와 일반 403`), `SyncFailureKindTest`, `GithubFailures.kt`.

### SY-D8 · 2026-10 · SY-2
- decision: A list item's `path` is relative to the repository root, even when the list file sits in a subfolder.
- why: The note path resolution stays as it was; two reference points would make the same repository behave differently by connection method.
- rejected: Resolving item paths relative to the list file's folder.
- evidence: `GithubManifestAdapterTest` (`SY-2 항목 path 는 저장소 루트 기준이다`).

### DEC-11 · 2026-10 · RC-4
- decision: Link verification uses a dedicated OkHttp client (qualifier ProbeClient) with no auth interceptor; a candidate token is attached per request as an argument. Response classification (401, 404, 403, 429, limit headers) is one network function shared by verification and sync.
- why: The stored token must never ride along on a verification of another repository, and the shared client's interceptor attaches the holder's token to every request.
- rejected: Reusing the sync client for verification.
- evidence: ProbeClient annotation, NetworkModule and GithubConnectProbe in core/network.
