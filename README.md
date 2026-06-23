# MyNote

> GitHub raw JSON 을 읽어 "블로그 글처럼" 보는 **읽기 전용** 안드로이드 학습 노트 앱.

다크 단일 테마 · 멀티 폼팩터(반응형) · 앱 내 쓰기 없음. 현재는 **PoC** 단계로, 모든 콘텐츠는 원격 JSON 대신 로컬 더미 데이터(`core/data/src/main/assets/notes.json`)로 제공된다.

---

## 무엇을 하는 앱인가

- 학습 노트 콘텐츠를 **서버드리븐**으로 받아 카테고리/목록/본문으로 읽는 리더 앱.
- 콘텐츠는 `Note`/`Category`/`NoteBlock` 직렬화 모델로 표현되며, 본문은 블록 단위로 렌더한다.
- 폼팩터에 따라 목록-상세를 2-pane(태블릿/확장) 또는 모달 드로어(컴팩트)로 적응 표시한다.

### 화면 (`feature:notes`)

| 화면 | 역할 |
|---|---|
| **HomeScreen** | 카테고리 · 최근 노트 진입점 |
| **ListPanelScreen** | 노트 목록 (목록-상세의 list pane) |
| **NoteReaderScreen** | 노트 본문 렌더 (서버드리븐 블록) |

---

## 기술 스택

| 영역 | 사용 기술 |
|---|---|
| 언어 / UI | Kotlin · Jetpack Compose · Material3 (XML 미사용) |
| 아키텍처 | **MVI** — `UiState` + `Intent` + ViewModel `onIntent()` |
| 내비게이션 | **Navigation 3** (`NavDisplay`, 앱이 백스택 소유) + **Material3 adaptive** `ListDetailSceneStrategy` (2-pane) |
| DI | **Hilt** (`@HiltAndroidApp` · `@HiltViewModel` · `@Binds` repository) |
| 직렬화 | kotlinx.serialization |
| 비동기 | Coroutines / Flow |
| 컬렉션 | kotlinx-collections-immutable |

### 빌드 정보

- AGP **9.2.1** · Kotlin **2.3.21**
- compileSdk **37** · targetSdk **36** · minSdk **32**
- convention plugins (`:build-logic` 의 `hance.android.*`) 로 모듈 빌드 통일
- 버전은 `gradle/libs.versions.toml` 한곳에서 관리

---

## 모듈 구조

```
MyNote/
├─ app/                    Application(Hilt), MainActivity, NotesRoute 호스트
├─ build-logic/convention/ hance.android.{application,library,feature,hilt,room…} 플러그인
├─ core/
│  ├─ design-foundation/   디자인 토큰(색·타이포·간격), AdaptiveTokens(반응형)
│  ├─ design-system/       MyNoteTheme, 공용 컴포넌트, 본문 블록 렌더
│  ├─ data/                Note·NoteBlock 모델, NotesRepository(+더미), Hilt 모듈, notes.json
│  └─ domain/              GetCategoriesUseCase 등 UseCase
└─ feature/notes/          NotesViewModel/UiState/Intent, NotesRoute, 화면 3개, Nav3 그래프
```

### 의존 그래프 (→ = 의존)

```
        ┌─────────┐
        │   app   │
        └────┬────┘
   ┌─────────┼───────────────┬───────────────┐
   ▼         ▼               ▼               ▼
design-   design-       ┌─ domain ─┐    feature:notes
foundation system       │          │     │   │
             │          ▼          │     │   │
             │   api(core:data)    │     │   │
             │                     │     │   │
   feature:notes ─────────────────┘     │   │
        │                                │   │
        ├──────────────► core:domain ◄───┘   │
        └──────────────► core:design-system ◄┘

핵심 규칙
  • feature:notes 는 core:data 를 직접 의존하지 않는다 → core:domain 경유
    (데이터 모델은 domain 의 api(core:data) 로 transitive 전파)
  • core:design-system → api(core:design-foundation)   (역방향 의존 금지)
  • feature ↔ feature 직접 의존 금지
```

---

## 현재 상태 / 다음 단계

**현재 (PoC)**
- 모듈 경계 · 디자인 토큰 · 더미 데이터 · 3개 화면 골격 완성.
- 콘텐츠는 원격이 아닌 로컬 `notes.json` 더미.

**선언만 되어 있고 아직 배선되지 않음**
- `gradle/libs.versions.toml` 에 Retrofit · OkHttp · Room · Coil · Lottie · WorkManager · Timber 등이 정의되어 있으나, 실제 코드에는 연결되지 않았다(Billboard 베이스에서 가져온 잔재). `:app` 은 core-ktx / activity-compose / lifecycle 만 의존한다.

**다음 단계**
- 실제 네트워크 레이어(Retrofit/OkHttp) 배선 — 원격 JSON 로딩.
- 테스트 확충 — 현재는 `core:data` 의 `NoteBlockSerializationTest` 정도이며 ViewModel · 화면 UI 테스트는 미작성.

---

## 빌드 & 실행

```bash
# 컴파일 검증
./gradlew :app:compileDebugKotlin

# 단위 테스트
./gradlew :core:data:testDebugUnitTest

# 디버그 APK
./gradlew :app:assembleDebug
```

---

**Package:** `com.hancekim.mynote`
