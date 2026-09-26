# AGENTS.md — :core:domain

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈이 하는 일

**가장 안쪽 레이어.** 모델 + 인터페이스(DIP) + UseCase 뿐이고 프로젝트 모듈은 하나도 의존하지 않는다.
외부 의존도 `kotlinx-coroutines-core` 와 `javax.inject` 둘뿐이다.

## 순수 JVM 이라 규칙이 문서가 아니라 빌드로 강제된다

`hancenote.jvm.library` 를 쓴다 — `com.android.library` 가 아니다. 매니페스트·R 클래스·리소스 머지·
계측 러너가 하나도 붙지 않고, 무엇보다 **`import android.*` 가 `Unresolved reference` 컴파일 에러**다.
"domain 은 순수 Kotlin" 이 지켜지길 바라는 규칙이 아니라 지킬 수밖에 없는 조건이 된다.

여기서 파생되는 것들:

- **코루틴은 `-core`**(`-android` 아님). `Dispatchers.Main` 이 없다. 스레드 선택은 바깥이 한다 —
  `:core:data` 가 `withContext(Dispatchers.Default)`, feature 가 `viewModelScope`.
- **어노테이션을 "안 붙이는" 게 아니라 붙일 수 없다.** `@Serializable`(serialization 플러그인 미적용),
  `@Immutable`/`@Stable`(compose runtime 없음), `R.string`(리소스 없음) 전부 classpath 에 없다.
  직렬화는 `:core:data` 의 DTO, stability 는 UI 상태, 표시 문구는 `strings.xml` 의 책임이라는 배치가
  실수로 무너질 수 없다. 모델은 평범한 `data class` + `List` 를 그대로 쓴다.
- **Hilt 플러그인이 없다.** `javax.inject.Inject` 만 쓰고 UseCase 는 unscoped 생성자 주입이다.
  `@Binds` 는 구현체를 가진 `:core:data`/`:core:network`/`:core:ai` 의 모듈이 한다.
- **Gradle 태스크 이름이 다르다.** `:core:domain:compileKotlin` / `:core:domain:test` 이고
  `compileDebugKotlin`·`testDebugUnitTest`·`assembleDebug` 는 **존재하지 않는다.**
  루트에서 `./gradlew testDebugUnitTest` 를 돌려도 이 모듈은 조용히 건너뛴다(현재 `src/test/` 도 없다).

## 인터페이스는 여기가 소유하고 바깥이 구현한다

| 인터페이스 | 하는 일 | 구현 |
|---|---|---|
| `NotesRepository` | `observeCategories(): Flow<List<Category>>` + `suspend sync(): Result<SyncOutcome>` | `NotesRepositoryImpl` (`:core:data`) |
| `TokenRepository` | 저장된 토큰 read/write/observe | `TokenRepositoryImpl` (`:core:data` → DataStore in `:core:database`) |
| `AppliedTokenSource` | 지금 **실제로 먹은** 토큰 | `GithubTokenHolder` (`:core:network`) |
| `QuizGenerator` | `isAvailable()` + `generate(title, text, count)` | `LiteRtLmQuizGenerator` (`:core:ai`) |

domain 은 넷 다 구현을 모른다. 그래프는 `:app` 이 네 모듈을 전부 포함해 완성한다.

### 토큰 인터페이스가 둘인 이유 (중복이 아니다)

`TokenRepository` = 사용자가 **저장한** 값. `AppliedTokenSource` = 네트워크 클라에 **꽂힌** 값.
`:app` 의 `TokenSync` 가 저장값을 관찰해 holder 에 밀어넣으면서 `BuildConfig.GITHUB_TOKEN` 을 폴백으로 얹는다.
그래서 debug 는 저장값이 없어도 붙는다 — 화면이 "토큰 있음"을 판정할 때 `ObserveTokenUseCase` 를 보면
debug 에서 항상 없다고 나온다. `ObserveAppliedTokenUseCase` 가 그 판정용이다(`NotesViewModel` 이 그걸 쓴다).

## UseCase 가 어디까지 하는가

대부분은 위임 한 줄이다(`GetCategories`·`SyncNotes`·`ObserveToken`·`ObserveAppliedToken`,
`SaveToken` 은 blank → null 정규화까지). 정렬·필터 같은 도메인 규칙이 생기면 여기로 온다.

실제 규칙을 든 건 **`GenerateQuizUseCase`** 하나다. 화면에서 `noteId` 만 받아 네 가지를 한다:

1. **노트를 찾는다** — `observeCategories().first()` 로 **캐시된 트리**에서 찾는다. 네트워크를 타지 않는다
   (SSOT 는 Room). 못 찾으면 `QuizFailure.Empty`. 즉 호출부가 `Note` 를 들고 오지 않아도 된다.
2. **블록을 평문으로 편다** — 8종 블록의 exhaustive `when`. `Divider`/`Spacer` 는 버리고 코드블록 본문은
   **포함한다**(코드에서도 문제를 낼 수 있게). 리스트는 `- ` 를 붙여 한 줄씩.
3. **`take(2_000)` — 본문을 자르는 책임이 여기 있다.** 프롬프트 길이가 곧 prefill 이고 prefill 이 곧 TTFT다
   (온디바이스 2B/GPU 기준 첫 토큰이 본문 길이에 비례해 늘어난다). 컨텍스트 창(`EngineConfig.maxNumTokens`)은
   엔진 레벨이라 늘리려면 재init 10초짜리라서, 실질적으로 조절 가능한 손잡이는 입력 길이뿐이다.
   구현마다 각자 자르면 기준이 흩어지고 어느 값이 먹었는지 추적이 안 된다.
   → **`QuizGenerator` 구현은 받은 텍스트가 이미 잘려 있다고 가정해도 된다**(`:core:ai` 가 그 전제로 쓴다).
4. 평문이 blank 면 `generate` 를 **부르지 않는다**(엔진을 깨우지 않는다).

경계 두 가지:

- **timeout 은 여기 없다.** `NotesViewModel` 의 `withTimeoutOrNull(60초)` 가 감싼다. 그래서 UI 쪽
  `QuizUiState.Failed.Kind` 에는 `Timeout` 이 있는데 `QuizFailure` 에는 없다 — 불일치가 아니라 소유자가 다르다.
- `isAvailable()` 은 UseCase 를 거치지 않는다. feature 가 `QuizGenerator` 를 직접 주입받아 부른다
  (도메인 인터페이스라 경계 위반은 아니다). 모델 파일 존재 여부를 묻는 것뿐이라 감싸도 얻는 게 없다.

## `QuizFailure` 의 메시지는 개발자용이다

`sealed class QuizFailure : Exception` — `Parse`/`Empty`/`Engine`. 메시지는 전부 영어 진단 문구다
(`"parse: no JSON array in output"`, `"engine failed to load or run"`). **화면 문구가 아니다.**

경로: `:core:ai` 가 실패를 만들고 → `NotesViewModel.toFailKind()` 가 `QuizUiState.Failed.Kind` 로 바꾸고
→ `QuizFailedScreen` 이 `Kind` → `R.string.notes_quiz_failed_*` 로 그린다.

여기에 한국어 안내 문장을 쓰면 안 되는 이유 셋:

1. **같은 문장이 두 벌이 된다.** `strings.xml` 을 고쳐도 도메인 쪽은 안 따라오고, 어느 쪽이 화면에
   보이는지는 호출 경로에 달린다.
2. **도메인이 표시 문구를 알게 된다.** 로케일·번역·리소스는 UI 의 관심사다. 도메인이 그걸 들면
   문구를 바꾸려고 가장 안쪽 레이어를 건드리게 된다.
3. **애초에 불가능하다.** 순수 JVM 이라 `R.string` 에 닿을 수 없다 — 남는 건 번역 불가능한
   하드코딩 문자열뿐이다.

그리고 화면 분기는 **타입으로** 한다. 메시지 문자열로 갈랐다면 문구를 다듬는 순간 화면이 조용히 깨진다.
새 실패 종류를 추가할 때도 문자열이 아니라 서브타입을 추가하고, `toFailKind()` + `Kind` + `strings.xml`
세 자리를 같이 고친다(`toFailKind` 의 `else` 가 `Engine` 이라 **컴파일러가 안 잡아 준다**).

## 모델

- `Category`/`Note`/`NoteBlock`/`NoteListItem`/`NoteSpan`/`SpanStyle` + `QuizQuestion` + `SyncOutcome`.
- **`NoteBlock` 은 같은 모양이 세 번 나온다:** `NoteBlockDto`(`:core:data`, `@Serializable`) →
  **`NoteBlock`(여기)** → `NoteContentBlock`(`:core:design-system`, `@Immutable`). 이 중복이 경계의 값이다 —
  DTO 가 UI 까지 새지 않고, UI stability 가 도메인을 오염시키지 않는다.
  블록 타입을 하나 추가하면 exhaustive `when` 이 있는 자리(`NoteMapper`, `NoteBlockMapper`,
  `GenerateQuizUseCase.toPlainText`)는 컴파일 에러로 잡히지만 **`notes/gen_index.py` 의 `BLOCK_TYPES` 와
  `notes/SCHEMA.md` 는 컴파일러가 잡아 주지 않는다.**
- **`SyncOutcome` 은 값이 아니라 판단까지 준다** — `changed`(added+updated)·`hasChanges`. 스낵바를 띄울지
  (`hasChanges`)와 문구(`added`/`updated`/`deleted`)를 같은 자리에서 못 세면 "변경 없음"인데 안내가 뜬다.
  `deleted` 는 개수가 아니라 **id 목록**이다 — 삭제된 노트를 보고 있으면 화면을 옮겨야 해서 호출부가 id 를 쓴다.
