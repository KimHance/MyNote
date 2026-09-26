# AGENTS.md — :app

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈의 역할

화면도 상태도 없다. Kotlin 파일은 네 개(`MyNoteApplication`·`MainActivity`·`TokenSync`·`di/` 셋)뿐이고
하는 일은 셋이다 — **Hilt 그래프 완성**, **`BuildConfig`/토큰 배선**, **Activity 껍데기**(edge-to-edge +
`NotesRoute` 호출). 기능 코드는 여기에 넣지 않는다. 여기 파일이 늘어나는 건 "앱 전체에 하나뿐인 것"이
생겼을 때뿐이다.

## Hilt — 그래프가 완성되는 유일한 자리

`@Module` 을 가진 모듈은 넷이다: `:core:data`·`:core:network`·`:core:database`·`:core:ai`.
그 넷을 동시에 아는 모듈은 `:app` 뿐이고, 의존 하나만 빠지면 Dagger 컴포넌트를 만드는
**`:app` 컴파일에서** 터진다(런타임이 아니다). `:core:domain`·`:core:navigation` 은 Hilt 플러그인조차 없다.

| 계약 (소유) | 구현 | `@Module` 위치 |
|---|---|---|
| `NotesRepository`·`TokenRepository` (domain) | `NotesRepositoryImpl`·`TokenRepositoryImpl` | `:core:data` |
| `RemoteNotesDataSource` (data) | `RemoteNotesDataSourceImpl` | `:core:network` |
| `GithubTokenProvider`(network) + `AppliedTokenSource`(domain) | `GithubTokenHolder` — **한 인스턴스가 두 계약** | `:core:network` |
| `LocalNotesDataSource`·`TokenStore` (data) | `LocalNotesDataSourceImpl`·`DataStoreTokenStore` | `:core:database` |
| `QuizGenerator` (domain) | `LiteRtLmQuizGenerator` | `:core:ai` |

어느 모듈도 제공하지 않아 **`:app` 이 직접 `@Provides` 하는 셋**:

- **`CoroutineScope`** (`AppModule`) = `SupervisorJob() + Dispatchers.Default`, **한정자 없는 싱글턴**.
  `CoroutineScope` 를 주입받는 모든 곳이 이걸 받는다(현재 `TokenSync`·`Navigator`). 한정자가 없으니
  성격이 다른 두 번째 스코프가 필요해지는 순간 `@Qualifier` 를 붙여야 한다.
- **`Navigator`** (`NavigationModule`) — 시작 키(`NotesNavKey.Home`)와 스코프를 고르는 자리.
  `:core:navigation` 은 Hilt 를 모르는 leaf 라 조립이 여기로 온다. `@Singleton` + 앱 스코프라
  백스택이 Activity/ViewModel 보다 오래 산다(회전에도 유지). 대신 프로세스가 죽으면 사라진다 —
  저장/복원은 없다.
- **`GithubConfig`** (`RemoteConfigModule`) — owner/repo/branch 를 `BuildConfig` 에서 읽어 넘긴다.
  `:core:network` 가 `BuildConfig` 를 모르게 하는 것이 이 모듈의 존재 이유다.

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

폴백 순서는 `stored ?: BuildConfig.GITHUB_TOKEN.ifBlank { null }` — **저장된 토큰이 있으면 그게 이긴다.**

### BuildConfig 필드

`defaultConfig` 에 넷: `GITHUB_OWNER`·`GITHUB_REPO`·`GITHUB_BRANCH`·`GITHUB_TOKEN`.

- 좌표 셋은 `local.properties` 의 `github.owner/repo/branch` 가 있으면 그 값, 없으면
  `KimHance`/`MyNote`/`main`. (현재 `local.properties` 는 이 세 키가 주석 처리돼 기본값이 쓰인다.)
- **`GITHUB_TOKEN` 은 `defaultConfig` 에서 빈 문자열이고 `debug` 블록에서만 덮어쓴다** —
  release APK 에는 토큰이 들어가지 않는다(런타임 입력 전용, 입력 다이얼로그는 `:feature:notes`).
  그래서 release 로 처음 켜면 노트가 안 보이는 게 정상 동작이다.
- `buildFeatures { buildConfig = true }` 가 필요하다(AGP 9 기본 off). 이 한 줄이 빠지면 `BuildConfig`
  자체가 사라져 `RemoteConfigModule`·`TokenSync` 가 함께 깨진다.
- `local.properties` 를 configuration 단계에 읽지만 Gradle 이 설정 입력으로 추적한다 — 내용을 바꾸면
  configuration cache 가 무효화되고 `BuildConfig` 가 다시 생성된다(확인함). 토큰을 바꿨는데 안 먹는
  현상을 캐시 탓으로 보지 말 것.

## Activity / Manifest

`MainActivity` 는 `super.onCreate` 앞에서 `enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT), …)` 를
한 번 부르고 `setContent { NotesRoute() }` 만 한다. `MyNoteTheme` 은 화면에서 시작한다 — Activity 는
테마를 모른다.

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
  `dependencies` 블록은 모듈 목록 + `core-ktx`·`lifecycle-runtime` 뿐이다.
- `:app` 에는 테스트 소스가 없다(`src/test`·`src/androidTest` 둘 다 없음). 여기 코드는 배선뿐이라
  검증은 `./gradlew :app:compileDebugKotlin`(= Hilt 그래프 검증)으로 한다.
