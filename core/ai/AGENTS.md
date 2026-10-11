# AGENTS.md — :core:ai

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

온디바이스 LLM(LiteRT-LM)으로 학습 노트에서 4지선다 퀴즈를 만들고, 충전 중 백그라운드에서 한 주를
정리하는 주간 요약·틀린 문제 패턴(통계 2·3단계, PRD 4.9.4)을 만든다. `:core:domain` 의 `QuizGenerator`·
`InsightScheduler` 를 구현하는 **바깥 레이어**다(인터페이스는 domain 소유 — DIP).

---

## 경계

| 항목 | 값 |
|---|---|
| 구현하는 계약 | `QuizGenerator` (`isAvailable`, `generate`) · `QuizModelSource`(`ModelDownloads`) · `InsightScheduler`(`insight/StudyInsightWork`) |
| 바인딩 | `AiModule` 의 `@Binds @Singleton`. 그래프에 넣는 건 `:app` (`implementation(projects.core.ai)`) |
| 의존 | `:core:common`(`runCatchingCancellable`) · `:core:domain` · litertlm-android **0.17.0** · coroutines · kotlinx-serialization · work · lifecycle-process(워커의 포그라운드 판정) |
| 없는 것 | Compose·Android UI·`:core:data`/`:core:network` |

역할은 셋으로 갈린다 — **`ModelEngine`**(엔진 수명·동시성), **`LiteRtLmQuizGenerator`**(프롬프트·생성
설정), **`QuizJsonParser`**(모델 출력 복구). 셋 다 서로의 이유가 다르니 섞지 말 것. 통계 쪽은 `insight/` 에
같은 결로 갈린다 — `InsightPrompts`(지시문·예시) · `InsightJsonParser`(JSON·길이) · `InsightGuards`(숫자·근거·베끼기)
· `InsightGenerator`(호출 한 번 + 재시도) · `StudyInsightWorker`/`StudyInsightWork`(워커·예약). 아래 "통계 워커" 절.

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
- **퀴즈(`use`)는 엔진을 닫지 않고, 워커(`tryUse`)가 새로 올린 엔진만 닫는다.** `use` 로 올린 엔진은
  프로세스 종료(또는 모델 삭제의 `release()`)가 유일한 해제다 — 다음 퀴즈에 다시 3–12초를 쓰지 않으려는 것.
  `tryUse(shouldRelease, block)` 은 `tryLock` 실패면 즉시 `EngineUse.Busy`(퀴즈가 쓰는 중이면 워커가 진다),
  엔진이 없었으면 만들어 쓰고 **끝나는 시점에** `shouldRelease()`(워커는 "앱이 백그라운드인가")가 참이면 같은
  락 안에서 닫는다(PRD D55 · 9.2 #41). 실행 중 앱이 앞으로 왔으면 남긴다(로그 `엔진 유지 · 앱이 포그라운드`).
  이미 올라 있던 엔진(퀴즈가 올림)은 재사용만 하고 절대 닫지 않는다(`Done.initMillis == null`).
  락·생성·닫기 규칙은 `EngineSlot`(제네릭, 가짜 엔진으로 `EngineSlotTest` 5건)에 있고 `ModelEngine` 은 위임만 한다.
- **엔진 생성은 `NonCancellable` 안에서 끝까지 받아 필드에 넣는다.** `create()` 는 네이티브 블로킹이라 취소로 멈추지
  않는데, 예전처럼 `withContext { create() }.also { engine = it }` 이면 취소된 호출자에게 결과가 버려져 2.6GB 엔진이
  필드에도 없고 닫히지도 않았다(검수 S3). 지금은 받은 뒤 `tryUse` 가 "새로 만들었으면 닫기" `finally` 안에서
  `ensureActive()` 를 부른다 — 퀴즈 init 중 그만두기는 엔진을 슬롯에 남겨 다음 퀴즈가 재사용한다. 이 줄을 되돌리면
  `EngineSlotTest` 의 init 취소 2건이 빨간불이다.
- **`ExperimentalFlags.enableBenchmark` 는 debug(`FLAG_DEBUGGABLE`) 빌드에서만 켠다**(`create()` 첫머리, 엔진 레벨이라
  생성 전에만 먹는다). 측정·로그 전용이다 — 생성 로그의 `출력 Ntok · ttft · backend=GPU|CPU` 와 평가 하네스의 토큰 수가
  여기서 나오고, release 로그는 `벤치마크 꺼짐`. 켜도 출력은 같다(S25: 같은 노트 퀴즈 914자로 켜기 전과 동일).
  **판정은 이 값에 기대지 않는다** — 예전엔 잘림 재시도가 `lastDecodeTokenCount >= 예산` 을 봐서 끄면 릴리즈에서
  잘림이 조용히 `Parse` 로 바뀌었다(검수 S4). 지금 잘림은 `InsightJsonParser.truncated`(첫 `{` 뒤 문자열 밖 괄호 짝이
  닫히지 않음)로 판정한다.
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

## 내려받기 알림 — `download/work/`

떠 있는 내려받기 알림은 상태로 정해지고 한 번에 하나다(기능 문서 `docs/spec/model-download.md` MD-9 ~ MD-11,
글 · 막대 · 아이콘은 `docs/design/model-download.md` `## 알림`). 판단은 전부 `DownloadNotices.kt` 의 순수 함수이고
`DownloadNotificationsTest`·`DownloadNoticesTest` 가 JVM 에서 본다(이 모듈에 Robolectric 이 없다).

| 함수 | 무엇 |
|---|---|
| `noticeOf(ModelStatus)` | 알림 표 한 행 통째 — 자리 · 제목/본문 키 + 인자 · 막대(없음 · 움직임 · 값) · 액션 · 작은 아이콘 · ongoing · 펼침 |
| `NoticeBoard.on(NoticeEvent)` | 사건 → 알림판. 워커(`Launched` · `Reported` · `Stopped`), `ModelDownloads`(`Started` · `Cancelled` · `Deleted` · `Backgrounded`), 앱 조정자(`Observed`)가 같은 함수를 부른다 |
| `NoticeBoard.post` | 하나를 올리면 다른 자리를 지운다 — "한 번에 하나" 가 여기 있다 |
| `noticeAfterStop(reason, …)` | `getStopReason()` 이 `STOP_REASON_CANCELLED_BY_APP` 일 때만 사용자 취소(알림 없음). 그 밖(Wi-Fi 끊김 · 시스템)은 재시도 알림 |
| `emptySlotsOf(status)` | 앱이 상태만 볼 때 지울 자리 — 없음 · 멈춤만 전부. 작업이 있는 상태는 비운다(관찰이 늦게 와 막 올린 결과 · 재시도를 지우지 않게) |

- **자리 셋이 따로다** — 진행 4201(포그라운드 · Wi-Fi 대기도 여기) · 결과 4202 · 재시도 4203. 작업이 끝나면 WorkManager 가
  포그라운드 알림(4201)을 지운다(`SystemForegroundDispatcher.onExecuted`). 결과 · 재시도를 4201 에 올리면 같이 사라진다.
  앱이 4201 을 `cancel` 해도 포그라운드 알림이면 시스템이 무시한다 — 그래서 `post` 가 다른 자리를 지울 때 무해하다.
- `DownloadNotifications` 는 `Notice` 를 `Notification` 으로 옮기기만 한다. 판단을 여기에 넣으면 JVM 이 못 본다.
- 재시도 막대는 `noticeOf(Retrying(received, total))` 하나로 그린다 — 워커는 `files.partBytes()`, 앱(뒤로 갈 때)은
  `statusOf` 가 준 값이라 같은 비율에서 멈춘다. `InstallResult.Retry` 에 바이트 칸을 다시 넣지 않는다.
- 워커의 `catch (CancellationException)` 에서 부르는 알림판 호출은 suspend 가 아니다 — 취소된 코루틴에서 suspend 를
  부르면 바로 던진다.
- **`setForeground` 는 감싼다.** 앱이 뒤에 있을 때 다시 시작된 실행(Wi-Fi 복귀 · 재시도 백오프 · dataSync 시간 소진)에서는
  포그라운드 서비스를 못 띄워 `IllegalStateException` 이 난다. 잡아서 일반 작업으로 이어 받고, 끊기면 다음 실행이 받은
  자리부터 잇는다. `CancellationException` 도 `IllegalStateException` 이라 그것만은 다시 던진다.
- **해시 검증은 IO 에서 덩어리마다 취소를 확인한다**(`ModelInstaller.verified`). 확인 중에 취소해도 검증이 끝까지 돌아 파일을
  자리에 넣고 `받았습니다` 를 올리면 MD-10 ② 와 어긋난다(`MD-10 파일 확인 중 취소하면 자리에 넣지 않고 조각을 남긴다`).
- 재시도 작은 아이콘 `res/drawable/ai_stat_download_retrying.xml` 은 **움직이지 않는** 그림이다(기능 문서 결정 12 — 멈춘 막대와 같은
  말). 받는 중 · 대기의 플랫폼 `stat_sys_download` 는 움직인다. 출처: Android Studio Vector Asset 의 Material Icons
  `downloading`(Filled, 24 뷰박스) — Apache License 2.0. `tint` 를 지우고 `fillColor` 를 흰색으로 박았다. Material Symbols
  SVG(뷰박스 `0 -960 960 960`)로 바꿀 때는 경로를 그대로 붙이면 그림이 뷰포트 밖으로 나가 빈 아이콘이 된다.

## 취소·타임아웃의 실제 동작 (함정)

`Conversation.sendMessage(...)` 는 suspend 가 아니라 **블로킹 JNI 호출**이다(0.17.0 시그니처 확인).
그래서:

- 호출자(`NotesViewModel`)가 60초 `withTimeoutOrNull` 로 감싸고 `quizJob.cancel()` 도 하지만,
  그건 **코루틴만 푼다.** 네이티브 디코딩은 끝까지 돌고, 그동안 `ModelEngine` 의 `Mutex` 를
  계속 붙잡고 있다 → 취소 직후 들어온 다음 요청은 앞 생성이 끝날 때까지 **대기**한다.
  "취소했는데 여전히 느리다"의 정체가 이것이다.
- **그래서 모든 생성은 `ConversationExt.kt` 의 `sendCancellable` 로 보낸다**(퀴즈·워커 공유). 블로킹
  `sendMessage` 를 `async` 로 돌리고, 기다리던 코루틴이 취소되면 `finally` 에서 `cancelProcess()` 를 불러 네이티브
  디코딩을 실제로 멈춘다(로그 `cancelProcess · 생성 중 취소`). 이게 없으면 위 증상대로 `Mutex` 를 계속 쥔다.
- **`cancelProcess()` 뒤 네이티브 호출은 `CancellationException` 이 아니라 JNI 예외로 끝난다.** 그대로 두면
  취소가 "엔진 실패" 로 기록된다(S25 에서 실제로 그렇게 찍혔다). `cancellable()` 이 실패를 잡으면 먼저
  `ensureActive()` 로 취소를 다시 던진다 — `CancellableTest` 가 지키고, 그 줄을 지우면 3건 중 2건이 빨간불이다.
- 무거운 호출은 전부 `Dispatchers.Default` 로 보낸다. 메인에서 부르면 ANR 이다.
- `runCatching` 이 아니라 `runCatchingCancellable` 을 쓰는 이유: `CancellationException` 을
  `Result.failure` 로 삼키면 취소가 실패 UI 로 보인다.

---

## 아직 안 잰 것 · 안 쓰는 것

- **퀴즈 토큰 수 — 한 번 쟀다(S25, 2026-10-02).** 노트 `set` 3문제: 입력 787tok, 출력 430tok/914자
  (≈ 2.1자/tok), 예산 1,020tok. 요약·패턴은 아래 "통계 워커" 절.
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

- 계측은 `Log.i("MyNoteAi", …)` — 엔진 init ms, 생성마다 `ms · 글자 · 출력/입력 tok · ttft · prefill/decode tok/s ·
  backend`, 워커의 판정·`skipped: <이유>`·대상별 ms·`release`/`엔진 유지`. **실제 GPU 여부**는 API 로 알 수 없다 —
  `backend=` 는 TTFT(3초 미만 GPU)로 추정한 값이고, 확증은 같은 프로세스의 네이티브 로그
  `tflite : Replacing 2068 out of 2068 node(s) with delegate (LITERT_CL)` 다(백그라운드 워커에서도 찍힌다).
- 액티비티 재실행 전 `adb shell am force-stop com.hancekim.mynote`.
  살아 있으면 `am start` 가 `onCreate` 를 안 부른다.
- `adb logcat` 을 붙여 뒀으면 반드시 회수한다(스트림이 남으면 기기가 잠들지 못한다).

---

## 통계 워커 (2·3단계) — `insight/`

`StudyInsightWorker`(`@HiltWorker`, 1일 주기, 충전·배터리 부족 아님) 가 `InsightPlan`(domain) 판정대로 지난주 확정 →
이번 주 요약 → 패턴을 최대 3번 부른다. 산술·판정은 전부 `:core:domain`(`WeeklyDigest`·`InsightPlan`)이고 이 모듈은
엔진·프롬프트·파서·저장 호출만 한다. 예약/취소는 `:app` 의 `InsightScheduleSync` 가 "모델 Ready && 기록 on" 을 보고 한다.

**2B(Gemma E2B) 약점을 코드로 막는 자리** — 모델이 지시를 어기면 화면에 가기 전에 버리거나 한 번 다시 시킨다:

| 약점 | 막는 곳 |
|---|---|
| 숫자를 엉뚱한 대상에 붙임 | **요약 문장에는 숫자를 쓰지 않는다**(2라운드, 검수 B1). 규칙 `Do not write any digits…`, 예시도 숫자 없는 문장, 방향은 digest `trend`(more/less/same)로 말하게 한다. 가드는 headline·detail 에서 노트 제목·카테고리 id 를 지운 뒤 숫자가 남으면 `HasDigits` → "no digits" 한 줄을 붙여 **한 번** 재시도. 숫자는 카드 아래 숫자 칸이 보여 준다. 1라운드의 "숫자 ⊂ digest" 가드는 날짜 조각까지 허용 집합에 들어가 오결합(`82분을 android 노트에` — 82 는 합계)을 못 막았다. 패턴 문장은 여전히 "오답 원문에 있는 숫자만"(`O(1)` 같은 표기) |
| 일반론·주제가 다른 태그 | 근거 말뭉치 = 카테고리 + **노트 제목**(`스택 (Stack)` 같은 이중 표기가 한–영 대응을 준다) + 문항 + 고른 보기 + 정답. 라틴 태그는 3자 이상 + 단어 경계 일치(`in` 처럼 짧은 것은 근거로 치지 않는다), 한 음절 한글 태그(`큐`)는 원문·문장 둘 다에 있을 때만. **태그는 근거가 있고 문장에도 나온 것만 남기고**, 하나도 없으면 문장 핵심어(근거 있는 것)로 바꾼다 — 다른 퀴즈의 태그만 남아 문장과 어긋나던 일(U3 `set` + 스택/큐 문장)을 막는다. 문장 핵심어가 원문과 하나도 안 겹치면 버린다. 프롬프트: `Copy each tag exactly as written in WRONG… Do not translate.`, `Use your tags in the sentence.`, 예시 태그에 한국어(`히스토리`) |
| 예시 베끼기 | one-shot 예시(swift/linux, git rebase/merge — 앱에 없는 값)와 **글자 그대로 같거나, 예시에만 있는 낱말이 입력에 없는데 나오면** 버린다. 틀을 빌리는 것(`cs 노트에 시간을 가장 많이 썼습니다.`)은 정상이다 — 예전 bigram 유사도 0.7 판정은 짧은 카테고리 이름의 정상 문장을 버렸다(2라운드 r2-1 에서 2건) |
| 키 이름 누출 | `least`·`top`·`stalled`·`reread`·`daysLeft`·`prev`·`change`·`trend`·`minutes`·`quizzes`·`accuracy` 가 (제목·id 를 지운) 문장에 나오면 버린다 |
| 영어로 답함 | 한글이 한 글자도 없으면 버린다 |
| 길이 초과·잘림 | 파서 상한(headline 48 · detail 100 · sentence 120 · tag 12). 초과 또는 **괄호가 닫히지 않은 잘린 JSON** 은 `TooLong` → "더 짧게" 한 줄로 **한 번만** 재시도(`HasDigits`·`MissingField` 와 같은 재시도 한 번을 나눠 쓴다 — 필드가 빠진 답은 "missed a field" 한 줄). 12자 넘는 태그 하나는 빼고 나머지를 살린다 |

- **`focus` 는 모델이 고르지만 `least`(가장 적게 읽은 카테고리, domain 계산)를 고르라고 시킨다.** 1회차에서 모델이
  가장 많이 읽은 카테고리를 초점으로 고르는 일이 잦아 digest 에 `top`/`least` 를 넣었다(2회차부터 10/10 이 `least`).
- **출력 토큰 예산 = 실질 길이 상한** — `InsightGenerator.SUMMARY_TOKENS = 69`, `PATTERN_TOKENS = 50`(S25 출력 p95 46·33tok
  의 1.5배, ≈ 2.9자/tok). 파서 상한(≈ 66–76tok 어치)보다 작으므로 길이를 실제로 자르는 건 예산이고 파서 상한은 안전망이다.
  예산을 바꾸면 평가 하네스를 다시 돌린다(긴 제목 두 개를 옮기는 `long-stalled-titles` 추가 케이스가 잘림을 본다).
- **결과 형식 버전 `InsightWork.SCHEMA_VERSION = 2`**(2라운드). 숫자가 든 1라운드 요약은 관찰에서 걸러져 화면에 안 나오고,
  지난 요약의 그 주는 `요약 없음` 이 된다. 형식·프롬프트 계약을 바꾸면 올린다.
- **예약은 사양 태그가 같으면 다시 등록하지 않는다**(`StudyInsightWork.schedule`, 검수 N2). 앱 시작마다 `UPDATE` 하면
  generation 이 오르고 JobScheduler job 이 취소·재등록돼, 워커가 띄운 프로세스의 `onCreate` 가 막 시작한 job 을 끊을 수
  있었다. 요청 사양을 바꾸면 `InsightWork.SPEC_TAG` 를 올린다(그때 한 번만 `UPDATE`).
- **3분 타임아웃도 이미 만든 결과를 지킨다**(검수 N3) — `Outcome` 이 `withTimeout` 바깥에 있어 저장분은 success,
  실패 시각·종류는 돌던 대상 기준(패턴에서 시간이 다 되면 요약 카드에 실패 줄을 안 띄운다).
- **모델 삭제는 워커를 먼저 취소한다**(`ModelDownloads.deleteModel`, 검수 N7) — 그래야 `cancelProcess` 로 락이 1초 안에
  풀리고 `release()` 가 최대 3분 기다리지 않는다.
- 평가 하네스: `app/src/androidTest/…/insight/InsightEvalTest`(합성 digest 10 + 오답 6 + 추가 3, 실제 모델) — `:app`
  AGENTS.md 의 "계측 테스트" 절. 요약 문장 속 숫자(제목 밖) 0, 태그가 문장에 없는 패턴 0 을 단언한다.
  2라운드 r2-2: 요약 10/10 · 패턴 5/6(`mixed-eight` 은 문장을 빼먹음, backlog 9.2 #48) · 호출 p95 1.68초 · 추가 3/3.
- 워커 실행 확인: 주기 작업은 한 번 돌면 다음 주기 전 `cmd jobscheduler run -f` 가 WorkManager 에서
  "executed before schedule" 로 미뤄진다. 새로 돌리려면 설정 `학습 기록 남기기` 를 껐다 켜서(취소 + 새 enqueue) 첫 실행을
  만든 뒤 `run -f -n androidx.work.systemjobscheduler com.hancekim.mynote <jobId>`. S25 는 `dumpsys battery set ac 1`
  로 JobScheduler 의 CHARGING 을 만족시키지 못했다(USB 저전류라 `Considered charging: false`) — 그래서 `-f`.
