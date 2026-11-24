---
units: [domain:connect-link, domain:connection, domain:settings]
plans: [001-reader-foundation, 006-sync, 007-repo-connect, 013-local-notes]
decisions: [RC-D1, RC-D2, RC-D10, RC-D25, RC-D27, RC-D35, RC-D54, RC-D23]
---
## connect-link — Connect link
### Behaviour
- A pasted address is read into a repository (owner and name) and up to three candidates, each a branch (the default branch or a named one) plus a list-file location. No network is used (007-repo-connect: RC-2).
- Surrounding whitespace is trimmed, then everything from the first `#` or `?` on is dropped; an address that is empty after that, including one that is only a query, is rejected as empty (007-repo-connect: RC-2, RC-3).
- Seven rejection reasons exist and each is a different message to the person: empty, not https, not a GitHub address, Gist, GitHub Pages (`*.github.io`), single file, and owner only or no repository (007-repo-connect: RC-3).
- Anything shaped like `scheme:` is a scheme and only `https://` (any letter case) passes; `http`, `ftp`, `ssh://`, `git://`, `file:` and `javascript:` all give the not-https reason (007-repo-connect: RC-3).
- With no scheme, a first segment that contains a dot is read as a host (`example.com/foo` is not GitHub, `owner.github.io` is Pages); a first segment without a dot is the owner of `owner/repo` (007-repo-connect: RC-2).
- Accepted hosts are `github.com` and `www.github.com` (case ignored) and the SSH form `git@github.com:owner/repo.git`; `raw.githubusercontent.com` is read as a raw file link; `gist.github.com` is the Gist reason; `*.github.io` is the Pages reason; any other host is not-GitHub (007-repo-connect: RC-2, RC-3).
- Owner characters are letters, digits and `-`; repository characters are letters, digits, `.`, `_` and `-`; a trailing `.git` is dropped. An owner with a dot or an underscore is not GitHub (007-repo-connect: RC-2).
- `owner`, `github.com`, `owner/`, `owner/.git` and `https://github.com` give the missing-repository reason. Empty path segments (`a//b`) are dropped and `%2F` is never decoded (007-repo-connect: RC-3).
- After owner and repository, `tree/…` and `blob/…` are read for a branch and a location; any other page (issues, pull, releases, wiki, none) reads as the repository itself with the default candidate: default branch and `notes/index.json` (007-repo-connect: RC-2).
- `tree/<parts>` with no part gives the default candidate. With parts, candidates run from the longest branch prefix to shorter ones: each splits the parts into a branch and a folder whose `index.json` is the list, and the whole path as a branch with `notes/index.json` comes after them. Duplicates are dropped and at most three are kept, so a long path may lose the whole-path candidate (007-repo-connect: RC-2).
- `blob/…` and raw links must name a path ending in `index.json` and have a branch part before it; otherwise the single-file reason. The candidates split the parts from the longest branch down, at most three (007-repo-connect: RC-2, RC-3).
- A template repository value `owner/repo` is read into the two public addresses `https://github.com/owner/repo` and the same with `/generate`. A value that is not exactly two parts, has bad characters (quotes, spaces, backslashes), or whose name is `.`, `..` or ends in `.git` reads as none (007-repo-connect: RC-26).
### Public API
- `parseConnectLink(input)` — a `ConnectLinkResult`: `Accepted(repo, candidates)` or `Rejected(reason)`.
- `ConnectLinkRepo`, `ConnectCandidate`, `ConnectBranch` (`Default`, `Named`), `ConnectLinkRejection` (seven values).
- `TemplateRepo` (`introUrl`, `generateUrl`) and `parseTemplateRepo(value)` — null when the value is not valid.
### Guarantees
- A table of more than 20 address shapes, the seven reasons, the scheme rule, the three-candidate cap and the owner rule are covered by `ConnectLinkParserTest`; the template value rules and that a valid template value is also accepted by the link parser by `TemplateRepoTest`.
### Not covered
- Checking the repository and list over the network — `network:probe` (through the probe in unit `connection`).
- Where the template address is configured — `app:build`; the entry button that opens it — `connect:entry`.

## connection — Repository connection
### Behaviour
- Verifying a link runs in this order: parse (a rejected link returns its reason with no request), network (an offline device returns the offline failure with no request; wifi, cellular and other all go on), then one probe with the repository, the candidates and the candidate token (007-repo-connect: RC-3, RC-11).
- The candidate token is trimmed and a blank token means none. The probe runs without a token unless the caller passes one, even when a token is stored (007-repo-connect: RC-4, RC-6).
- A probe failure is one of nine kinds: needs token, token rejected, repository not visible, list inaccessible, rate limited, no list file, malformed list, offline, transient. Verification returns it as it is and changes nothing (007-repo-connect: RC-5, RC-7, RC-11).
- A verified probe gives the connection to use (repository name as GitHub spells it, branch, list location, public flag), the trimmed candidate token and what replacing would erase (007-repo-connect: RC-12).
- With a connection already saved: the same repository (owner and name compared ignoring case, other branch or location allowed) erases nothing; another repository erases all notes and bookmarks of the saved connection, and says the token will be dropped only when the new one is public, no candidate token was given and a token is saved (007-repo-connect: RC-14, RC-15).
- With no connection: the repository counts what adopting would erase from the new list's note ids (only notes missing from that list, and their bookmarks); zero means nothing to confirm, otherwise it erases that count and flags that it adopts. Device notes are never counted (007-repo-connect: RC-15, RC-19) (013-local-notes: LN-12).
- Connecting emits "syncing" first, then one result: connected with the sync outcome, or failed with a sync failure (007-repo-connect: RC-6).
- The attempt begins with the connection and the candidate token before the first sync; the first sync waits for the saved token to be applied, and the connection is confirmed only if the sync succeeds (007-repo-connect: RC-6).
- A failed first sync discards the attempt; the prior token comes back and the old connection, notes and token stay. A failure that is not a sync failure is wrapped as a server failure (007-repo-connect: RC-6, RC-15).
- Cancelling while the first sync runs discards the attempt and rethrows the cancellation; confirm and the start of the attempt are not cancellable once begun (007-repo-connect: RC-24).
- If confirm throws, the connection is read again: when it shows the pending connection confirmed, the result is connected; otherwise failed with a server failure. Discard is never called after confirm has started, so only a confirmed connection's token keeps its new value (007-repo-connect: RC-6).
- Before the first sync, the saved token is waited for up to 5 seconds until the network client reports it applied. On timeout with a saved token the sync is not started and the result is a server failure; with no saved token it goes on (007-repo-connect: RC-6, RC-22).
- The build seed (debug builds only) is used when it has both owner and repository and the repository says it is eligible; otherwise nothing happens. Its connection has the default list location, the branch as given (an empty branch means the repository default), and is public when the seed has no token (007-repo-connect: RC-19).
- A seed token is saved only when no token is saved yet, so a token the person replaced is never overwritten; the seed then syncs after the token is applied, confirms on success and discards on failure or cancellation (007-repo-connect: RC-19, RC-22).
- A sync run is one per trigger key: the applied token plus a refresh counter, observed together with whether a connection attempt is open. A changed key cancels a running sync and starts a new one; cancelling is not a failure (006-sync: SY-1).
- While an attempt is open no sync runs and the current key is taken as handled, so connecting with a new token starts no extra sync; confirming starts none either. After the attempt is discarded, a later change of the applied token (the token rolled back) starts one (006-sync: SY-1).
- A run is announced when the refresh counter is above zero: the start-up run and runs after connection changes made before any refresh tap are silent, a tapped refresh announces, and a token-change run after the first tap is announced too (006-sync: SY-1) (001-reader-foundation: RD-2).
- A run reports running, then finished with the sync result; while an attempt is open it reports idle instead. A new key means the earlier run's finished report never appears.
- Begin saves a blank token as none; the other commands (confirm, discard, count erasable, disconnect, recover, observe) are one-line pass-throughs. Disconnect returns a result, and recover completes work an interrupted disconnect left behind (007-repo-connect: RC-17).
### Public API
- `VerifyConnectLinkUseCase` — `(link, candidateToken)` gives a `ConnectVerification`: `Ready(connection, candidateToken, replacement)`, `Rejected(reason)` or `Failed(failure)`.
- `ConnectReplacement` (`None`, `Erases(count, dropsToken, adopts)`), `ConnectProgress` (`Syncing`, `Connected`, `Failed`), `ConnectProbeFailure` (nine values).
- `ConnectRepositoryUseCase` — `(ready)` gives a flow of `ConnectProgress`.
- `ConsumeSeedUseCase`, `SeedOutcome` (`NotApplicable`, `Connected`, `Failed`), `ConnectionSeed`.
- `SyncPendingConnectionUseCase`, `ObserveSyncRunsUseCase` (`(refreshes)` gives `SyncRun` values: `Idle`, `Running(announce)`, `Finished(announce, result)`).
- `BeginConnectionUseCase`, `ConfirmConnectionUseCase`, `DiscardConnectionUseCase`, `DisconnectUseCase`, `RecoverConnectionUseCase`, `CountErasableUseCase`, `ObserveConnectionUseCase`.
- `ConnectionState` (`None`, `Connected`), `PendingConnection` (`New`, `Adopt`, `SameRepository`), `ErasureCount`, `RepoConnection` (`fullName`, `isSameRepositoryAs`, `DEFAULT_INDEX_PATH`).
- `ConnectionRepository` (implemented in `data:connection`), `ConnectProbe` with `ConnectProbeResult` (implemented in `network:probe`), `NetworkStatus`, `NetworkStatusSource` (implemented in `network:status`).
### Guarantees
- No request on a rejected link, offline, probe failures, token handling, replacement counts, confirm-exception handling and cancel behaviour are covered by `ConnectFlowTest`.
- Seed use, partial seed, no revival after a disconnect, the token rules and the not-applied failure are covered by `ConnectionUseCaseTest`; the sync-run rules by `SyncRunsTest` (in the same file).
### Not covered
- Persisting the connection, the pending marker, the erase order, adoption itself and what disconnect removes — `data:connection`; the connection state files — `database:prefs`.
- The GitHub requests of the check, status mapping and the probe implementation — `network:probe`; the requests of a sync — `network:contents`.
- The applied-token holder — `network:token`; the push of the saved token into it — `app:startup` (token sync).
- Connect screens — `connect:flow`; failure sentences — `connect:issues`; the change and take-over dialogs — `connect:confirm`.
- The first-run decision of which screen to show — `app:shell-state`.

## settings — Token and settings values
### Behaviour
- Two token interfaces exist and are not duplicates: the saved token is what the person stored, the applied token is what the network client is using now. The connect flow waits until they agree before its first sync.
- There is no fallback from one to the other: a built-in debug token reaches the saved token only through the seed connection and never after a disconnect or a removal (007-repo-connect: RC-22).
- Saving a blank token saves none, which removes the token; the saved token can be read once or observed.
- The body text size is one of two values, normal and large, with normal as the default. The type is the domain's own and is mapped to the design system's text scale outside this module (`ui:mapper`).
- The study-log switch is read and set here; the rules it drives are in unit `study-log`.
- Every command except saving a token (a blank token becomes none) is a one-line pass-through to the repository.
### Public API
- `TokenRepository` — `currentToken()`, `saveToken(token)`, `observeToken()`; `AppliedTokenSource` — `applied()`.
- `SettingsRepository` — `observeFontScale()`, `setFontScale`, `observeStudyLogEnabled()`, `setStudyLogEnabled`.
- `FontScale` (`Normal`, `Large`, `Default`).
- `SaveTokenUseCase`, `ObserveTokenUseCase`, `ObserveAppliedTokenUseCase`, `SetFontScaleUseCase`, `ObserveFontScaleUseCase`, `SetStudyLogEnabledUseCase`, `ObserveStudyLogEnabledUseCase`.
### Not covered
- Where values are stored and the default of the study-log switch — `database:prefs`; masking a token — `settings:repo`.
- Applying the saved token to requests — `network:token` and `app:startup`.
