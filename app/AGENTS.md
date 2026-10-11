# AGENTS.md — :app

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈의 역할

루트에 `MyNoteApplication`·`MainActivity`·`SplashExit`·`TokenSync`, `di/` 에 모듈 넷, `download/` 에 모델 내려받기 배선 둘,
`shell/` 에 셸이 있고 하는 일은 다섯이다 — **Hilt 그래프 완성**, **`BuildConfig`(debug 시드)/토큰 배선**,
**Activity 껍데기**(edge-to-edge + 스플래시 내리기 + 제목 글꼴 첫 프레임 문 + `NotesRoute` 호출),
**시스템과 닿는 지점**(알림 `PendingIntent`·딥링크·프로세스 라이프사이클), **셸**(여러 기능을 한 화면에 묶는 것 — 아래 절).
기능 화면 · 기능 ViewModel 은 여기에 넣지 않는다. 여기 파일이 늘어나는 건 "앱 전체에 하나뿐인 것" 이거나
"`PendingIntent`/매니페스트 컴포넌트가 필요한 것" 이거나 셸이 바뀔 때뿐이다.

## 셸 — `shell/`

`MainActivity` 가 부르는 `NotesRoute` 부터 셸 전부가 여기 있다(기능 문서 `feature-modules` 결정 2 · FM-5). 시작 분기 · 창 크기 판정 ·
`MyNoteTheme` · 루트 스낵바 호스트(FM-16) · 알림 권한 요청 · Scaffold · 드로어/고정 패널 틀 · 플로팅 내비 · 모델 알약 · `NavDisplay` ·
오버레이 자리 · 뒤로가기 7단 · 셸 ViewModel(`ShellViewModel` — 백스택 파생 · 종료 · 시작 분기 · 연결이 바뀔 때 화면 정리)이다.
셸은 모든 기능 모듈을 알 수 있는 유일한 자리라 여기 산다 — 기능 모듈은 셸을 모르고, 셸이 부르는 것만 공개로 내놓는다.
기능 ViewModel 과 화면 · 등록 파일은 그 기능 모듈에 있다(`feature/notes/AGENTS.md`).

셸 문구는 `res/values/strings.xml` 의 `shell_*` 다. `:app` 에는 `resourcePrefix` 를 걸지 않는다(걸면 런처 · 테마 · 백업 규칙 리소스
이름까지 바꿔야 한다 — 결정 16). 접두사는 파이썬 `FM-11 셸 문구는 shell_ 로 시작한다` 가 보고, 셸 파일 자리 · 순서 고정은
`FM-5 셸은 app 에 살고 순서를 한 파일이 고정한다` 가 본다.

### 뒤로가기 — **컴포즈 순서가 곧 우선순위다**(LIFO, 나중에 컴포즈된 게 이긴다)

파일을 옮기거나 쪼갤 때 가장 쉽게 깨지는 부분이다. 지금 순서는 아래와 같고, 위쪽이 먼저 먹는다.

| 순위 | 핸들러 | 어디서 컴포즈되나 |
|---|---|---|
| 1 | 드로어 닫기 (Material3) | `ModalDrawerSheet(drawerState = …)` 의 예측 뒤로가기 핸들러. 드로어가 열려 있는 동안 `NotesShell` 이 Scaffold 내용의 자식 디스패처를 꺼서 이 핸들러가 받는다 |
| 2 | 퀴즈 오버레이 닫기 | `NotesShell` 의 `QuizOverlay` (`NotesOverlays` 뒤 · Scaffold 내용의 맨 끝) |
| 3 | 전환 중 삼킴 | `NotesShell` 의 `BackHandler(enabled = !isNavSettled && !drawerEngaged) {}` |
| 4 | `NavDisplay` 의 pop | `NotesNavDisplay` |
| 5 | 여는 중인 드로어 닫기 | `NotesShell` 본문 |
| 6 | 펼친 고정 패널 접기 | `NotesShell` 본문 |
| 7 | 앱 종료 안내 → 종료 | `NotesShell` 본문 (`enabled = !canPop && !drawerEngaged && !collapsiblePanelOpen`) |

각각 그 자리에 있어야 하는 이유:

- **Scaffold 내용(NavDisplay · 삼킴 · 오버레이)은 자식 디스패처 안에 있다.** `NotesShell` 이
  `rememberNavigationEventDispatcherOwner(enabled = !(useDrawer && drawerState.isOpen))` 로 감싸고, 같은 값을 `SideEffect` 로
  자식 디스패처에 바로 넣는다 — 라이브러리는 `LaunchedEffect` 로 넣어 한 프레임 늦고, 그 틈에 열림 직후 pop 이 이기거나 닫힘 직후
  아무도 받지 않아 앱이 꺼진다(D36). M3 드로어 핸들러는
  본 컴포지션에서 등록되고 `NavDisplay` 는 Scaffold 내용(서브컴포즈)이라 나중에 등록돼, 셸이 처음부터 쌓인 스택으로
  만들어지면(접기 · 펴기로 창 등급 가지가 바뀔 때) pop 이 드로어를 이겼다. 드로어가 열려 있는 동안 자식을 통째로 끄면
  M3 핸들러가 늘림 연출과 함께 닫는다. 여는 중(`isOpen` 거짓)엔 자식이 켜져 있어 ⑤ 는 그대로다. `useDrawer &&` 를 빼면
  접힌 화면에서 드로어를 연 채 펼쳤을 때 열림으로 남은 상태가 넓은 창의 pop 을 막는다. 순서는 계측
  `src/androidTest/…/shell/BackOrderTest`(FM-10)가 지킨다 — 상태 없는 셸을 하네스로 띄워 단마다 누른다.

- **삼킴은 `NavDisplay` 호출 뒤에 둔다.** `NavDisplay` 는 예측 뒤로가기 애니메이션을 **이미 커밋한
  뒤** `onBack` 을 부른다. 앞에 두고 버리면 화면은 닫혔는데 스택은 그대로라 어긋난다.
  `activity-compose` 의 `BackHandler` 는 `NavigationEventDispatcher` 가 있으면 거기 등록되므로
  nav3 핸들러와 같은 LIFO 를 탄다.
- **드로어 판정은 `isOpen` 이 아니라 `drawerEngaged = isOpen || targetValue == Open` 이다.**
  `isOpen` 은 `currentValue` 이고 그 값은 `AnchoredDraggable` 이 애니메이션이 끝난 뒤에 넣는다.
  여는 동안은 `false` 인데 Material3 의 핸들러도 `enabled = drawerState.isOpen` 이라 같이 꺼져 있다 —
  그 틈으로 뒤로가기가 종료 안내로 샌다.
- **여는 중(`drawerOpening`)은 우리가 받아 닫는다.** 비활성만 시키면 그 구간에 아무도 받지 않아
  시스템으로 넘어가 앱이 그냥 꺼진다.
- **펼친 고정 패널도 뒤로가기를 먹는다.** 백스택 엔트리가 아니지만 Compact 의 드로어와 대칭이다.
  빠뜨리면 홈에서 패널을 펴 둔 채 뒤로가기 했을 때 종료 안내가 먼저 먹는다.

### 파일 — 호스트를 일 단위로 쪼갠 것

`NotesScreen` → `NotesStart`(시작 분기) → `NotesShell` 이고 그 안에서 조립된다. 한 파일이 한 가지만 한다:
순서가 뜻을 갖는 호출 전부(`NotesShell` — 아래) / 인셋 산수(`NotesLayout`) / 씬 전략(`NotesScenes`) /
등록 묶음과 `NavDisplay`(`NotesNavDisplay`) / 플로팅 내비·맨 위로·모델 알약(`NotesOverlays`·`ModelPill`).
스크롤과 "맨 위로" 판정(`NotesScrollState`) · 상단 바(`NotesTopBar`)는 연결 화면 · 등록 파일도 써서 `:core:ui` 의 `shell/` 에 있다.

**`NotesShell.kt` 한 파일이 순서를 고정한다(결정 12 · FM-5).** 시작 분기의 읽는 중 · 연결 루트(`NotesStart`), 모델 확인
대화상자, 뒤로가기 핸들러, 드로어 · 고정 패널의 목록 패널, 퀴즈 오버레이, 상단 바와 그 행동(헤더 슬롯)을 이 파일에서 컴포즈
순서대로 직접 부른다. 순서 없는 집합(Hilt `@IntoSet` 같은 멀티바인딩)으로 받지 않는다 — 받으면 퀴즈 오버레이(②)가 삼킴(③)보다
먼저 컴포즈될 수 있다.

**목적지 키 7개의 화면은 기능별 등록 파일이 등록한다(FM-4).** `:feature:notes` 의 `NotesDestinations.kt`(홈 · 북마크 · 리더) ·
`:feature:stats` 의 `screen/StatsDestinations.kt`(통계 · 지난 요약) · `settings/SettingsDestinations.kt`(설정) ·
`:feature:connect` 의 `screen/ConnectDestinations.kt`(쌓은 연결 화면). 셸 파일은 목적지 화면을 부르지 않고, `shellDestinations` 가 기능마다
자기 상태 · 입구만 넘겨 묶는다. 화면이 셸에게서 받는 틀 값은 셸 틀 계약 `:core:ui` 의 `frame/ShellFrame`(위 · 아래 · 끝 여백 · 두 단 · 쌓이는 씬 ·
스크롤 홀더 · draft · 스낵바 호스트)뿐이다. 기능 자기 상태 · 입구와 셸이 묶어 넘기는 화면용 값(연결 있음 · 셸 뒤로 · 리더 CTA 의
모델 상태 · 행동 · 알림 설정 열기 같은 것)은 틀 밖의 등록 함수 인자다(기능 문서 `feature-modules` 결정 13 · 18). 등록 틀(`:core:navigation` 의 `registry/`)이 모든 화면을 공유 전환 스코프로 감싸고
내용 키는 건드리지 않는다. 새 목적지를 더하면 키는 `:core:navigation`, 화면은 그 기능의 등록 파일, 시험은
`ShellDestinationsTest`(`FM-4 모든 목적지 키에 화면이 하나씩 등록된다`)의 키 목록이다.

- **등록 화면 · 슬롯은 자기 기능 상태만 읽는다.** 셸이 기능 상태마다 `rememberUpdatedState` 로 State 를 만들어 람다로 넘기고,
  화면은 그 안에서 읽는다. 다른 기능 상태(예: 모델 받기 진행)가 바뀌어도 그 엔트리 · 슬롯은 다시 그리지 않는다.
  entryProvider 는 `remember` 로 한 번 만든다 — 매 컴포지션 새로 만들면 엔트리가 셸 상태를 통째로 캡처하던 옛 꼴로 돌아간다.
- **틀 값은 화면 안에서 읽는다.** 등록 함수 본문(묶음을 만들 때 한 번 돈다)에서 읽으면 그때 값에 묶인다.
  알약 예약 여백은 `animateDpAsState` 의 State 를 틀이 들고 있어 300ms 동안 셸 본문이 아니라 여백을 읽는 화면만 다시 그린다.
- **목적지별 ViewModel 데코레이터를 넣지 않는다.** `NavDisplay` 에 `entryDecorators` 를 넘기지 않는다(기본값). 모든
  `hiltViewModel` 은 Activity owner 다(결정 7) — 창 등급이 바뀌어 `NavDisplay` 가 다시 만들어져도 산다.

쪼개거나 옮길 때 깨지는 것:

- **`layerBackdrop` Box 안에는 `NotesNavDisplay` 만.** 그 Box 가 유리의 샘플링 대상이므로
  오버레이(내비·맨 위로·퀴즈)가 안에 들어가면 유리가 자기를 샘플링한다. 그래서 `NotesOverlays` 와 `QuizOverlay` 는
  그 Box 의 **형제**다. 반대로 `layerBackdrop` 을 `SharedTransitionLayout` 에 직접 걸면 레이어가
  비어 효과가 통째로 사라진다.
- **씬 슬롯(`panel`/`header`/`panelExpanded`)은 `remember` 로 정체성을 고정한다.** 매 컴포지션
  새 람다가 들어가면 `sceneStrategies` 가 달라진 것으로 보여 씬이 통째로 재생성된다. 안의 값은
  `rememberUpdatedState` 로 읽는다(`NotesShell.kt` 의 `rememberNotesPanelSlot`/`rememberNotesHeaderSlot` 참조).
  manifest 에 `configChanges` 가 켜져 있어 Activity 가 재생성되지 않으므로 낡은 캡처는 실제 버그다.
- `nestedScroll(navScroll.nestedScrollConnection)` 은 `layerBackdrop` Box **바깥**의 Box 에 걸려 있다 —
  홈 그리드든 리더든 스크롤이 플로팅 내비 숨김 판정으로 올라와야 한다. 노트가 바뀌면
  `navScroll.reset()` 으로 다시 보여 준다.
- `NotesScrollState` 의 리더 스크롤은 `key(activeNoteId) { rememberScrollState() }` 다 — 노트마다
  스크롤 위치가 따로다. 리더 본문의 원문 펼침 상태(`readerContent` = design-system `NoteContentState`)도 같은 자리·같은 키다.
  **창 등급이 바뀌면(접기·펴기·회전) 셸이 `if (useDrawer) ModalNavigationDrawer(content = scaffold) else scaffold()` 의 다른
  가지로 옮겨 `NavDisplay` 가 통째로 다시 만들어진다** — 항목 안의 `remember`/`rememberSaveable` 은 키를 명시해도 그때 사라진다.
  창 등급을 넘어 남아야 하는 리더 상태는 여기(셸, 그 분기 위)에 둔다(관문 2 S1).

### 상단 알약은 콘텐츠를 가리지 않는다

`ModelPill` 이 보이는 동안 홈·리더의 스크롤 `contentTopPadding` 이 +54dp 된다(300ms).
`NotesShell` 이 `rememberModelPillVisible()` 을 한 번 계산해 **오버레이와 패딩 양쪽에 같은 값**을
넘긴다. 두 군데서 따로 계산하면 알약이 뜨는데 패딩이 안 밀리는 순간이 생긴다.

`퀴즈 모델 준비됨`(Ready)은 받는 중이던 상태에서 Ready 로 바뀔 때만 3.2초 뜬다(`readyJustArrived`). 처음 본 상태가
Ready 면(앱을 켬 · 재생성) 띄우지 않는다. VM 의 모델 상태 초깃값은 `Missing` 이다.

---

## Hilt — 그래프가 완성되는 유일한 자리

`@Module` 을 가진 모듈은 넷이다: `:core:data`·`:core:network`·`:core:database`·`:core:ai`.
그 넷을 동시에 아는 모듈은 `:app` 뿐이고, 의존 하나만 빠지면 Dagger 컴포넌트를 만드는
**`:app` 컴파일에서** 터진다(런타임이 아니다). `:core:domain`·`:core:navigation` 은 Hilt 플러그인조차 없다.

| 계약 (소유) | 구현 | `@Module` 위치 |
|---|---|---|
| `NotesRepository`·`TokenRepository` (domain) | `NotesRepositoryImpl`·`TokenRepositoryImpl` | `:core:data` |
| `NoteSourceAdapter` (data, `@IntoSet`) | `GithubManifestAdapter` | `:core:network` |
| `GithubTokenProvider`(network) + `AppliedTokenSource`(domain) | `GithubTokenHolder` — **한 인스턴스가 두 계약** | `:core:network` |
| `LocalNotesDataSource`·`TokenStore` (data) | `LocalNotesDataSourceImpl`·`DataStoreTokenStore` | `:core:database` |
| `QuizGenerator` (domain) | `LiteRtLmQuizGenerator` | `:core:ai` |
| `InsightScheduler` (domain) | `StudyInsightWork`(통계 워커 예약·상태) | `:core:ai` |
| `StudyLogRepository`·`InsightRepository` (domain) | `StudyLogRepositoryImpl`·`InsightRepositoryImpl` | `:core:data` |
| `ModelDownloadIntents` (ai) — `@BindsOptionalOf` 로 비워 둔 구멍 | `AppModelDownloadIntents` | **`:app`** (`di/DownloadIntentsModule`) |

**마지막 줄이 방향이 반대인 유일한 항목이다.** `:core:ai` 가 `@BindsOptionalOf` 로 `Optional<ModelDownloadIntents>`
를 선언해 두고, 그걸 채우는 `@Binds` 가 `:app` 에 있다. 알림의 `PendingIntent` 는 `:app` 의 `MainActivity` 를
가리켜야 하니 구현이 여기로 온다. **바인딩을 지우면 `Optional` 이 비어 실패 알림의 "다시 시도" 액션이 조용히
사라지고, 알림을 눌러도 설정이 아니라 런처 화면이 열린다** — 컴파일도 되고 내려받기도 된다. `TokenSync` 와 같은
종류의 조용한 실패다.

어느 모듈도 제공하지 않아 **`:app` 이 직접 `@Provides` 하는 셋**:

- **`CoroutineScope`** (`AppModule`) = `SupervisorJob() + Dispatchers.Default`, **한정자 없는 싱글턴**.
  `CoroutineScope` 를 주입받는 모든 곳이 이걸 받는다(현재 `TokenSync`·`Navigator`). 한정자가 없으니
  성격이 다른 두 번째 스코프가 필요해지는 순간 `@Qualifier` 를 붙여야 한다.
- **`Navigator`** (`NavigationModule`) — 시작 키(`NotesNavKey.Home`)와 스코프를 고르는 자리.
  `:core:navigation` 은 Hilt 를 모르는 leaf 라 조립이 여기로 온다. `@Singleton` + 앱 스코프라
  백스택이 Activity/ViewModel 보다 오래 산다(회전에도 유지). 대신 프로세스가 죽으면 사라진다 —
  저장/복원은 없다.
- **`ConnectionSeed?`·`TemplateRepo?`** (`RemoteConfigModule`) — `BuildConfig` 의 시드·템플릿 주소 필드를 읽어 넘긴다(비면 `null`).
  `:core:network`·`:core:data` 가 `BuildConfig` 를 모르게 하는 것이 이 모듈의 존재 이유다.

`:core:design-foundation`/`:core:design-system` 에는 `@Module` 이 없다. `:app` 코드가 참조하는 것은 스플래시 내려감의
떠남 곡선(`MyNoteExitEasing`)과 제목 글꼴 문(`font/` · `ProvideTitleFontFamily`)이다 — 나머지는 `:feature:notes`(feature convention) → design-system →
`api(design-foundation)` 로 이미 들어온다.

## 토큰 — 저장소에서 OkHttp 로 가는 유일한 길

```
DataStore(DataStoreTokenStore) → TokenRepository → ObserveTokenUseCase
    → TokenSync → GithubTokenHolder.update(...)
        ├─ AuthInterceptor 가 요청마다 currentToken() 을 읽어 Authorization: Bearer 를 붙인다
        └─ 같은 홀더가 AppliedTokenSource 로서 NotesViewModel.appliedToken 을 흔들고, 그게 sync 를 다시 돌린다
```

**`holder.update(...)` 를 부르는 코드는 `TokenSync` 하나뿐이고, `TokenSync` 를 붙잡는 참조는
`MyNoteApplication` 의 `@Inject lateinit var tokenSync` 한 줄뿐이다.** 아무도 주입받지 않는 클래스라
그 필드를 지우면 Hilt 가 인스턴스를 만들지 않는다 — 컴파일도 통과하고 앱도 켜지지만 홀더가 영원히
`null` 이라 private 레포 요청이 전부 인증 없이 나간다. 조용히 깨지는 종류다.

`ModelWaitNotice` 도 같은 모양이다 — `@Inject lateinit var modelWaitNotice` + `onCreate` 의 `start()`
한 줄이 유일한 참조이고, 지우면 앱을 나갈 때 대기 · 재시도 알림이 안 뜨고 알림의 `취소` 뒤에 그 알림이 남는다 —
둘 다 조용히.

`StudyLogMaintenance` 도 같다 — `@Inject lateinit var studyLogMaintenance` + `onCreate` 의 `start()` 가 앱 시작마다
365일 지난 학습 기록과 26주 지난 요약·패턴을 지우는 유일한 자리다. 지우면 컴파일·실행은 되고 기록만 무한히 쌓인다.

`InsightScheduleSync` 도 같다 — `@Inject lateinit var insightScheduleSync` + `onCreate` 의 `start()` 가 "모델 Ready &&
학습 기록 on" 을 관찰해 통계 워커(`study-insights`)를 예약/취소하는 유일한 자리다. 지우면 컴파일·실행은 되고 **주간 요약이
영원히 생기지 않는다**(통계 카드가 자리째 없는 것과 구별이 안 된다). 앱 시작마다 `schedule()` 을 부르지만 같은 사양(태그
`study-insights-spec-N`)의 작업이 이미 살아 있으면 아무것도 하지 않는다(`:core:ai` `StudyInsightWork`).

**`TokenSync` 에 폴백이 없다 — 저장된 토큰만 holder 로 올린다**(결정 23). 끊기·토큰 지우기 뒤에 빌드 내장 토큰이 되살아나지
않게 하려는 것이다. debug 내장 토큰은 `ConnectionSeed` 로 시드 연결을 할 때만 쓰이고(`ConsumeSeedUseCase`), 그때 저장 칸에
쓴 뒤 적용되고 나서 첫 동기화가 돈다. 이미 저장된 토큰이 있으면 덮어쓰지 않는다(결정 14 · RC-22).

### BuildConfig 필드 — debug 첫 실행 시드 + 템플릿 주소

저장소 연결은 앱 화면(`:feature:connect` 연결 화면)에서 한다. 빌드가 넣는 값은 **둘**이다: **debug 첫 실행 시드**와
**템플릿 저장소 주소**(RC-26). `RemoteConfigModule` 이 시드를 `ConnectionSeed?`(`GITHUB_OWNER`·`GITHUB_REPO` 둘 다 채워졌을 때만),
템플릿 주소를 `TemplateRepo?`(`parseTemplateRepo(BuildConfig.TEMPLATE_REPO)` — 모양이 틀리거나 비면 `null`)로 만든다.
필드는 여섯: `GITHUB_OWNER`·`GITHUB_REPO`·`GITHUB_BRANCH`·`GITHUB_TOKEN`·`TEMPLATE_REPO`·`TITLE_FONT_NAME`(아래 제목 글꼴 절).

- **`TEMPLATE_REPO` 는 `local.properties` 가 아니라 커밋되는 루트 `gradle.properties` 의 `template.repo` 에서 온다**(결정 54).
  `defaultConfig` 에서 한 번만 넣는다 — `debug{}`·`release{}` 에 쓰지 않으므로 둘이 같은 값이고 release 의 시드 비우기와 겹치지 않는다.
  공개 전에는 빈 값(= 입구 없음). 허용 문자 `[A-Za-z0-9._/-]` 밖(따옴표·역슬래시·공백·개행)이 든 값은 빌드가 깨지지 않고 빈 값으로
  내려간다 — 이건 `BuildConfig` 문자열을 깨지 않게 하는 안전판일 뿐이고 모양 판정은 `:core:domain` 의 `parseTemplateRepo` 한 곳이다.
  `-Ptemplate.repo=…` 로 한 번만 덮어 쓸 수도 있다.

- **`defaultConfig` 는 넷 다 빈 문자열이다 — 코드에 기본 저장소가 없다**(RC-19 d). `local.properties` 에 키가 없으면
  시드 없음이고 첫 실행은 연결 화면이다.
- **`debug` 블록만 `local.properties` 의 `github.owner`·`github.repo`·`github.branch`·`github.token` 으로 덮어쓴다.**
  브랜치가 비면 저장소 기본 브랜치다.
- **`release` 블록은 넷을 항상 빈 문자열로 못 박는다**(결정 22) — `local.properties` 에 값이 있어도 공개 APK 에는 시드·토큰이
  들어가지 않는다. 그래서 release 로 처음 켜면 연결 화면이 뜨는 게 정상이다. 이 줄을 지우면 `defaultConfig` 가 바뀔 때
  시드가 release 로 샐 수 있다.
- `local.properties` 는 gitignore 되고, 키 이름만 안내한다: `github.owner`·`github.repo`·`github.branch`·`github.token`
  (release 서명 키는 아래 release 절). 값을 문서·커밋·로그에 적지 않는다.
- `buildFeatures { buildConfig = true }` 가 필요하다(AGP 9 기본 off). 이 한 줄이 빠지면 `BuildConfig`
  자체가 사라져 `RemoteConfigModule` 이 깨진다.
- `local.properties` 를 configuration 단계에 읽지만 Gradle 이 설정 입력으로 추적한다 — 내용을 바꾸면
  configuration cache 가 무효화되고 `BuildConfig` 가 다시 생성된다(확인함). 값을 바꿨는데 안 먹는
  현상을 캐시 탓으로 보지 말 것.
- 확인: `assembleDebug`·`assembleRelease` 뒤 `app/build/generated/source/buildConfig/<변형>/…/BuildConfig.java` 에서 네 필드가
  비었는지만 본다(값을 출력하지 않는다). release 는 전부 비어야 한다.

## Activity / Manifest

`MainActivity` 는 `super.onCreate` 앞에서 `enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT), …)` 를
한 번 부르고, 같은 태스크에 이전 화면이 살아 있지 않으면 스플래시 내려감 리스너를 걸고, 설정 딥링크 인텐트를 한 번 소비하고,
제목 글꼴 첫 프레임 문을 건 뒤 `setContent { ProvideTitleFontFamily(…) { NotesRoute() } }` 만 한다(`NotesRoute` 는 `shell/`).
`MyNoteTheme` 은 화면에서 시작한다 — Activity 는 Compose 테마를 모른다.

- **시스템바 아이콘 명암을 여기서 고정하는 이유:** 앱은 다크 단일 테마인데 XML `Theme.MyNote` 의 부모는
  `android:Theme.Material.Light.NoActionBar` 다(다크 팔레트는 Compose 안에서만 시작한다). 한 번에 고정해 두기 때문에
  "화면에서 `statusBarsPadding` 을 수동으로 더하지 않는다"는 규칙이 성립한다.
- **창 바탕은 `@color/window_background`(= 토큰 `bg`)다**(`brand` BR-4). 스플래시가 내려간 뒤 Compose 가 첫 프레임을
  그리기 전에 보이는 색이라, 부모 테마의 라이트 바탕으로 두면 흰 프레임이 낀다. 토큰 `bg` 를 바꾸면 이 값도 같이 바꾼다
  (`notes-tests/test_brand_resources.py` 가 둘을 대조한다).
- `android:enableOnBackInvokedCallback="true"` — 예측 뒤로가기용(커밋 `f97d89d`).
- `android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboard|keyboardHidden"`
  — 회전·분할에서 Activity 가 재생성되지 않는다. 그래서 씬 슬롯의 낡은 캡처가 실제 버그가 되고,
  셸(`shell/NotesShell.kt`)이 `remember` + `rememberUpdatedState` 로 막고 있다. 이 속성을 지우면 그쪽 전제가 바뀐다.
- **`<uses-native-library>` 두 줄(`libvndksupport.so`·`libOpenCL.so`, `required="false"`)은 LiteRT-LM 의
  GPU 백엔드용이다.** 없으면 예외도 로그도 없이 CPU 로 떨어지고, 판정은 TTFT 로만 가능하다(커밋 `c522d30`).
  매니페스트 정리하다 지우기 쉬운 자리다.
- **백업 규칙(`res/xml/data_extraction_rules.xml`)은 토큰이 든 DataStore 둘과 모델을 클라우드 백업과 기기 이전 둘 다에서
  뺀다.** DataStore 는 `datastore/github_token.preferences_pb` 와, 연결 시도 전 토큰을 담는
  `datastore/connection_attempt_token.preferences_pb` 다. 모델은 `external` 전체와, 외부 저장소가 없을 때 `filesDir` 에
  생기는 `gemma-4-E2B-it.litertlm`·`.part`·`.plan` 이다. `minSdk` 32 라 이 파일만 읽히고 `backup_rules.xml` 은 같은 규칙의 사본이다.
  `<device-transfer>` 구역이 없으면 그 모드는 전부 옮긴다. 모델이 한 번이라도 클라우드 백업 대상에 들어가면 앱별 한도
  25MB 를 넘어 **그 앱의 백업 전체가 조용히 멈춘다** — 북마크·학습 기록도 같이 안 올라간다(기기 이전 한도는 2GB 라
  모델이 넘는다). `include` 를 하나라도 쓰면 나머지가 전부 빠지므로 `exclude` 만 쓴다. DataStore 이름이나 모델 파일
  이름을 바꾸면 이 파일도 같이 고친다.
- 매니페스트에 `<receiver>` 가 없다. 알림이 여는 앱 컴포넌트는 `MainActivity` 하나다(아래 `download/` 절).

### 설정 딥링크 — 중복 push 를 막는 세 겹

알림을 누르면 `MainActivity` 가 `EXTRA_OPEN_SETTINGS`(`"open_settings"`) 를 읽어
`navigator.dispatchImmediately(NavCommand.Push(NotesNavKey.Settings))` 를 한다. 같은 탭이 두 번 쌓이지
않는 이유가 셋이다 — 하나만 남아도 동작하지만 왜 그렇게 생겼는지는 알아둘 것:

1. **`intent.removeExtra` 를 읽은 직후에 부른다.** `onCreate` 와 `onNewIntent` 가 같은 `Intent` 객체를
   보게 되는 경로에서 두 번째 호출이 죽는다.
2. **`settingsIntent()` 의 플래그가 `NEW_TASK or CLEAR_TOP or SINGLE_TOP` 이다.** `launchMode` 는
   기본값 `standard` 그대로인데, `CLEAR_TOP` 만 주면 standard Activity 는 **파괴되고 다시 만들어진다**
   (`onNewIntent` 가 아니라 `onCreate`). `SINGLE_TOP` 을 같이 줘야 살아 있는 인스턴스에
   `onNewIntent` 로 들어온다. 둘 중 하나를 빼면 조용히 Activity 재생성으로 바뀐다.
3. **`Navigator.reduce` 가 `current.lastOrNull() == command.key` 면 `null` 을 돌려준다.**
   `Settings` 는 `data object` 라 동등성이 성립해서 이미 설정 위에 있으면 push 자체가 무효가 된다.

`push()` 대신 `dispatchImmediately` 를 쓴다 — `dispatch` 는 `Throttler` 를 거쳐 방금 화면을 옮겼으면
`NavResult.Throttled` 로 **버려진다.** 알림 탭은 버려지면 안 되는 입력이다.

`ACTION_OPEN_SETTINGS` 라는 전용 액션을 쓰는 이유는 `:core:ai` 의 `openApp()` 이 만드는
런처 `PendingIntent`(request code 0)와 `filterEquals` 로 구분되게 하려는 것이다. 액션이 같으면
`FLAG_UPDATE_CURRENT` 가 서로의 extra 를 덮어쓴다.

### 런처 아이콘 · 스플래시 — 리소스와 내리기

그림의 정본은 `labs/brand-design/app/svg/` 이고 값 · 움직임은 `docs/design/brand.md` 다. 여기 리소스는 그 SVG 를 손으로 옮긴
것이고 `notes-tests/test_brand_resources.py` 가 path 이름(= SVG id)별로 경로 글자 · 선 · 그라디언트 좌표 · 색을 대조한다.
그림을 바꾸면 SVG 와 리소스를 같이 고친다.

- **런처** — `mipmap-anydpi/ic_launcher{,_round}.xml` 이 같은 세 층(`ic_launcher_background` · `_foreground` ·
  `_monochrome`)을 가리킨다. `minSdk` 32 라 밀도별 비트맵은 두지 않는다. monochrome 은 전경과 다른 전용 그림이다(테마 아이콘).
  창틀 선 색은 토큰 `accent` 값을 리소스에 그대로 적은 것이다 — 시험이 토큰과 대조한다.
- **스플래시** — 플랫폼 SplashScreen API(새 의존성 없음). 테마의 `windowSplashScreenBackground`(밤) ·
  `windowSplashScreenAnimatedIcon`(`drawable/splash_icon` AVD) · `windowSplashScreenAnimationDuration`(AVD 길이와 같게 616 = 꺼진 채 166 + 불 켜짐 450).
  알림으로 시작할 때도 플랫폼 기본 그대로다(`windowSplashScreenBehavior` 안 씀).
- **AVD 의 기본 상태는 끝 그림(불 켜짐)이다.** 감속 모션 · 움직임을 못 돌리는 판에서 시스템이 기본 상태를 그리기 때문이다.
  처음 166ms(플랫폼이 허용하는 시작 지연 상한)는 `startOffset` 이 아니라 같은 값 두 keyframe 으로 꺼진 채 붙잡는다 — `startOffset`
  이면 그 동안 기본 상태(켜짐)가 그려져 스플래시 첫 프레임에 불이 이미 켜져 있었다(에뮬레이터 녹화). 시스템은 창이 다 열리기 전에
  움직임을 시작해, 앞의 꺼진 창은 창이 커지는 전환과 겹친다 — 붙잡기를 상한까지 늘린 이유다.
  두 `objectAnimator` 의 보간기는 선형이어야 한다 — 빠지면 기본 AccelerateDecelerate 가 keyframe 시각과 곡선을 함께 휜다(시험이 본다).
- **내리기는 앱이 한다** — `SplashExit.kt` 의 `fadeOutAfterIcon()`. `iconAnimationStart + iconAnimationDuration × 애니메이터 배율`
  까지 남은 시간을 기다렸다가 스플래시 뷰와 아이콘 뷰를 함께 알파 0 으로(180ms · `MyNoteExitEasing`) 내리고 `remove()` 한다.
  아이콘은 별도 `SurfaceView` 로 넘어와 부모 알파만으로는 안 사라질 수 있어 둘 다 내린다. 리스너를 지우면 시스템 기본 내려감이 돈다.
  리스너는 앱 첫 프레임 뒤에 불리므로 데이터를 기다리며 붙잡지 않는다(`brand` 결정 10) — 시작 판정이 남았으면 읽는 중 화면이 이어진다.
  기다림 계산은 순수 함수 `splashWaitMillis`(시작 · 길이 · 배율 · 지금 → 기다림)다.
- **같은 태스크에 끝나 가는 이전 `MainActivity` 가 살아 있으면 리스너를 걸지 않는다**(`LiveTasks` · `onCreate` 에서 적고 `onDestroy`
  에서 지운다). `--activity-clear-task` 웜 시작이 그 경우다 — 이전 화면이 스플래시 도중 파괴될 때 시스템이 앱에 넘긴 아이콘 표면을
  태스크 단위로 거둬 가, 마크가 사라지고 빈 밤 바탕만 내려감까지 남았다(에뮬레이터 녹화 · 다른 태스크의 이전 화면이면 안 사라짐).
  리스너가 없으면 넘김 자체가 없고 시스템이 앱 첫 프레임에 앱 메인 스레드와 상관없이 내린다 — 그 경로는 불 켜짐이 덜 끝났어도 내려간다.
  카운터를 지우면 컴파일 · 콜드 시작은 그대로이고 그 웜 시작에서만 빈 바탕이 다시 남는다.
- 핫 시작(살아 있는 화면으로 돌아옴) · 최근 앱에서 돌아옴에는 스플래시가 없다(플랫폼). 안드로이드 12+ 는 루트 화면에서 뒤로가기를
  눌러도 화면을 끝내지 않아 다시 켜면 핫 시작이다.

### 제목 글꼴 — 첫 프레임 문

제목 글꼴 스타일만 Gothic A1 을 GMS 글꼴 제공자에서 받는다(`brand` BR-8 · 판단은 `:core:design-system` `font/`). `MainActivity` 가 하는 것:

- **프로세스마다 한 번 정한다.** companion 의 `TitleFontGate` 하나가 처음 결정(Gothic A1 또는 기기 글꼴)을 들고 있고, 재생성 · 웜 시작은
  그 값을 그대로 쓴다. 받지 못했거나 늦었으면 그 프로세스 내내 기기 글꼴이다 — 제공자는 뒤에서 받기를 마저 끝내 다음 프로세스부터 빠르다.
- **받기는 `onCreate` 맨 앞에서 시작하고, 첫 프레임을 그릴 수 있게 된 뒤 불 켜짐이 남은 만큼만 더 기다린다.** 불 켜짐 끝 =
  실행 요청 시각 + 테마 `windowSplashScreenAnimationDuration`(AVD 길이와 같다 — BR-5 시험) × 애니메이터 배율. 실행 요청 시각은 프로세스가
  이 실행으로 막 떴으면(화면을 만들기까지 5초 안) 프로세스 시작 요청(33+ `Process.getStartRequestedUptimeMillis()` · 32 `getStartUptimeMillis()`),
  아니면 `onCreate` 다. 첫 프레임이 불 켜짐 끝보다 늦으면 그 사이에 온 글꼴을 쓰고 더 기다리지 않는다 — 어느 쪽이든 글꼴이 스플래시를 더
  붙잡지 않는다(`brand` 결정 10). 감속 모션이면 불 켜짐이 0 이라 첫 프레임까지 온 글꼴만 쓴다.
- **제공자의 답은 메인이 아니라 따로 둔 루퍼 스레드로 받는다**(`GoogleTitleFont.kt`). 메인으로 받으면 첫 컴포지션이 끝날 때까지 답이 줄을 서
  늘 첫 프레임에 진다 — 부하 큰 에뮬레이터에서 받아 둔 글꼴(제공자 응답 약 30ms)도 기기 글꼴이 됐다. 판정도 `Dispatchers.Default` 에서 한다.
- **화면은 바로 짓고 그리기만 막는다.** `setContent` 는 곧바로 해 ViewModel · 시작 판정이 늦지 않고, `OnPreDrawListener` 가 그리기를 막다가
  정한 글꼴로 컴포지션이 적용되면(`SideEffect`) 연다. 처음 막힌 그리기가 "첫 프레임을 그릴 수 있게 됨" 신호다. 정하기 전 컴포지션은 기기 글꼴이고
  그려지지 않는다 — 그래서 한 화면에서 글꼴이 바뀌는 프레임이 없다. 이미 정한 프로세스의 재생성은 첫 컴포지션에서 바로 연다(스플래시 없는
  재생성에서 빈 창을 붙잡지 않는다).
- **받지 못하는 경우를 보려면** `./gradlew :app:installDebug -Ptitle.font=<없는 이름>`. debug `BuildConfig.TITLE_FONT_NAME` 에만 들어가고
  release 는 늘 빈 값(= 정식 이름)이다. `gradle.properties` 에 키를 두지 않는다.
- 글꼴 파일은 APK 에 없다(`notes-tests/test_brand_font.py` 가 소스를, 검수가 APK 목록을 본다).

## `download/` — 시스템이 앱을 깨우는 두 자리

`:core:ai` 는 내려받기와 알림 판단을 전부 알지만 알림이 열 액티비티와 프로세스 라이프사이클을 모른다.
그 둘이 여기 있다. 알림 판단(상태 → 알림 하나 · 사건 → 알림판)은 `:core:ai` `download/work/DownloadNotices.kt` 다.

- **`AppModelDownloadIntents`** — 알림이 여는 `PendingIntent` 둘을 만든다. 본문 탭 = `MainActivity.settingsIntent`
  (설정 열기), 실패 알림의 `다시 시도` 액션 = `MainActivity.retryIntent`(설정 열기 + `EXTRA_RETRY_DOWNLOAD`).
  **다시 시도는 리시버가 아니라 액티비티로 간다**(11ab42b 부터 — 매니페스트에 receiver 가 없다).
  `MainActivity.consumeSettingsRequest` 가 설정을 push 한 뒤 `ModelDownloads.start()` 를 부른다.
  `ModelDownloadWorker.enqueue` 가 `ExistingWorkPolicy.KEEP` 라서 끝난 작업에는 새 요청이 들어가고
  돌고 있는 작업은 건드리지 않는다 — `start()` 하나가 "시작" 과 "다시 시도" 를 겸하는 근거다.
  **액션 탭에는 autoCancel 이 안 먹는다**(본문 탭만) — 실패 알림은 `start()` 가 결과 자리를 지워서 사라진다.
  `FLAG_IMMUTABLE` 필수 — API 31+ 는 `FLAG_IMMUTABLE`/`FLAG_MUTABLE` 중 하나가 없으면 `IllegalArgumentException`
  이고 이 앱의 `minSdk` 는 그보다 위다. request code 는 `:core:ai` 의 `0` 과 겹치지 않게 `4211`/`4212`.
- **`ModelWaitNotice`** — 이름은 대기 알림이지만 쉬는 동안(Wi-Fi 대기 · 재시도)의 알림을 맞추는 자리다. 둘을 한다.
  - `ProcessLifecycleOwner` 의 `ON_STOP` 에서 `ModelDownloads.notifyResting()` — Wi-Fi 대기면 대기 알림, 재시도면
    재시도 알림을 올리고 그 밖은 건드리지 않는다. **쉬는 동안(`ENQUEUED`)에는 워커가 돌지 않아 포그라운드 알림이
    없다** — 앱을 나가면 아무 표시 없이 기다리는 구멍을 메운다. 앱이 앞으로 와도 지우지 않는다(알약과 겹쳐도 됨).
  - `start()` 에서 `status()` 를 모아 `ModelDownloads.settleNotices(status)` 로 넘긴다 — 없음 · 멈춤이면 내려받기
    알림을 모두 지운다. **쉬는 중 알림의 `취소` 액션은 WorkManager 의 `createCancelPendingIntent`
    (`SystemForegroundService`)로 바로 가서 앱 코드가 한 줄도 안 돈다** — 이 관찰이 없으면 취소 뒤 대기 · 재시도
    알림이 남는다. 액션이 프로세스를 깨우면 `Application.onCreate` 에서 다시 붙는다. 작업이 있는 상태에서는 지우지
    않는다 — 상태 관찰은 늦게 올 수 있어 빨리 끝난 작업이 막 올린 결과 · 재시도 알림을 지울 수 있다. 그동안의
    알림판은 워커와 `start()`·`cancel()`·`deleteModel()` 이 차례대로 맞춘다.
  - `TokenSync` 처럼 `MyNoteApplication.onCreate` 의 `start()` 한 줄이 유일한 참조다. 끝난 작업 기록을 WorkManager 가
    정리하면(기본 하루) 실패가 멈춤 · 없음으로 보여 남은 실패 알림도 그때 지워진다.

## 네이티브 라이브러리 / 모델 파일

`ndk { abiFilters += listOf("arm64-v8a") }` — litertlm 엔진 `.so` 가 ABI 마다 한 벌씩 들어와 기본값이면
4벌이 실린다(커밋 `92d4bfd`):

| ABI | 크기 | |
|---|---|---|
| arm64-v8a | 20.8MB | 실기기·설치된 에뮬레이터 이미지가 전부 이것 |
| x86_64 | 24.8MB | 쓸 기기가 없다 |
| armeabi-v7a / x86 | 각 0.02MB | litertlm `.so` 자체가 없어 못 돌린다 |

APK 85.4MB → 60.6MB. **되돌릴 조건은 하나다: 인텔 맥에서 x86_64 에뮬레이터를 쓰게 될 때** — 그때는 이
`ndk` 블록을 지우면 원래대로 돌아온다. 지금 상태로 x86_64 에뮬레이터에 설치하면 엔진 `.so` 가 없는 APK 가
올라간다.

**모델 파일은 APK 에 없다.** `:core:ai` 의 `ModelEngine` 이
`getExternalFilesDir(null)/gemma-4-E2B-it.litertlm` 를 보고, 없으면 `isAvailable()` 이 false 라 퀴즈 기능이
숨는다(기능이 안 보이면 먼저 이 파일을 의심한다).

- 기기 경로: `/sdcard/Android/data/com.hancekim.mynote/files/gemma-4-E2B-it.litertlm` (약 2.59GB)
- `adb push` 로 따로 넣는다. `installDebug` 재설치로는 안 날아가지만 uninstall 하면 사라진다.

## release 빌드

`isMinifyEnabled` + `isShrinkResources` + `proguard-android-optimize.txt` + `proguard-rules.pro`.

- **keep 규칙을 미리 넣지 않는 것이 방침이다.** Retrofit/OkHttp/Room/Hilt/kotlinx.serialization 은 각자
  consumer rules 를 아티팩트에 담아 온다. 실제로 빠진 게 있으면 R8 이
  `build/outputs/mapping/release/missing_rules.txt` 를 남기므로 그때 필요한 것만 추가한다. 지금 파일에
  있는 건 스택트레이스 복원용 `-keepattributes SourceFile,LineNumberTable` +
  `-renamesourcefileattribute` 뿐이다.
- `src/main/keepRules/rules.keep` 는 AGP 가 이 디렉터리의 규칙 파일을 모아 R8 에 넘기는 자리인데 현재는
  템플릿 주석뿐이다. 규칙을 추가할 곳은 `proguard-rules.pro` 다.
- **release 서명은 이 파일에 없다.** convention(`build-logic/.../SigningConfig.kt`)이 `local.properties` 의
  `keystore.path`·`keystore.password`·`key.alias`·`key.password` **네 개가 모두 있을 때만** 붙인다.
  넷 다 없으면 조용히 넘어가고, 하나만 빠지면 경고만 찍고 서명 없이 빌드된다 — `BUILD SUCCESSFUL` 을
  서명 성공으로 읽지 말 것.

## 여기서 정하지 않는 것

- `compileSdk`·`minSdk` 는 `KotlinAndroid.kt`, `targetSdk` 는 application convention 이
  `libs.versions.toml` 에서 넣는다. `android { }` 에 다시 쓰지 않는다.
- `versionCode`·`versionName` 은 `libs.versions.appVersionCode`/`appVersionName` 을 읽는다 — 올릴 때
  고치는 곳은 카탈로그다.
- Compose BOM·material3·activity-compose·compose lint 는 `.compose` convention 이 넣는다. `:app` 의
  `dependencies` 블록은 모듈 목록 + `core-ktx`·`lifecycle-runtime`·`lifecycle-process`·
  `work-runtime`·`hilt-work` + 셸의 `hilt-navigation-compose`·`lifecycle-runtime-compose` + 시험의 `junit`·`coroutines-test` 다.
  `lifecycle-process` 는 `ProcessLifecycleOwner` 때문에, `work-runtime`/`hilt-work` 는 `HiltWorkerFactory` 배선 때문에,
  셸 둘은 Activity owner ViewModel 받기와 상태 수집 때문에 여기 있다.
- `:app` 의 계측은 여섯이다 — `src/androidTest/…/insight/InsightEvalTest`(통계 LLM 평가 하네스 · 인자 없으면 건너뜀),
  `…/brand/LauncherMaskTest`(`brand` BR-1 — 런처 세 층을 원 · 둥근 사각 · 기기 마스크로 그려 아치가 안 잘리는지 보고 PNG 를
  앱 외부 캐시 `brand-mask/` 에 남긴다), `SplashWaitTest` · `LiveTasksTest`(`brand` BR-4 — 내려가기 기다림 계산과 같은 태스크 판단). 런처 그림은 `:app` 리소스라 라이브러리 계측에서 안 보여 여기 있다. 리소스 값 대조는
  파이썬(`notes-tests/test_brand_resources.py`)이다.
  `…/shell/BackOrderTest`(FM-10 뒤로가기 7단 · FM-4 창 등급 — 상태 없는 셸에 가짜 상태를 넣어 그린다. 몸통은 하네스 말만 쓰고, 셸이
  바뀌면 하네스만 고친다) · `…/brand/TitleFontScreensTest`(`brand` BR-8 — 가로 800×360 첫 실행 연결 화면 · 통계 요약 카드를 기기 글꼴과
  Gothic A1 로 그려 스크롤 없음 · 두 단 · 줄 수를 보고 PNG 를 시험 APK 외부 캐시 `brand-font/` 에 남긴다. Gothic A1 쪽은 글꼴 제공자 ·
  네트워크가 없으면 건너뛴다)도 여기 있다 — 셸과 여러 기능 화면을 같이 그려서다. 둘 다 Hilt ViewModel 없이 그리지만 앱 계측이라
  `MyNoteApplication`(Hilt · 토큰 동기 · 워커)이 실제로 뜬 채 돈다.
- JVM 시험은 `src/test/…/shell/` — 셸 VM(`ShellViewModelTest`) · 등록 묶음(`ShellDestinationsTest`) · 시작 분기(`ConnectStartTest`) ·
  셸을 지나는 기능 사이 신호(`FeatureSignalsTest` · `NotesViewModelTest` · `HomeBodyTest` 의 셸 쪽 — 같은 클래스 이름이 `:feature:notes`
  시험에도 있다, FM-13). `FeatureFixture` 는 셸 VM 과 기능 VM 셋을 같은 Navigator · 신호 계약 · 홀더로 세우는 `:feature:notes` 시험 도구의 사본이다.
- 배선 검증은 여전히 `./gradlew :app:compileDebugKotlin`(= Hilt 그래프 검증)이다.

## 계측 테스트 — 평가 하네스와 "앱이 지워지지 않게"

- **`gradle.properties` 의 `android.injected.androidTest.leaveApksInstalledAfterRun=true` 를 지우지 말 것.** AGP 의
  `connectedAndroidTest` 는 기본으로 끝난 뒤 **테스트 APK 와 대상 앱을 uninstall** 한다. `:app` 의 대상 앱을 지우면
  `/sdcard/Android/data/com.hancekim.mynote/files/` 의 2.6GB 모델까지 사라진다. 에뮬레이터에서 이 플래그로 돌린 뒤
  `firstInstallTime`·모델 파일이 그대로인 것을 확인했다. 부작용은 `com.hancekim.mynote.test` 가 기기에 남는 것뿐이다.
- 하네스는 `evalRound` 인자가 있을 때만 돈다(없으면 skip — 2.6GB 엔진을 올리는 무거운 테스트라서). 실제 모델·실제
  `InsightGenerator` 로 합성 digest 10종 + 오답 묶음 6종을 돌려 `files/eval/eval-round<N>.json` 에 쓴다:

  ```bash
  ANDROID_SERIAL=<기기> ./gradlew :app:connectedDebugAndroidTest \
      -Pandroid.testInstrumentationRunnerArguments.evalRound=3
  adb -s <기기> pull /sdcard/Android/data/com.hancekim.mynote/files/eval/eval-round3.json .
  ```

  `calibrate=true` 를 더하면 24tok 예산으로 잘림 위치를 재는 보정 3건을 더한다. 기준(요약 ≥ 9/10, 패턴 ≥ 5/6, 호출 p95
  ≤ 20초)을 못 넘으면 테스트가 실패한다(JSON 은 그 전에 쓴다). 앱 프로세스 안에서 돌므로 시작할 때 `study-insights` 를
  취소한다 — 같은 프로세스에서 워커가 두 번째 엔진을 올리는 것을 막으려는 것이고, 앱을 다시 켜면 `InsightScheduleSync` 가 되살린다.
- application 컨벤션이 `testInstrumentationRunner = AndroidJUnitRunner` 를 넣는다(라이브러리 컨벤션과 같다).
