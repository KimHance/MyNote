# AGENTS.md — :core:ai

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

온디바이스 LLM(LiteRT-LM)으로 학습 노트에서 4지선다 퀴즈를 만든다. `:core:domain` 의
`QuizGenerator` 를 구현하는 **바깥 레이어**다(인터페이스는 domain 소유 — DIP).

---

## 경계

| 항목 | 값 |
|---|---|
| 구현하는 계약 | `core.domain.repository.QuizGenerator` (`isAvailable`, `generate`) |
| 바인딩 | `AiModule` 의 `@Binds @Singleton`. 그래프에 넣는 건 `:app` (`implementation(projects.core.ai)`) |
| 의존 | `:core:common`(`runCatchingCancellable`) · `:core:domain` · litertlm-android **0.17.0** · coroutines · kotlinx-serialization |
| 없는 것 | Compose·Android UI·`:core:data`/`:core:network` |

역할은 셋으로 갈린다 — **`ModelEngine`**(엔진 수명·동시성), **`LiteRtLmQuizGenerator`**(프롬프트·생성
설정), **`QuizJsonParser`**(모델 출력 복구). 셋 다 서로의 이유가 다르니 섞지 말 것.

**입력을 자르는 책임은 이 모듈에 없다.** `GenerateQuizUseCase`(`:core:domain`)가 노트를 평문으로
펴서 **2,000자**로 자른 뒤 넘긴다. 프롬프트 길이가 곧 TTFT 라 그 상한이 곧 성능 상한이다 —
이 모듈에서 느리다고 느껴지면 먼저 그 쪽을 본다.

---

## `ModelEngine` — 모르고 건드리면 깨지는 곳

- **엔진은 앱 전체에 하나. 단 앱 시작 때 만들지 않는다.** init 이 초 단위다(코드 주석 8~10초,
  최초 구현 커밋 실측 3.5~12초 — 파일 캐시 상태에 따라). 요청마다 만들면 GPU 이점이 사라지고,
  앱 시작에 만들면 퀴즈를 한 번도 안 누르는 사용자가 2.5GB 를 메모리에 이고 있게 된다.
  그래서 첫 요청에서 lazy 생성 + 이후 재사용.
- **엔진을 쓰는 통로는 `use { }` 하나뿐이고 `Mutex` 로 직렬화한다.** 이유가 둘이다 —
  init 이 두 번 겹치지 않게, 그리고 **생성이 동시에 돌지 않게.**
  뒤쪽이 중요하다: 0.17.0 AAR 바이트코드에서 `Conversation` 은 `monitorenter` 가 **0개**다
  (`Engine` 4개, `Capabilities` 7개와 대조). 라이브러리가 대화 객체에 락을 안 건다 —
  **직렬화는 우리 책임**이고, `use` 를 우회해 `Engine` 을 직접 만지면 그 보장이 사라진다.
- **`ExperimentalFlags.enableSpeculativeDecoding = true` 는 `Engine(...)` 생성 *전에* 세팅해야 먹는다.**
  전역 object 를 엔진이 생성 시점에 읽는다. `create()` 첫 줄이 그 자리다 — 순서를 바꾸면
  에러 없이 조용히 무시된다.
- **`Engine` 은 `AutoCloseable` 인데 우리는 닫지 않는다.** 의도다. 프로세스 종료가 유일한 해제.
  닫는 코드를 넣으려면 "다음 요청에 다시 10초"를 받아들일지부터 정해야 한다.
- `cacheDir = context.cacheDir.path` 는 컴파일된 셰이더 등의 캐시다. 지우면 2회차 init 이 다시 느려진다.
- `EngineConfig.maxNumTokens`(컨텍스트 창)는 **엔진 레벨**이라 늘리려면 재init 이다. 현재 지정하지
  않고 라이브러리 기본값을 쓴다.

## GPU 는 예외도 로그도 없이 CPU 로 떨어진다

`Backend.GPU()` 라고 써 있어도 GPU 로 돌고 있다는 보장은 이 모듈 안에 없다.
**`app/src/main/AndroidManifest.xml` 의 `<application>` 안에 있는 두 줄**에 달려 있다:

```xml
<uses-native-library android:name="libvndksupport.so" android:required="false" />
<uses-native-library android:name="libOpenCL.so"      android:required="false" />
```

이게 없으면 **예외도 로그도 없이** CPU 로 폴백한다. 매니페스트는 `:app` 소유이므로
이 모듈만 읽어서는 보이지 않는다 — 이 모듈에서 가장 먼 결합이다. 매니페스트를 정리하는
작업이 이 두 줄을 "안 쓰는 선언"으로 보고 지우면 성능만 10배 나빠지고 아무도 모른다.

**판정은 시간으로만 가능하다.** 예전 실측(Galaxy S25 / SM8750 / API 36) 기준선:

| 구간 | 값 (예전 실측) |
|---|---|
| TTFT | 0.6~0.9초 = GPU · **6~7초대 = CPU 폴백** |
| 퀴즈 3문제 | 4.9초 / 972자 |
| thinking 켠 경우 | +50초 |
| MTP(speculative decoding) 끈 경우 | +27초 |

NPU 는 예전 실측에서 `Backend.NPU` 가 예외 없이 CPU 로 폴백했다(TTFT ~7초). 현재 코드에 NPU 분기는 없다.

---

## 생성 설정이 왜 지금 값인가

- **thinking off** — `ThinkingConfig(enableThinking = false)`. 켜면 위 표대로 수십 초가 붙는다.
  참고로 이 모델의 메타데이터(`Capabilities`)는 thinking 미지원이라고 답하는데 **실제로는 동작한다**
  (예전 실측). 그래서 명시적으로 끄는 게 의미가 있다.
- **greedy** — `SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0)`. 구조화(JSON) 출력이라 온도 0.
  **부수효과가 UX 전제다:** 같은 노트·같은 문항 수로 재시도하면 사실상 같은 출력이 나온다.
  그래서 실패 화면의 실질적 탈출구는 "같은 조건 재시도"가 아니라 **문항 수를 줄인 재시도**
  (`NotesIntent.RetryQuizWithFewer`)다. 샘플러를 건드리면 그 전제가 바뀐다.
- **`maxOutputToken = 340 * count`** — 문항당 토큰 예산. **220 으로 잡았을 때 해설이 문장 중간에서
  잘렸다**(커밋 `c522d30` 실측). 한국어는 토큰당 글자 수가 적다. 호출자는 3~5문항을 보내므로
  (`QuizUiState.MIN/DEFAULT/MAX_COUNT` = 3/4/5) 실제 예산은 1,020~1,700 토큰이다.
  **예산이 모자라면 JSON 이 끝까지 나오지 않아 파서가 `Parse` 실패로 떨어진다** — 모델이
  이상한 소리를 한 것으로 오해하기 쉬운 자리다. 문항 수를 늘리려면 이 상수부터 본다.
- **프롬프트는 영어, 출력은 한국어.** 지시문 토큰을 줄이려고 지시문만 영어로 썼다(노트 본문은
  한국어 그대로다). 그러면 모델이 영어로 답하므로 `Write question, choices and explanation in
  KOREAN` 한 줄로 되돌린다 — **그 줄을 빼면 답이 영어로 나온다.**
- 응답 텍스트는 `Message.contents.contents` 에서 `Content.Text` 만 골라 이어붙인다.
  `toString()` 에 의존하지 않는다.

---

## 파서: 관대한 게 기능이다

2B 모델은 `"네, 만들어 드릴게요"` + 코드펜스를 붙여 온다. 엄격한 역직렬화면 전부 실패한다.

- 첫 `[` ~ 마지막 `]` 사이만 떼어내 쓰고, `Json { isLenient; ignoreUnknownKeys }` 로 읽는다.
- 한 문항이 망가져도 나머지는 살린다(`mapNotNull`). 전부 죽으면 `QuizFailure.Empty`.
- **`answerIndex` 1-based 보정은 `trimmed.size` 와 정확히 같을 때만 한다.** 그 밖의 범위 이탈은
  문항 폐기 — 정답을 추측하지 않는다.
- **중복 제거 순서가 load-bearing이다.** `answerIndex` 는 *지우기 전* 목록의 번호다. 그래서
  정답을 **텍스트로 먼저 확보** → `distinctBy { dedupeKey() }` → 살아남은 목록에서 번호를 다시 찾는다.
  순서를 바꾸면 정답이 엉뚱한 선택지를 가리킨다(화면에는 멀쩡해 보이므로 조용히 틀린다).
  `dedupeKey()` = lowercase + 연속 공백 1칸. 4개 중 하나가 겹치면 **3지선다로 살리고**, 하나만
  남으면 문항을 버린다.
- **4개로 자를 때 정답이 잘리는 자리에 있으면 마지막 칸을 정답에 내준다.** 정답을 0번으로
  몰아넣지 않는 이유는 위치가 고정되면 읽지 않고도 맞힐 수 있기 때문이다.
- 파서가 **못 잡는 것**: 글자가 달라서 의미만 겹치는 선택지(`"실제 데이터 값"` vs `"실제 값"`).
  이건 프롬프트 규칙으로만 막는다(커밋 `8c69cb9`) — 파서를 아무리 조여도 안 걸린다.
- **실패는 반드시 `QuizFailure.Parse`/`Empty`/`Engine` 으로 낸다.** 화면이 이 타입으로 실패 문구와
  재시도 버튼을 고른다(`QuizUiState.Failed.Kind`). 일반 예외로 바꾸면 전부 `Engine` 으로 뭉개진다.
- `QuizJsonParserTest` 16개가 위 규칙을 전부 고정한다. **파서를 고치기 전에 그 테스트를 읽어라.**
  기기 없이 돌 수 있는 유일한 검증이다(`./gradlew :core:ai:testDebugUnitTest`).
  커밋 `b74f697` 에서 `distinctBy` 를 빼는 변이로 빨간불을 확인해 뒀다.

---

## 모델 파일은 APK 에 없다

- `gemma-4-E2B-it.litertlm`(2.59GB)을 `getExternalFilesDir(null)` 에서 찾는다 →
  `/sdcard/Android/data/com.hancekim.mynote/files/`. `.gitignore` 에 `*.litertlm` 이 등록돼 있다.
- `applicationIdSuffix` 가 없어 debug/release 가 **같은 경로**를 쓴다. `adb push` 한 번으로 둘 다 된다.
- **파일이 없으면 예외가 아니라 조용한 비활성이다.** `isAvailable()` = 파일 존재 여부 →
  `NotesViewModel` 의 `quizAvailable` 이 false → 퀴즈 진입점이 화면에서 사라진다.
  "퀴즈 버튼이 안 보인다"는 대부분 버그가 아니라 파일이 없는 것이다.
- `isAvailable()` 은 **존재만** 본다. 깨진 파일이나 다른 모델이면 첫 `use` 에서 `QuizFailure.Engine` 으로 터진다.

## 취소·타임아웃의 실제 동작 (함정)

`Conversation.sendMessage(...)` 는 suspend 가 아니라 **블로킹 JNI 호출**이다(0.17.0 시그니처 확인).
그래서:

- 호출자(`NotesViewModel`)가 60초 `withTimeoutOrNull` 로 감싸고 `quizJob.cancel()` 도 하지만,
  그건 **코루틴만 푼다.** 네이티브 디코딩은 끝까지 돌고, 그동안 `ModelEngine` 의 `Mutex` 를
  계속 붙잡고 있다 → 취소 직후 들어온 다음 요청은 앞 생성이 끝날 때까지 **대기**한다.
  "취소했는데 여전히 느리다"의 정체가 이것이다.
- 라이브러리에 `Conversation.cancelProcess()` 가 있지만 **아직 쓰지 않는다.** 진짜 취소를
  원하면 그걸 엮어야 한다(미착수).
- 무거운 호출은 전부 `Dispatchers.Default` 로 보낸다. 메인에서 부르면 ANR 이다.
- `runCatching` 이 아니라 `runCatchingCancellable` 을 쓰는 이유: `CancellationException` 을
  `Result.failure` 로 삼키면 취소가 실패 UI 로 보인다.

---

## 아직 안 잰 것 · 안 쓰는 것

- **프롬프트 토큰 수 — 미측정.** 지시문을 영어로 바꿨지만 같은 커밋에서 규칙을 4개→8개로
  늘렸다. 실제로 토큰이 줄었는지 **확인하지 못했다.** 재려면 `Conversation.getBenchmarkInfo()` →
  `lastPrefillTokenCount` / `lastDecodeTokenCount` / `lastPrefill·DecodeTokensPerSecond`
  (0.17.0 API 확인). 코드 어디서도 호출하지 않고, `ExperimentalFlags.enableBenchmark` 도 켜지 않는다.
- **`Capabilities` 는 쓰지 않는다.** 예전 실측에서 이 모델의 플래그가 틀려 있었다
  (`supportsFunctionCalling`/`supportsThinking` 이 `false` 인데 실제로는 동작). **능력 판단은
  플래그가 아니라 실제로 돌려서 한다.**
- **스트리밍 미사용.** `sendMessageAsync(...): Flow<Message>` 가 있지만 블로킹 `sendMessage` 로
  한 번에 받는다. JSON 배열을 통째로 파싱해야 해서 부분 출력이 쓸 데가 없다.
  (진행 표시를 넣고 싶어지면 여기가 후보지만, 파서 계약을 먼저 바꿔야 한다.)

## ADK for Kotlin 은 지금 넣을 수 없다

`google-adk-kotlin-litertlm 1.0.1` 의 바이트코드는
`ConversationConfig.<init>(Contents, List, List, SamplerConfig, boolean, List, Map, LoraConfig, …)`
**8인자** 시그니처를 부른다(0.13.1 로 컴파일됨). 0.17.0 에는 **12인자**만 있다(두 AAR 바이트코드 확인)
→ 런타임 `NoSuchMethodError`. Gradle 은 좌표당 버전을 하나로 수렴시키므로 두 구현이 한 APK 에
공존하지도 못한다. 예전에 있던 `-PaiImpl=adk` / `:core:ai-adk` 분기는 **현재 레포에 없다.**

## 디버깅

- 계측은 `Log.i("MyNoteAi", …)` 두 줄뿐이다 — 엔진 init ms, 생성 ms·응답 글자수.
  어디서 멈췄는지 알아야 하면 **단계별 경과 시간을 더 찍어라.** 없으면 추측밖에 안 된다.
- 액티비티 재실행 전 `adb shell am force-stop com.hancekim.mynote`.
  살아 있으면 `am start` 가 `onCreate` 를 안 부른다.
- `adb logcat` 을 붙여 뒀으면 반드시 회수한다(스트림이 남으면 기기가 잠들지 못한다).
