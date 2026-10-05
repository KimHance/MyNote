# AGENTS.md — :app

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈의 역할

화면도 상태도 없다. 루트에 `MyNoteApplication`·`MainActivity`·`TokenSync`, `di/` 에 모듈 넷,
`download/` 에 모델 내려받기 배선 셋이 있고 하는 일은 넷이다 — **Hilt 그래프 완성**,
**`BuildConfig`(debug 시드)/토큰 배선**, **Activity 껍데기**(edge-to-edge + `NotesRoute` 호출),
**시스템과 닿는 지점**(알림 액션 리시버·딥링크·프로세스 라이프사이클). 기능 코드는 여기에 넣지 않는다.
여기 파일이 늘어나는 건 "앱 전체에 하나뿐인 것" 이거나 "`PendingIntent`/매니페스트 컴포넌트가 필요한 것"이
생겼을 때뿐이다.

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
를 선언해 두고, 그걸 채우는 `@Binds` 가 `:app` 에 있다. `PendingIntent` 는 매니페스트에 등록된 컴포넌트를
가리켜야 하고 그 컴포넌트는 `:app` 에만 있으니 구현이 여기로 온다. **바인딩을 지우면 `Optional` 이 비어
알림의 "다시 시도" 액션만 조용히 사라진다** — 컴파일도 되고 내려받기도 된다. `TokenSync` 와 같은 종류의
조용한 실패다.

어느 모듈도 제공하지 않아 **`:app` 이 직접 `@Provides` 하는 셋**:

- **`CoroutineScope`** (`AppModule`) = `SupervisorJob() + Dispatchers.Default`, **한정자 없는 싱글턴**.
  `CoroutineScope` 를 주입받는 모든 곳이 이걸 받는다(현재 `TokenSync`·`Navigator`). 한정자가 없으니
  성격이 다른 두 번째 스코프가 필요해지는 순간 `@Qualifier` 를 붙여야 한다.
- **`Navigator`** (`NavigationModule`) — 시작 키(`NotesNavKey.Home`)와 스코프를 고르는 자리.
  `:core:navigation` 은 Hilt 를 모르는 leaf 라 조립이 여기로 온다. `@Singleton` + 앱 스코프라
  백스택이 Activity/ViewModel 보다 오래 산다(회전에도 유지). 대신 프로세스가 죽으면 사라진다 —
  저장/복원은 없다.
- **`ConnectionSeed?`** (`RemoteConfigModule`) — `BuildConfig` 의 시드 필드를 읽어 넘긴다(비면 `null`).
  `:core:network`·`:core:data` 가 `BuildConfig` 를 모르게 하는 것이 이 모듈의 존재 이유다.

`:core:design-foundation`/`:core:design-system` 의존은 `:app` 코드가 참조하지 않고 `@Module` 도 없다 —
`:feature:notes`(feature convention) → design-system → `api(design-foundation)` 로 이미 들어온다.

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
한 줄이 유일한 참조이고, 지우면 Wi-Fi 대기 알림만 조용히 사라진다.

`StudyLogMaintenance` 도 같다 — `@Inject lateinit var studyLogMaintenance` + `onCreate` 의 `start()` 가 앱 시작마다
365일 지난 학습 기록과 26주 지난 요약·패턴을 지우는 유일한 자리다. 지우면 컴파일·실행은 되고 기록만 무한히 쌓인다.

`InsightScheduleSync` 도 같다 — `@Inject lateinit var insightScheduleSync` + `onCreate` 의 `start()` 가 "모델 Ready &&
학습 기록 on" 을 관찰해 통계 워커(`study-insights`)를 예약/취소하는 유일한 자리다. 지우면 컴파일·실행은 되고 **주간 요약이
영원히 생기지 않는다**(통계 카드가 자리째 없는 것과 구별이 안 된다). 앱 시작마다 `schedule()` 을 부르지만 같은 사양(태그
`study-insights-spec-N`)의 작업이 이미 살아 있으면 아무것도 하지 않는다(`:core:ai` `StudyInsightWork`).

**`TokenSync` 에 폴백이 없다 — 저장된 토큰만 holder 로 올린다**(결정 23). 끊기·토큰 지우기 뒤에 빌드 내장 토큰이 되살아나지
않게 하려는 것이다. debug 내장 토큰은 `ConnectionSeed` 로 시드 연결을 할 때만 쓰이고(`ConsumeSeedUseCase`), 그때 저장 칸에
쓴 뒤 적용되고 나서 첫 동기화가 돈다. 이미 저장된 토큰이 있으면 덮어쓰지 않는다(결정 14 · RC-22).

### BuildConfig 필드 — debug 첫 실행 시드뿐이다

저장소 연결은 앱 화면(`:feature:notes` 연결 화면)에서 한다. 빌드가 넣는 값은 **debug 첫 실행 시드** 하나뿐이고
`RemoteConfigModule` 이 이를 `ConnectionSeed?` 로 만든다(`GITHUB_OWNER`·`GITHUB_REPO` 둘 다 채워졌을 때만 시드).
필드는 넷: `GITHUB_OWNER`·`GITHUB_REPO`·`GITHUB_BRANCH`·`GITHUB_TOKEN`.

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
한 번 부르고, 설정 딥링크 인텐트를 한 번 소비한 뒤 `setContent { NotesRoute() }` 만 한다.
`MyNoteTheme` 은 화면에서 시작한다 — Activity 는 테마를 모른다.

- **시스템바 아이콘 명암을 여기서 고정하는 이유:** 앱은 다크 단일 테마인데 XML `Theme.MyNote` 의 부모는
  `android:Theme.Material.Light.NoActionBar` 다(윈도우 배경은 플랫폼 라이트 값, 다크 팔레트는 Compose
  안에서만 시작한다). 한 번에 고정해 두기 때문에 "화면에서 `statusBarsPadding` 을 수동으로 더하지
  않는다"는 규칙이 성립한다.
- `android:enableOnBackInvokedCallback="true"` — 예측 뒤로가기용(커밋 `f97d89d`).
- `android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboard|keyboardHidden"`
  — 회전·분할에서 Activity 가 재생성되지 않는다. 그래서 씬 슬롯의 낡은 캡처가 실제 버그가 되고,
  `:feature:notes` 가 `remember` + `rememberUpdatedState` 로 막고 있다. 이 속성을 지우면 그쪽 전제가 바뀐다.
- **`<uses-native-library>` 두 줄(`libvndksupport.so`·`libOpenCL.so`, `required="false"`)은 LiteRT-LM 의
  GPU 백엔드용이다.** 없으면 예외도 로그도 없이 CPU 로 떨어지고, 판정은 TTFT 로만 가능하다(커밋 `c522d30`).
  매니페스트 정리하다 지우기 쉬운 자리다.
- **백업 규칙(`res/xml/data_extraction_rules.xml`)은 토큰 DataStore(`datastore/github_token.preferences_pb`)와 모델
  (`external` 전체 + 외부 저장소가 없을 때 `filesDir` 에 생기는 `gemma-4-E2B-it.litertlm`·`.part`·`.plan`)을 클라우드 백업과
  기기 이전 **둘 다**에서 뺀다.** `minSdk` 32 라 이 파일만 읽히고 `backup_rules.xml` 은 같은 규칙의 사본이다.
  `<device-transfer>` 구역이 없으면 그 모드는 전부 옮긴다. 모델이 한 번이라도 클라우드 백업 대상에 들어가면 앱별 한도
  25MB 를 넘어 **그 앱의 백업 전체가 조용히 멈춘다** — 북마크·학습 기록도 같이 안 올라간다(기기 이전 한도는 2GB 라
  모델이 넘는다). `include` 를 하나라도 쓰면 나머지가 전부 빠지므로 `exclude` 만 쓴다. DataStore 이름이나 모델 파일
  이름을 바꾸면 이 파일도 같이 고친다.
- `<receiver android:name=".download.ModelRetryReceiver" android:exported="false" />` — 인텐트 필터가 없다.
  `PendingIntent` 가 컴포넌트를 명시하는 explicit intent 라 필터가 필요 없고, 필터를 붙이면 그 순간부터
  `exported` 판정이 달라진다.

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

## `download/` — 시스템이 앱을 깨우는 세 자리

`:core:ai` 는 내려받기를 전부 알지만 `PendingIntent`·매니페스트 컴포넌트·프로세스 라이프사이클을 모른다.
그 셋이 여기 있다.

- **`ModelRetryReceiver`** — 실패 알림의 "다시 시도" 액션이 오는 곳. `ModelDownloads.start()` 만 부른다.
  `@AndroidEntryPoint` 를 **쓰지 않고** `EntryPointAccessors.fromApplication` 을 쓴다. Hilt 의
  `BroadcastReceiver` 주입은 `super.onReceive()` 호출을 요구하는데, 부모 클래스를 바꾸는 건 컴파일 뒤의
  바이트코드 변환이라 Kotlin 컴파일러에는 `onReceive` 가 아직 abstract 로 보인다 —
  `Abstract member cannot be accessed directly` 로 터진다. `@AndroidEntryPoint` 로 되돌리려다 여기서 막힌다.
  `ModelDownloadWorker.enqueue` 가 `ExistingWorkPolicy.KEEP` 라서 끝난 작업에는 새 요청이 들어가고
  돌고 있는 작업은 건드리지 않는다 — `start()` 하나가 "시작" 과 "다시 시도" 를 겸하는 근거다.
- **`AppModelDownloadIntents`** — 위 리시버로 가는 `PendingIntent` 를 만든다. `FLAG_IMMUTABLE` 필수 —
  API 31+ 는 `FLAG_IMMUTABLE`/`FLAG_MUTABLE` 중 하나가 없으면 `IllegalArgumentException` 이고 이 앱의
  `minSdk` 는 그보다 위다. request code 는 `:core:ai` 의 `0` 과 겹치지 않게 `4211`/`4212`.
- **`ModelWaitNotice`** — `ProcessLifecycleOwner` 의 `ON_STOP` 에서 `notifyWaitingForWifi()` 를 부른다.
  **Wi-Fi 대기(`ENQUEUED`)에는 워커가 돌지 않아 포그라운드 알림이 없다** — 앱을 나가면 아무 표시도 없이
  기다리는 상태가 된다. 그 구멍만 메우는 코드이고, 대기 상태가 아니면 `notifyWaitingForWifi()` 가 스스로
  아무것도 하지 않는다. `TokenSync` 처럼 `MyNoteApplication.onCreate` 의 `start()` 한 줄이 유일한 참조다.

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
  `work-runtime`·`hilt-work` 뿐이다. `lifecycle-process` 는 `ProcessLifecycleOwner` 때문에,
  `work-runtime`/`hilt-work` 는 `HiltWorkerFactory` 배선 때문에 여기 있다.
- `:app` 의 테스트는 계측 하나뿐이다 — `src/androidTest/…/insight/InsightEvalTest`(통계 LLM 평가 하네스). 배선 검증은
  여전히 `./gradlew :app:compileDebugKotlin`(= Hilt 그래프 검증)이다.

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
