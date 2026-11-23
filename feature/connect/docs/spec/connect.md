---
units: [connect:flow, connect:issues, connect:entry, connect:confirm, connect:reading]
plans: [007-repo-connect, 008-note-template]
decisions: [RC-D53, RC-D57, RC-D59, RC-D63, NT-D31, NT-D32, DEC-21a]
---
## flow — Connect screen and flow
### Behaviour
- The screen is a stacked destination only, key `Connect`, opened from home's first-run `저장소 연결`, the home hint and settings `바꾸기`; the three share one state because the ViewModel is owned by the Activity. It is never a start result (007-repo-connect: RC-1).
- It draws its own top bar with the title and a back arrow and no refresh; the shell hides its own bar, floating navigation, drawer and pill while this screen is on top. The title is `저장소 바꾸기` when a connection exists and `저장소 연결` when none does (007-repo-connect: RC-14).
- The registration keeps the default content key, so the wide-window check recognises it as one pane; the screen takes the shell's scene edge, the token drafts, the snackbar host and the shell back (007-repo-connect: RC-14).
- Content is one column centred at most 560 dp wide, scrolling, with 16 dp sides, 12 dp top, 24 dp bottom and 24 dp between blocks (16 dp when the window height is compact); the horizontal and bottom safe insets are respected. When the keyboard opens the connect button is scrolled into view.
- The first block is an intro line, `저장소 주소` and a one-line link field with the hint `예: octo-learner/android-notes`: URI keyboard, autocorrect off, Go key. There is no paste button and no clipboard reading.
- Every edit of the link is read at once, without network, into a result under the field: a reason (neutral grey when the field is empty, warn color with a warn border otherwise) or `저장소 <owner/repo> · 브랜치 <branch> · 노트 위치 <folder>` (`기본 브랜치` and no label when no branch is named; `/` for the root folder) (007-repo-connect: RC-2, RC-3).
- `연결` is enabled only when not busy, the link was recognised and, when the token field is open, the token is not blank. Go on the keyboard does the same. Submitting hides the keyboard and drops focus (007-repo-connect: RC-3).
- Without the token field the check runs with no token even when one is stored; with it, the typed token (trimmed) is the candidate. Nothing is sent when the link is not recognised (007-repo-connect: RC-3, RC-4).
- While the check runs the field is locked and a `저장소 확인 중` line with a quiet `취소` shows (with the token field open, the line is `토큰 확인 중` inside the card). Cancel returns to editing with the link kept and no message (007-repo-connect: RC-24).
- Back while checking cancels the request; back or the top arrow during the first sync is ignored and the arrow is dimmed and disabled; otherwise back leaves to the previous screen (007-repo-connect: RC-24).
- A failed check is read by `issues`: it opens the token card, or shows a notice box, or nothing (a link the use case rejects just returns to editing) (007-repo-connect: RC-5, RC-7).
- The token card has a lock, the title `토큰이 필요합니다` (or `요청 한도를 넘었습니다` for a limit) and a short description, the three-line token guidance, `토큰 만들기` that opens GitHub's new fine-grained token page in a browser, the token field, the failure sentence of the last attempt and, for a `ghp_` token, a warning (007-repo-connect: RC-5, RC-7, RC-8).
- A failed attempt keeps the typed token and the link; a later submit clears the old failure sentence first. Editing the link closes the token card and the notice but keeps the token draft; edits are ignored while a check or sync runs (007-repo-connect: RC-5).
- When the check passes and nothing would be erased the first sync starts at once; when something would be erased the confirm dialog opens first (see `confirm`). Opening the dialog hides the keyboard and drops focus (007-repo-connect: RC-15).
- During the first sync the connect button is replaced by a card, `노트 불러오는 중`, with a progress bar and `끝나면 연결됩니다. 지금은 닫을 수 없습니다.` The link and token cannot be edited (007-repo-connect: RC-24).
- A failed first sync returns to editing with a token card or a notice, as read by `issues`; the connection and notes are unchanged on the screen's side (007-repo-connect: RC-6, RC-7).
- A completed connection clears the token draft and, 1.5 seconds later, resets the screen state.
- An unexpected exception in the flow is logged and shown as the transient notice, with the screen back in editing and any pending confirm dropped; it does nothing once the connection is done.
- When the connection goes from connected to not connected (a disconnect), the screen resets: link, token card, notice, dialog cleared, a running job cancelled, a reset count up so the shell clears the token draft once. A connection that never existed does not clear what the user typed (007-repo-connect: RC-17).
- The link is kept in saved state and survives process death; the token draft is not saved and survives only rotation and folding (007-repo-connect: RC-24).
- When no browser can be opened for `토큰 만들기` or an entry button, the snackbar says `브라우저를 열지 못했습니다. 브라우저 앱이 있는지 확인하세요.` and nothing else changes.
### Public API
- `ConnectRoute(hasConnection, drafts, snackbarHostState, onShellBack, modifier)` — the stateful screen with the Activity-owned ViewModel.
- `ConnectScreen(state, title, draft, onIntent, onBack, onCreateToken, ...)` — the stateless screen (used by previews).
- `ConnectViewModel` — Hilt ViewModel; `ConnectUiState` (`link`, `linkCheck`, `step`, `tokenPanel`, `notice`, `confirm`, `resetCount`, `template`, and `entryShown`), `TokenPanelUi`, `ConnectIntent` (`LinkChanged`, `Submit`, `CancelProbe`, `AcceptConfirm`, `DismissConfirm`).
- `connectDestinations(hasConnection, onShellBack)` — registers the `Connect` key over the shell frame.
- `ConnectStep` (`Editing`, `Probing`, `Syncing`, `Done`) · `ConnectBackAction` · `connectBackAction(step)` — checking cancels, syncing and done ignore, editing pops (007-repo-connect: RC-24).
### Guarantees
- Cancel while checking keeps input; input during the first sync is ignored; a 404 without token opens the token card with the link kept; a disconnect empties the screen once and a never-connected screen is left alone (`ConnectViewModelTest`) (007-repo-connect: RC-5, RC-17, RC-24).
- The back decision is fixed by `ConnectBackTest` (007-repo-connect: RC-24).
### Not covered
- Parsing a link, the check requests, the first sync, confirm and rollback — `domain` use cases, implemented in `data`.
- Link reasons and failure mapping — `connect:issues`; the entry section — `connect:entry`; the confirm dialog — `connect:confirm`.
- Hiding the shell's top bar, navigation and drawer while this screen is on top, and the exit hint — `app`.
- The home hero repository line (tested in `app`'s `ConnectViewModelTest`), connect band and hint — `notes`; disconnect, token editing and the repository card — `settings`.

## issues — Link checks and failure mapping
### Behaviour
- `linkCheckOf(link)` reads a pasted address with the domain parser and answers either a rejection with one of seven reasons (empty, not https, not a GitHub address, Gist, GitHub Pages address, a single file, owner only or no repository) or the repository, the branch if one is named, and the folder of the notes list (007-repo-connect: RC-2, RC-3).
- Each of the seven reasons has its own sentence, so no two reasons read the same (007-repo-connect: RC-3).
- The found result is the first candidate of the parser: `owner/repo`, its branch only when named (else null, shown as the default branch), and the list's folder, `/` for the repository root (007-repo-connect: RC-2).
- Only a found result can be submitted.
- A failed check becomes one of four screen reactions: ask for a token, a rate-limit prompt, a token-field failure (rejected, repository not visible, list not reachable), or a notice (no list, bad list, offline, transient) (007-repo-connect: RC-5, RC-7).
- Without a token, an unauthenticated 404 or an unseen repository asks for a token; with a token the same results are shown as `rejected` or `not visible` on the field. A token rejection is a field failure either way (007-repo-connect: RC-5, RC-7).
- A list that cannot be read with a token that sees the repository is the field failure `no list`. A rate limit is its own prompt (007-repo-connect: RC-7).
- A repository without a notes list is a notice, not a token problem; a broken list file is the bad-list notice; offline and a timeout or server error are the offline and transient notices and change nothing stored (007-repo-connect: RC-9, RC-10, RC-11).
- The first sync's failures map the same way: offline to the offline notice, token rejected to the field, access denied to the field (with a token) or the token ask (without), rate limit to the prompt, server to the transient notice, bad format to the bad-list notice (007-repo-connect: RC-6, RC-7).
- Notice wording: no list is this module's sentence (`이 저장소에는 노트 목록이 없습니다. 이 앱은 노트 목록이 있는 저장소만 읽을 수 있습니다.`); bad list, offline and transient are shared sentences from `ui`. Wording tables are in the design note `docs/design/repo-connect.md`.
### Public API
- `linkCheckOf(link)` returning `LinkCheck` (`Rejected(reason)`, `Found(repoName, branch, folder)`); `LinkCheck.canConnect`; `linkReasonRes(reason)`.
- `issueOfProbe(failure, hadToken)` and `issueOfSync(failure, hadToken)` returning `ConnectIssue` (`NeedsToken`, `Limit`, `TokenField(kind)`, `Notice(kind)`); `ConnectNotice` (`NoList`, `BadList`, `Offline`, `Transient`); `connectNoticeRes(kind)`.
### Guarantees
- Different token failures give different states, the limit gives the prompt, six sync failures map as above, notices are distinct, seven reasons are distinct sentences and the found result shows repository, branch and folder (`ConnectIssueTest`) (007-repo-connect: RC-2, RC-3, RC-5, RC-6, RC-7, RC-9, RC-10, RC-11).
### Not covered
- The parser itself and the probe failures — `domain`; what each HTTP answer means — `network` and `data`.
- Drawing the boxes — `ui:parts`; the token failure sentences — `ui:token`.

## entry — Template entry
### Behaviour
- The entry is a section below the link section for a person with no notes repository. It exists only when the app was built with a template repository value; without one the screen is exactly as without the entry and no way buttons appear (007-repo-connect: RC-25, RC-K7).
- Sections are always link first, then the entry; the link section is never rebuilt when the entry appears or goes. The entry never has a primary button (007-repo-connect: RC-25).
- The entry shows only while input can still be connected: editing, token card closed and no notice. It hides while checking, during the first sync, with the token card open, offline, on a transient error, a broken list or the no-list box. An open confirm dialog does not change it (007-repo-connect: RC-25).
- It reads `또는`, `처음이라면 저장소부터 만드세요`, a one-line description and a `저장소 만들기` button that opens and closes a guide (state `안내 펼침` or `안내 접힘`, remembered across rotation) (007-repo-connect: RC-25).
- The guide has three numbered steps (open the template, name it and press Create repository on GitHub, copy the address and paste it here), a line that a private repository leads to token guidance, and `템플릿 열기` (48 dp tall), which opens the template's generate address in an external browser. It opens and closes over 220 ms and, once open, scrolls its end into view (007-repo-connect: RC-25).
- Buttons keep a 48 dp touch area; the step list is announced as a list and the numbers are decoration; `템플릿 열기` and the two no-list ways are named as opening in an external browser (007-repo-connect: RC-25).
- The entry's buttons never change state and send no request: they only open a browser, and a missing browser shows a snackbar (007-repo-connect: RC-25).
- The no-list box (public repository, no list) gets a second line `방금 템플릿으로 만들었다면 잠시 뒤 다시 연결하세요.` and two ways, `저장소 만들기` (generate address) and `형식 안내 보기` (the template's introduction address), only when a template is configured; no other notice and no token-in-use list error has ways (007-repo-connect: RC-9).
- The ways sit outside the polite announcement of the box text.
### Public API
- `entryVisible(step, tokenPanelOpen, notice, templateAvailable)` · `connectSectionsOf(entryShown)` returning `ConnectSection` (`Link`, `Entry`) · `noticeWays(notice, templateAvailable)` returning `NoticeWays` (`None`, `CreateAndGuide`).
### Guarantees
- Visibility over the states, no entry without a template, link first, and the ways only for no-list with a template are fixed by `ConnectEntryTest`; the generate address ends in `/generate` and the introduction address is the repository address (`ConnectViewModelTest`) (007-repo-connect: RC-9, RC-25).
### Not covered
- Where the template value comes from, its validation and the generated addresses — `domain` (`TemplateRepo`) and the app's build configuration (007-repo-connect: RC-26).
- The template repository and its README — outside the app (008-note-template: NT-5).

## confirm — Change and take-over dialog
### Behaviour
- When the check passes and the use case says something would be erased, a dialog opens before the first sync; with nothing to erase there is no dialog (007-repo-connect: RC-15).
- Change (another repository replaces the connected one): `저장소를 바꿀까요?`, a body with the count of notes loaded from this repository and of bookmarks to remove, and that study records stay; a second sentence `저장된 토큰도 지웁니다.` when the token is dropped; the confirm is `바꾸기`, a solid destructive button (007-repo-connect: RC-15).
- Take-over (the first connection adopts notes loaded earlier): `저장소를 연결할까요?`, a body naming only the count of earlier notes that the new repository lacks, that their bookmarks go, and that bookmarks and records of notes that remain stay; the confirm is `연결`, a primary button (007-repo-connect: RC-19).
- The token line is shown for a change only, never for a take-over (007-repo-connect: RC-15, RC-19).
- The dialog calls the notes "notes loaded from this repository", never "notes of this device"; that phrase is for device notes.
- `취소` or leaving the dialog drops the pending connection and changes nothing; the confirm starts the first sync (007-repo-connect: RC-15).
- The dialog is a full-width window around the shared glass frame, at most 420 dp wide.
### Public API
- `confirmUiOf(replacement)` returning `ConfirmUi?` (`kind`, `notes`, `bookmarks`, `dropsToken`); `ConfirmKind` (`Switch`, `Adopt`).
### Guarantees
- No dialog for nothing to erase, counts carried over, the token line only when switching, and the adopt kind are fixed by `ConnectConfirmTest` (007-repo-connect: RC-15, RC-19).
### Not covered
- Counting what is erased, the confirm transaction and rollback — `domain` and `data`.
- The dialog frame — `ui:parts`; the disconnect dialog — `settings`.

## reading — Start reading screen
### Behaviour
- A centred column with the app name `MyNote`, a small progress ring and the caption `연결 정보 읽는 중`, inside the safe area on the app background.
- It is announced as one polite live region. It has no buttons and no back handling of its own.
- The shell shows it in place of itself while the start branch is still reading, that is, while the connection or the seed is not yet known (007-repo-connect: RC-1).
### Public API
- `ConnectReadingScreen(modifier)`.
### Not covered
- Deciding the start branch — `app`.
