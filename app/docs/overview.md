---
id: app
name: App
aliases: [앱, 셸, 컴포지션 루트, composition root, shell, 앱 모듈]
uses: [common, design-foundation, design-system, domain, ai, navigation, data, network, database, ui, notes, connect, settings, quiz, stats, editor]
units:
  - {id: startup, name: Startup tasks, aliases: [앱 시작 작업, 토큰 동기, TokenSync, 학습 기록 정리, 요약 예약, Application], code: [src/main/java/com/hancekim/mynote/MyNoteApplication.kt, src/main/java/com/hancekim/mynote/TokenSync.kt, src/main/java/com/hancekim/mynote/InsightScheduleSync.kt, src/main/java/com/hancekim/mynote/StudyLogMaintenance.kt]}
  - {id: launch, name: Main activity and splash, aliases: [메인 액티비티, 스플래시, 설정 딥링크, 제목 글꼴 문, 런처 아이콘], code: [src/main/java/com/hancekim/mynote/MainActivity.kt, src/main/java/com/hancekim/mynote/SplashExit.kt, src/androidTest/java/com/hancekim/mynote/LiveTasksTest.kt, src/androidTest/java/com/hancekim/mynote/SplashWaitTest.kt, src/androidTest/java/com/hancekim/mynote/brand/]}
  - {id: share-in, name: Share receiver, aliases: [공유 받기, 트램펄린, ShareReceiverActivity, 공유 시트], code: [src/main/java/com/hancekim/mynote/ShareReceiverActivity.kt, src/test/java/com/hancekim/mynote/ShareRequestTest.kt]}
  - {id: download, name: Model download wiring, aliases: [내려받기 알림 배선, 대기 알림, 다시 시도 알림], code: [src/main/java/com/hancekim/mynote/download/]}
  - {id: di, name: Hilt wiring, aliases: [Hilt, 의존성 주입, 앱 스코프, 그래프], code: [src/main/java/com/hancekim/mynote/di/]}
  - {id: shell-state, name: Shell state and start decision, aliases: [셸 ViewModel, 시작 분기, 화면 정리, 기능 사이 신호, 종료 안내], code: [src/main/java/com/hancekim/mynote/shell/ShellViewModel.kt, src/main/java/com/hancekim/mynote/shell/ShellUiState.kt, src/main/java/com/hancekim/mynote/shell/ConnectStart.kt, src/main/java/com/hancekim/mynote/shell/ScreenCleanup.kt, src/main/java/com/hancekim/mynote/shell/ShellFeatures.kt, src/test/java/com/hancekim/mynote/shell/ShellViewModelTest.kt, src/test/java/com/hancekim/mynote/shell/ConnectStartTest.kt, src/test/java/com/hancekim/mynote/shell/HomeBodyTest.kt, src/test/java/com/hancekim/mynote/shell/NotesViewModelTest.kt, src/test/java/com/hancekim/mynote/shell/ConnectViewModelTest.kt, src/test/java/com/hancekim/mynote/shell/ConnectIssueTest.kt, src/test/java/com/hancekim/mynote/shell/FeatureSignalsTest.kt, src/test/java/com/hancekim/mynote/shell/FeatureFixture.kt]}
  - {id: shell, name: Shell frame and back order, aliases: [셸, 뒤로가기 7단, 드로어, 고정 패널, NavDisplay, 알림 권한, 스낵바 호스트], code: [src/main/java/com/hancekim/mynote/shell/NotesRoute.kt, src/main/java/com/hancekim/mynote/shell/NotesShell.kt, src/main/java/com/hancekim/mynote/shell/NotesNavDisplay.kt, src/main/java/com/hancekim/mynote/shell/NotesScenes.kt, src/main/java/com/hancekim/mynote/shell/NotesLayout.kt, src/test/java/com/hancekim/mynote/shell/ShellDestinationsTest.kt, src/test/java/com/hancekim/mynote/shell/NotesLayoutTest.kt, src/test/java/com/hancekim/mynote/shell/HeaderTest.kt, src/androidTest/java/com/hancekim/mynote/shell/]}
  - {id: shell-chrome, name: Floating chrome and reader effects, aliases: [모델 알약, 플로팅 내비, 맨 위로 버튼, 리더 진행률, ModelPill], code: [src/main/java/com/hancekim/mynote/shell/NotesOverlays.kt, src/main/java/com/hancekim/mynote/shell/ModelPill.kt, src/main/java/com/hancekim/mynote/shell/ReaderScrollEffects.kt, src/test/java/com/hancekim/mynote/shell/ModelPillTest.kt, src/test/java/com/hancekim/mynote/shell/ReaderScrollEffectsTest.kt]}
  - {id: build, name: Build, manifest and resources, aliases: [빌드, 매니페스트, 백업 규칙, BuildConfig, 릴리스, 시드, 통계 평가 하네스], code: [build.gradle.kts, proguard-rules.pro, src/]}
---
## Overview
`:app` is the composition root and the shell. It completes the Hilt graph, turns build values into injectable seeds, owns the two activities and the Android manifest, and draws the one screen that binds every feature: start branch, window-class decision, scaffold, drawer or fixed panel, floating navigation, model pill, `NavDisplay`, overlay slots and the back order.
It holds no feature screen and no feature ViewModel; those live in the six `:feature:*` modules, and pieces shared by two features live in `:core:ui`. Feature modules do not know the shell: they expose registration functions and slots, and the shell calls them.
The system touch points that need a manifest component or the process lifecycle also live here: notification intents, the share trampoline, the splash exit and the title-font gate.
Several things here stay alive only through one injected field in `MyNoteApplication`; deleting such a field compiles and runs but silently drops the behaviour (see `startup`).
## Structure
- `startup` is started from `MyNoteApplication.onCreate` and uses the `di` application scope; `download` adds the model notice observer to the same start and supplies the notification intents that open `launch`.
- `launch` hosts the shell (`shell`) in a Compose root and consumes the settings deep link that `download` notifications and `share-in` produce; `share-in` hands over through the inbox in `:core:ui`, not through intent extras.
- `shell-state` supplies the state and intents; `shell` composes the start branch, scaffold and back handlers in one file so the order stays fixed; `shell-chrome` is read by `shell` as overlays that sit outside the backdrop box.
- `di` binds the seeds and the implementations only this module can name; `build` holds build values, manifest, backup rules and the instrumented test harnesses (including the statistics evaluation).
```mermaid
flowchart LR
  startup --> di
  download --> di
  launch --> shell
  share-in --> launch
  download --> launch
  shell --> shell-state
  shell --> shell-chrome
  build -.-> di
```
