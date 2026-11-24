---
units: [network:contents, network:probe, network:token, network:status, network:network]
plans: [006-sync, 007-repo-connect, 008-note-template, 009-note-format]
decisions: [SY-D7, SY-D8, DEC-11]
---
## contents — GitHub contents adapter
### Behaviour
- The adapter serves sources of kind GitHub and sends only GET requests to `repos/{owner}/{repo}/contents/{path}`, with `ref` set to the branch (007-repo-connect: RC-K1).
- Owner, repository, branch and list location are read from the source's stored connection on every request; the module keeps no coordinates of its own (006-sync: SY-2).
- An empty branch sends no `ref`, so GitHub uses the repository's default branch (007-repo-connect: RC-K1).
- A source with no readable stored connection fails before any request (sync never calls the adapter for such a source) (006-sync: SY-3).
- Both requests ask for the raw file with `Accept: application/vnd.github.raw+json`; without it GitHub answers with a metadata envelope holding base64 content, which the list decoding and the original text would both get wrong (007-repo-connect: RC-K1).
- Paths are sent unencoded, so the slashes of `notes/<group>/<id>.md` stay slashes (007-repo-connect: RC-K1).
- The list file is an object whose keys are group names and whose values are item arrays (id, title, date, path, hash); listing flattens it into entries with doc id = item id, revision = item hash, group = key (008-note-template: NT-K5).
- An item's path is used as given, relative to the repository root, not to the list location (006-sync: SY-2) (009-note-format: NF-K2).
- The list is decoded strictly as a whole: a list that is not JSON or has the wrong shape fails the source as a format failure; items are never skipped one by one, since a skipped item would delete its note.
- A note body is returned as the unparsed original string, even when it is broken JSON or Markdown; parsing happens later per note in `data:sync`, so one broken note cannot become a transfer failure.
- Every changed note is one request; there are no conditional requests, no batching and no app-level retry.
- Failures are thrown as one of six kinds, with the original error kept as the cause: token rejected (401), access denied (404, 403, 451), rate limited, server (5xx, any other status, timeout), format (list not decodable), offline (other I/O errors) (006-sync: SY-5).
- Rate limited is 429, or 403 with `x-ratelimit-remaining: 0`, or 403 with any `retry-after`; another 403 is access denied (006-sync: SY-5).
- Body fetch failures are classified the same way as list failures (006-sync: SY-5).
- Cancellation and errors that are not HTTP, decoding or I/O errors pass through unchanged.
### Public API
- `GithubManifestAdapter` — the note-source adapter of `data:contracts` for kind GitHub: `list(source)` gives entries, `fetch(source, entry)` gives the original text.
- `GithubContentsApi` — Retrofit interface with `getIndex` (decoded list) and `getNote` (raw body), and `BASE_URL`, `https://api.github.com/`.
### Guarantees
- Group keys fold into entries, bodies come back unparsed (a broken body does not throw), request path, `ref`, `Accept` header and unencoded slashes, a stored `a/b`, `dev`, `docs/index.json` connection requests `contents/docs/index.json?ref=dev` and fetches items without `docs/`, empty branch omits `ref`, a source without connection fails with no request: `GithubManifestAdapterTest`.
- Each status, the three rate-limit cases, a plain 403, a timeout and a closed server map to their kinds, and body failures keep the HTTP cause: `SyncFailureMappingTest`.
### Not covered
- Revision comparison, reconcile, parsing and what a failure does to the cache — `data:sync`.
- Showing failure kinds — `settings:repo` (repository card, from `ui:repo-card`) and `connect:issues` (connect screen); the sync snackbar shows no kind (`ui:notices`).
- Writing to GitHub — the app has no write path.
### Unconfirmed
- GitHub allows 60 unauthenticated requests per hour, so the first sync of a large public repository can hit the limit; this is GitHub's rule and not checked in this code.

## probe — Connection check
### Behaviour
- The check runs on its own client that has no auth header; a token is sent only when the caller passes one, as `Authorization: Bearer <token>`, so a stored token never rides on the unauthenticated first check (007-repo-connect: RC-4).
- It first reads `repos/{owner}/{repo}` and takes the canonical owner and name from `full_name` (falling back to the typed ones when malformed), the default branch and the private flag (007-repo-connect: RC-12).
- Repository step failures: rate limit gives rate limited; 5xx or another status gives transient; 401 gives token rejected with a token, else token needed; 404, 403 or 451 gives repository not visible with a token, else token needed (007-repo-connect: RC-5, RC-7).
- An unreadable or empty repository answer is transient.
- Candidates (branch and list location) are tried in order; the default-branch candidate becomes the repository's default branch, and an empty branch sends no `ref` (007-repo-connect: RC-2).
- The first candidate whose list answers with success (2xx) verifies: the connection (canonical owner and name, branch, list location, public = not private) plus the note ids of that list; later candidates are not tried (007-repo-connect: RC-4, RC-12).
- An empty list object verifies with no note ids; a list that does not decode gives a broken-list failure (007-repo-connect: RC-10).
- Only 404 moves on to the next candidate; list rate limit gives rate limited, 401 gives token rejected or token needed, 403 or 451 gives list not reachable, 5xx or any other status gives transient (007-repo-connect: RC-7).
- When every candidate is 404: without a token the repository has no list (format not supported); with a token the list is not reachable (007-repo-connect: RC-9).
- A timeout gives transient, any other I/O error gives offline; neither changes anything stored (007-repo-connect: RC-11).
- The check reads only the repository and the list file; it never fetches note bodies and never stores anything (009-note-format: NF-K3).
### Public API
- None — `GithubConnectProbe`, `ConnectProbeApi` and `GithubRepoDto` are internal; callers use the `ConnectProbe` of `domain:connection`, bound in `network`.
### Guarantees
- No stored token on the unauthenticated check, public repository connects without token, 404 and list 401 ask for a token, five token failures differ, rate-limit rules, no list, broken and empty list, offline versus transient, candidate order and stop at the first hit, default branch from the answer, note ids returned: `ConnectProbeTest`.
### Not covered
- Reading the link into candidates and refusing a link — `domain:connect-link`; checking offline before the request — `domain:connection` and `status`.
- What a result does to the screen — `connect:issues`; to stored state — `data:connection`.

## token — Applied token
### Behaviour
- The applied token lives in memory only, starts empty on every process start and is filled by the app's token sync (`app:startup`) from the stored token; nothing else writes it and there is no built-in fallback token (007-repo-connect: RC-K6).
- A blank token is stored as no token (007-repo-connect: RC-K2).
- The auth header reads the applied token on every request and adds `Authorization: Bearer <token>`; with no token no header is sent at all (007-repo-connect: RC-K2).
- Its stream tells others when a token is actually applied to the client, not merely saved; the notes screen (`notes:state`) syncs again on a change, and connection confirm (when it drops the old token) and discard (when it restores the earlier token) wait on it (`data:connection`) (006-sync: SY-1).
- If the app's token sync (`app:startup`) is not kept alive, every request goes out without authentication, silently (007-repo-connect: RC-K6).
### Public API
- `GithubTokenHolder` — singleton holder: `update(token)`, `currentToken()`, `applied()` stream; it is both `GithubTokenProvider` and the domain `AppliedTokenSource`.
- `GithubTokenProvider` — the token read seam of the auth header.
- `AuthInterceptor` — OkHttp interceptor adding the bearer header.
### Not covered
- Storing the token — `database:prefs` and `data:records`; masking and editing it — `settings:repo`.
- Keeping the token sync alive — `app:startup`.
- The connection check's token — passed per request in `probe`, not through this holder.

## status — Network status
### Behaviour
- The status is read on demand as a snapshot of the active network; it is not observed.
- No connectivity service, no active network, no internet capability or an unvalidated network reads as offline (007-repo-connect: RC-11).
- Otherwise Wi-Fi wins (also when Wi-Fi and cellular are both up during a handover), then cellular, else other (for example Ethernet).
### Public API
- `NetworkStatusSourceImpl` — the domain `NetworkStatusSource`: `current()` gives offline, Wi-Fi, cellular or other.
### Guarantees
- Wi-Fi, cellular, other, no internet, unvalidated and handover cases: `NetworkStatusResolverTest`.
### Not covered
- Deciding not to send the check while offline — `domain:connection`.

## network — Network module root
### Behaviour
- The main client carries the auth header; the probe client has none; both use timeouts of connect 10 s, read 15 s, write 10 s.
- No logging interceptor is installed, so no network log appears.
- Both Retrofit instances use base `https://api.github.com/` and the shared `Json` from `data`; this module never creates its own `Json`.
- Binds the adapter into the adapter set, the probe, the holder as both token provider and applied token source (one instance), and the network status source.
### Public API
- `NetworkModule` — clients, Retrofit and the contents API.
- `RemoteDataSourceModule` — bindings of the adapter, probe, holder and status source.
- `ProbeClient` — qualifier of the client without auth header.
### Not covered
- The `Json` settings — `data:data`.
