# Feature modules

Source: old feature document `docs/spec/feature-modules.md` (v3) as of commit c8c54350; work done 2026-10-11 … 12, editor module added 2026-10-14 by 013-local-notes.

## Plan
- why: All screens lived in one `:feature:notes` module (about 16k lines, 11 screens), so boundaries were only promises, one ViewModel held 27 dependencies and had no tests, and the back-press order depended on a file nobody guarded; the owner decided to split by feature, with ViewModel and back-order tests built first.
- flow: A change to one feature stays inside its module and cannot see another feature module (the compiler stops it); a new screen is a destination key in `:core:navigation` plus a registration in its feature module with no shell edit; moving between features goes by destination key or by a contract in `:core:ui`; ViewModels take time and wording by injection and run in JVM tests on virtual time.
- not doing: No change to what the user sees or does (wording is 011-screen-copy), no destination-key change and no quiz as a back-stack destination, no back-stack saving or process-death restore, no change to the data and domain boundaries of `:core:*`, no behaviour change of repo-connect, sync, model-download, note-format, brand and note-template, no Hilt instrumentation base and no new library.

## Items
| ID | What | Accept |
|---|---|---|
| FM-1 | Screens live in their feature module: `:feature:{notes, connect, settings, quiz, stats, editor}` and `:core:ui` exist, `:feature:notes` holds no connect, settings, quiz or stats screen, shared pieces live in `:core:ui` (tab and open-reader commands in `:core:navigation`) | Python `test_feature_modules.py`: `FM-1 모듈 지도의 모듈이 모두 등록돼 있다` · `FM-1 공용 조각은 core:ui 와 core:navigation 에 산다` · `FM-1 화면은 모듈 지도대로 산다` |
| FM-2 | Feature modules do not depend on each other | Python: `FM-2 기능 모듈은 서로를 의존하지 않는다`; `dependencies --configuration debugRuntimeClasspath` of each feature has no `project ':feature:` line and `:app` shows all six |
| FM-3 | Feature modules and `:core:ui` do not know the data layer or model implementations (data, network, database, ai, format) | Python: `FM-3 기능 모듈은 데이터 층을 모른다` |
| FM-4 | Each destination screen is registered by its feature module, one per key of `NotesNavKey`; the shell draws them passing only the shell frame values; reader scroll, source expansion and token draft survive a window-class change | JVM `ShellDestinationsTest`: `FM-4 모든 목적지 키에 화면이 하나씩 등록된다`; instrumented `FM-4 창 등급이 바뀌어도 리더 스크롤과 원문 펼침과 토큰 입력이 남는다` |
| FM-5 | The shell lives in `:app` and one file fixes the order: `NavDisplay`, the drawer, floating nav and the back handlers are only in `:app`; overlays, panels and handlers are not collected as an unordered set | Python: `FM-5 셸은 app 에 살고 순서를 한 파일이 고정한다`; no `@IntoSet` of overlay or handler types |
| FM-6 | Moves between features go by destination key and non-navigation signals by `:core:ui` contracts; eleven paths (stats to reader, retry quiz, connect entry, token cleanup, reader-end CTA to model download, permission re-read, quiz covers reader, quiz reads note facts, connection change clears, add-button hide, undo after study-log clear) happen as before | JVM `FeatureSignalsTest` (`FM-6 …` names) and `ConnectStartTest` (four `RC-23` tests); manual: notification permission request and toggling it |
| FM-7 | A ViewModel holds only its own feature's state and every one is obtained with the Activity as owner; exactly one class, `NotesViewModel`, takes the sync use case | Python: `FM-7 기능 ViewModel 은 Activity 를 owner 로 받는다` · `FM-7 동기화 UseCase 를 받는 클래스는 NotesViewModel 하나다`; JVM `FM-7 동기화는 구독이 여럿이어도 한 번 돈다` |
| FM-8 | ViewModels read neither the clock, logging, context nor string resources directly; all are built in JVM tests on virtual time | Python: `FM-8 ViewModel 은 시간과 문구를 직접 읽지 않는다` |
| FM-9 | Time-dependent ViewModel rules are tested: double back at root (exit inside the window, hint again outside), quiz auto-advance, quit and token and bookmark undo, a seen signal is not shown again | JVM `ShellViewModelTest` and the feature ViewModel tests: `FM-9 루트에서 창 안에 두 번 누르면 종료한다` · `FM-9 늦게 온 자동 진행은 문항을 건너뛰지 않는다` · `FM-9 본 신호는 다시 나오지 않는다` |
| FM-10 | The seven-step back order is kept by an instrumented test: drawer, quiz overlay, swallow during transition, pop, opening drawer, expanded fixed panel, root exit; with two steps pending only the higher one acts; token edit back cancels the edit | Instrumented `BackOrderTest`: `FM-10 열린 드로어가 먼저 닫힌다` · `FM-10 예측 뒤로가기 도중 전환 중이면 삼킨다` · `FM-10 겹치면 순위가 높은 쪽만 일어난다` · `FM-10 토큰 편집 중 뒤로가기는 편집을 취소한다` |
| FM-11 | Each string lives in the module that references it (one module: that module, two or more: `:core:ui`, common actions `ds_action_*`: design-system) with its prefix; no duplicate and no unused string except `ds_action_confirm` and `ds_action_retry` | `./gradlew lintDebug`; Python: `FM-11 문구는 참조하는 모듈 자리에 산다` · `FM-11 쓰지 않는 문구가 없다` · `FM-11 셸 문구는 shell_ 로 시작한다` |
| FM-12 | Tests, README and documents bound to wording find it by name in every module's `strings.xml` and follow moved names; no old name remains | `python3 -I -m unittest discover notes-tests` and `public-repos/tests` pass |
| FM-13 | Tests are moved keeping their names and the count does not fall: the 209 (class, name) pairs of base commit `9d6f0fd` exist in some module afterwards | Command: set difference of the base list and the later pairs is 0 |
| FM-14 | The whole verification passes: `ktlintCheck`, `:app:assembleDebug`, `test`, `lintDebug`, the three Python suites, instrumented tests per module and `:app:assembleRelease` | Command run of the one-shot verification of the root `AGENTS.md` |
| FM-15 | Documents know the new modules: each new module has an `AGENTS.md` and the root map, build-logic table, `app`, `feature/notes` and `core/navigation` guides match | Python: `FM-15 모듈마다 AGENTS.md 가 있다`; `test_hygiene` passes |
| FM-16 | The snackbar is received at one host at the `:app` root; a sync failure while on settings shows there and a seen signal does not reappear after folding; undo returns to the feature that raised it | `NotesSignalTest`, FM-9 tests; manual on the Fold emulator |
| FM-K1 | Every behaviour and test of repo-connect, sync, model-download, note-format, brand and note-template stays (names kept, passing in the moved module) | `test`, instrumented and Python suites |
| FM-K2 | The seven-step back order and dialogs receiving back in their own window stay | FM-10 and manual |
| FM-K3 | Floating-nav selection derives from the back stack and tab-switch rules stay | `TabSwitchTest`, `OpenReaderTest` |
| FM-K4 | Reader scroll, source expansion, token draft, quiz progress and stats period survive fold, unfold and rotation | FM-4 instrumented; manual on the Fold emulator |
| FM-K5 | The home to reader shared-element transition stays | Manual: card, reader, back |
| FM-K6 | The connect screen stays full width without a list column or header on wide windows (RC-14) | Existing RC-14 tests |
| FM-K7 | The raw token never appears in state, saved bundle or `toString` | `TokenDraftTest`, `TokenMaskTest`, `NotesDraftsTest` |
| FM-K8 | Only `NavDisplay` sits inside the `layerBackdrop` box | FM-5 and manual (glass nav and pill do not sample themselves) |
| FM-K9 | Stats aggregation and insights run only while that screen is on top | Manual: debug `MyNoteStats` log |
| FM-K10 | Sync does not run again from screen resubscription while the app is alive (SY-1) | FM-7 and existing SY tests |
| FM-K11 | One-shot signals (snackbar, undo) do not reappear after recreation | `NotesSignalTest`, FM-9, FM-16 |
| FM-K12 | The template entry area shows only when a template repository exists (RC-25) | Existing RC-25 tests |
| FM-K13 | External links open in a browser app (the module manifest's browser query stays in the moved module) | `ExternalLinkTest`; manual: token creation button |
