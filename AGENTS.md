# AGENTS.md — MyNote

이 파일은 이 레포에서 작업하는 에이전트를 위한 안내다.

GitHub 의 JSON 노트를 받아 읽는 **읽기 전용** 안드로이드 학습 노트 앱. 앱에서 쓰기는 없다.
온디바이스 LLM(LiteRT-LM)으로 노트에서 4지선다 퀴즈를 만드는 기능이 붙어 있다.
퀴즈 모델(2.6GB)은 APK 에 없고 **앱에서 런타임에 내려받는다**(WorkManager + 포그라운드 알림).

**모듈마다 `AGENTS.md` 가 따로 있다. 어떤 모듈을 건드리든 그 파일을 먼저 읽어라.**
여기에는 모듈 하나만 봐서는 알 수 없는 것만 적는다.

> `README.md` 는 사람용 소개이고 지금 낡았다 — AGP·Kotlin 버전이 옛 값이고 모듈 트리에
> `core/ai`·`core/navigation`·`core/common` 이 빠져 있다. 사실 확인은 `gradle/libs.versions.toml`
> 과 `settings.gradle.kts` 로 해라.

---

## 명령

```bash
./gradlew ktlintCheck                  # 포맷·컴포즈 룰
./gradlew ktlintFormat --continue      # 자동 수정
./gradlew :app:assembleDebug           # 디버그 APK
./gradlew test                         # 단위 테스트 — testDebugUnitTest 가 아니다(아래 참조)
./gradlew lintDebug                    # Android Lint
./gradlew :app:installDebug            # 기기 설치
```

한 번에 검증하려면:

```bash
./gradlew ktlintCheck :app:assembleDebug test lintDebug
```

**단일 테스트:**

```bash
./gradlew :core:ai:testDebugUnitTest --tests "*QuizJsonParserTest.똑같은*"
```

### `testDebugUnitTest` 가 아니라 `test` 인 이유

`:core:domain` 은 안드로이드 라이브러리가 아니라 **순수 JVM 모듈**이라 `testDebugUnitTest`
태스크가 없다. 루트에서 `./gradlew testDebugUnitTest` 를 돌리면 그 모듈을 **조용히 건너뛴다.**
`test` 는 두 종류를 다 잡는다.

---

## 모듈 지도와 의존 방향

```
app  ─────────────────────────────  composition root. 구현체를 전부 알고 Hilt 로 묶는다
 │
 ├─ feature/notes  ───────────────  화면 · ViewModel · 퀴즈
 │    └→ core/{common, design-system, domain, navigation}   (feature convention 이 자동 주입)
 │
 ├─ core/ai        ──→ core/domain  LiteRT-LM 으로 QuizGenerator 구현
 ├─ core/network   ──→ core/data    GitHub Contents API
 ├─ core/database  ──→ core/data    Room3 캐시 + DataStore
 ├─ core/data      ──→ core/domain  DataSource 인터페이스 소유 + reconcile
 ├─ core/domain    ─────────────────  순수 JVM. 인터페이스를 소유한다(DIP)
 ├─ core/navigation ─→ core/common   nav3 계약. 테마·도메인을 모른다
 ├─ core/design-system ─→ design-foundation, core/common
 ├─ core/design-foundation ────────  토큰(색·타이포·간격·AdaptiveTokens). leaf
 └─ core/common    ─────────────────  MVI base · Throttler. leaf
```

**화살표는 항상 바깥→안쪽이다.** 인터페이스는 안쪽이 소유하고 바깥이 구현한다.

지켜야 할 것:
- **feature 는 `:core:data`/`:core:network`/`:core:database` 를 직접 의존하지 않는다.** `:core:domain`
  의 UseCase 만 경유한다.
- Room `@Entity`·Retrofit·DataStore 타입은 각 모듈 밖으로 새지 않는다.
- `:core:design-foundation` 은 `:core:design-system` 을 의존하지 않는다. feature ↔ feature 직접 의존 금지.
- **버전·좌표는 `gradle/libs.versions.toml` 에만.** 모듈 `build.gradle.kts` 에 하드코딩 금지
  (현재 위반 0건).

### 안쪽 모듈의 순수성은 빌드가 강제한다

`:core:domain` 은 `hancenote.jvm.library` 라서 `import android.*` 가 **컴파일 에러**다
(`Unresolved reference 'android'`). "순수 Kotlin" 이 약속이 아니라 빌드 규칙이다.
그래서 이 모듈에는 `R.string`·`@Serializable`·`@Immutable` 이 classpath 에 아예 없다.

---

## 모듈을 가로지르는 함정

한 모듈만 봐서는 안 보이는 것들이다. 각각 해당 모듈 `AGENTS.md` 에 자세히 있다.

**모르는 블록 타입 하나가 동기화 전체를 죽인다.**
본문 블록은 `@SerialName` 으로 고정된 닫힌 집합이고, `sync()` 는 전부-아니면-전무다.
원격 `notes/` 에 앱이 모르는 `type` 이 올라가면 그 노트 하나가 아니라 **모든 기기의 동기화가
실패한다.** → **앱 업데이트가 콘텐츠보다 먼저 나가야 한다.** (`core/data/AGENTS.md`)

**`MyNoteApplication` 의 `@Inject lateinit var tokenSync` 를 지우면 조용히 인증이 없어진다.**
그 필드 한 줄이 `TokenSync` 를 살아 있게 하는 유일한 참조이고, `GithubTokenHolder` 를 채우는
유일한 호출자다. 지워도 컴파일·실행은 되고 private 레포 요청만 무인증으로 나간다. (`app/AGENTS.md`)

**60초 타임아웃이 퀴즈 엔진을 못 푼다.**
`Conversation.sendMessage()` 는 블로킹이라(suspend 아님) 코루틴을 취소해도 네이티브 디코딩이
계속 돌고 `Mutex` 를 계속 쥔다. 라이브러리에 `cancelProcess()` 가 있는데 아직 안 쓴다.
(`core/ai/AGENTS.md`)

**퀴즈 모델 상태 타입은 `:core:domain` 이 소유한다.**
받는 구현은 `:core:ai` 지만 feature 는 `:core:ai` 를 의존하지 않는다. `ModelStatus`·`ModelFailure`·
`QuizModelSource` 를 domain 에 두고 `ModelDownloads` 가 구현한다. `:core:ai` 에 public 을 만들어도
feature 에서는 안 보인다. (`feature/notes/AGENTS.md`)

**뒤로가기 우선순위는 컴포즈 순서다.**
`feature/notes/shell/` 의 파일을 옮기거나 쪼개면 조용히 깨진다. (`feature/notes/AGENTS.md`)

**`layerBackdrop` Box 안에는 `NavDisplay` 만.** 오버레이가 들어가면 유리가 자기를 샘플링한다.
(`core/design-system/AGENTS.md`)

---

## 전 모듈 공통 규약

**언어** — 대화 응답·코드 주석·문서 전부 한국어.

**화면 문구는 `strings.xml`.** 코드에 한글 리터럴을 두지 않는다(프리뷰 더미와 개발자용 `error()`·예외
메시지는 예외). 모듈별 `resourcePrefix` 가 걸려 있다 — design-system `ds_`, feature:notes `notes_`.
공통 문구(확인·취소·저장·닫기·되돌리기)는 design-system 의 `ds_*` 를 쓴다.
말투는 **합니다체** — 서술 `~합니다`/`~습니다`, 권유 `~하세요`.

**MVI** — 모든 ViewModel 이 `:core:common` 의 `MviViewModel<S, E>` 를 상속하고 `build()` 하나에서만
상태를 만든다. 쓰기 전에 `core/common/AGENTS.md` 를 읽어라.

**Compose** — 애니메이션 값은 `graphicsLayer { }` 람다 안에서 읽는다. `Modifier.alpha(v)` 처럼
인자로 받으면 매 프레임 리컴포지션이 돈다. 컬렉션 파라미터는 `ImmutableList`.

**컨벤션 플러그인** — 모듈은 `hancenote.*` 플러그인만 적용하고 수동 설정을 하지 않는다.
ktlint·Compose 의존·Hilt 는 컨벤션이 자동으로 넣는다. (`build-logic/AGENTS.md`)

---

## 노트 콘텐츠 (`notes/`)

앱이 GitHub 에서 읽어가는 **서버드리븐 콘텐츠**다. 코드가 아니다.

- 노트 추가·수정·삭제는 `write-note` 스킬로 한다. 손으로 파일을 만들지 않는다.
- **`notes/index.json`·해시·`gen_index.py` 를 직접 건드리지 않는다.** pre-commit 훅이 커밋 순간
  `gen_index.py` 로 재생성해 같은 커밋에 넣는다(작문은 LLM, 결정적 변환은 훅).
- 블록은 `notes/SCHEMA.md` 의 닫힌 집합만. 새 타입을 넣으려면 위의 "동기화 전체가 죽는다" 를 먼저 읽어라.
- **`git push` 까지 해야 반영된다.** 앱은 원격을 읽지 로컬 클론을 읽지 않는다.

---

## git

- 훅 설치(머신당 1회): `git config core.hooksPath .githooks`
  pre-commit 이 **스테이징된 Kotlin 파일만** ktlint 검사하고, 노트를 건드렸으면 `index.json` 을 재생성한다.
- **`git commit --no-verify` 금지** — 훅을 건너뛰면 `index.json` 이 안 만들어진다.
- 커밋은 사용자가 요청할 때만. 사용자 신원으로, AI 공동저자 푸터 없이.
- 커밋 메시지에 **"무엇" 보다 "왜"** 를 적는다. 이 레포는 그렇게 쓰여 있고, 에이전트가 근거를 찾을 때
  실제로 `git log` 를 읽는다.

---

## 모델 파일

퀴즈용 LiteRT-LM 모델(2.59GB)은 **APK 에 들어 있지 않다.** 기기에 따로 넣는다.

```
PC   <PC 의 모델 폴더>/gemma-4-E2B-it.litertlm
기기  /sdcard/Android/data/com.hancekim.mynote/files/gemma-4-E2B-it.litertlm
```

`adb uninstall` 은 기기 파일을 지운다. `installDebug` 재설치로는 안 지워진다.
없으면 퀴즈 기능이 조용히 비활성화되고(크래시 아님) 안내 문구가 뜬다.
