# :core:ai — Claude Code Instructions

온디바이스 LLM 구현 모듈. `:core:domain` 의 `QuizGenerator` 를 LiteRT-LM 으로 구현한다.
의존: `:core:domain` + `litertlm-android`. **Compose 를 의존하지 않는다.**

## 스택

| 항목 | 값 |
|---|---|
| 런타임 | `com.google.ai.edge.litertlm:litertlm-android` (Google Maven) |
| 모델 | `gemma-4-E2B-it.litertlm` 2.59GB (HF `litert-community/gemma-4-E2B-it-litert-lm`) |
| 백엔드 | GPU |
| 기기 경로 | `/sdcard/Android/data/com.hancekim.mynote/files/` |

모델은 APK 에 안 들어간다. `adb push` 로 기기에 직접 넣는다. `applicationIdSuffix` 가 없어
debug/release 가 같은 경로를 쓴다. `.gitignore` 에 `*.litertlm` 등록됨.

---

## 실측 기준선 (Galaxy S25 / SM8750 / API 36)

| 구간 | 값 |
|---|---|
| `Engine.initialize()` | 3.4 ~ 10.3초 (파일 캐시 상태에 따라) |
| `createConversation()` | ~0.2초 |
| TTFT 첫 턴 | 0.6 ~ 0.9초 |
| TTFT 2번째 턴 | **0.2초** (KV 캐시) |
| decode | ~90자/초 |
| 퀴즈 3문제 생성 | **4.9초 / 972자** |
| thinking + 자동 툴 2회 | **66초** ← 이래서 thinking 을 끈다 |

---

## 하드 룰

- **`Engine` 은 앱 전체에 하나.** init 이 10초라 요청마다 만들면 GPU 이점이 사라진다.
  단 **앱 시작 때 만들지 않는다** — 안 쓰는 사용자가 2.5GB 를 메모리에 이고 있을 이유가 없다.
- **`Conversation` 은 스레드 안전하지 않다.** 바이트코드상 `monitorenter` 0개다
  (`Engine` 4개, `Capabilities` 7개와 대조). 앱이 `Mutex` 로 직렬화해야 한다.
- **GPU 는 매니페스트 선언이 필요하다.** 없으면 **예외도 로그도 없이 CPU 로 떨어진다.**
  ```xml
  <uses-native-library android:name="libvndksupport.so" android:required="false"/>
  <uses-native-library android:name="libOpenCL.so" android:required="false"/>
  ```
- **GPU 여부는 시간으로만 판정한다.** TTFT 0.6초대 = GPU, 6~7초대 = CPU 폴백.
- 무거운 호출(`initialize`, `sendMessage`)은 워커 스레드에서. 메인에서 부르면 ANR.
- 툴 반환값과 프롬프트 입력은 **반드시 자른다.** 프롬프트 길이가 곧 TTFT 다.
  자르는 책임은 `:core:domain` 의 UseCase 에 둔다.

## `Capabilities` 를 믿지 마라

`Capabilities(modelPath)` 는 모델 파일 메타데이터를 JNI 로 읽을 뿐이고, **이 모델은 값이 틀려 있다.**

| 항목 | 플래그 | 실제 |
|---|---|---|
| `supportsFunctionCalling()` | `false` | **동작함** (툴 호출·체이닝 실측) |
| `supportsThinking()` | `false` | **동작함** (thought 2,930자 수신) |
| `hasSpeculativeDecodingSupport()` | `true` | 맞음 |
| `inputModalities()` | text/vision/audio | 맞음 (이미지 인식 실측) |

근거: 모델의 `chat_template.jinja` 에 `<|tool>`·`<|tool_call>`·`<|tool_response>`·`<|think|>` 가
전부 들어 있다. **능력은 있는데 메타데이터가 안 채워진 것**이다.

→ **기능 가능 여부는 플래그가 아니라 실제로 돌려서 판단한다.**

---

## ADK 와 함께 쓸 수 없다 (버전 충돌)

```
litertlm 0.13.1  ConversationConfig(Contents, List, List, SamplerConfig, boolean, List, Map, LoraConfig)
litertlm 0.17.0  ConversationConfig(… 위 8개 …, boolean, Integer, ThinkingConfig, boolean)
```

ADK for Kotlin 1.0.1 은 **0.13.1 로 컴파일**됐고 8개짜리 `$default` 합성 메서드를 호출한다.
0.17.0 에는 그 시그니처가 없어 **런타임 `NoSuchMethodError`** 다.

Gradle 은 좌표당 버전을 하나로 수렴시키므로 두 구현이 한 APK 에 공존할 수 없다.
`:app/build.gradle.kts` 가 Gradle 프로퍼티로 구현을 고른다:

```bash
./gradlew :app:installDebug              # 직접 구현 (0.17.0)
./gradlew :app:installDebug -PaiImpl=adk # ADK 구현 (0.13.1) — :core:ai-adk 필요
```

**ADK 가 툴 능력을 대신 만들어주지 않는다.** `ManualOpenApiTool` 이 LiteRT-LM 의 `OpenApiTool` 을
구현해 `ConversationConfig(tools = …)` 에 그대로 넣는다. ADK 가 맡는 건 실행과 오케스트레이션뿐이다.

## NPU 는 현재 못 쓴다

기기 전용 모델(`gemma-4-E2B-it_qualcomm_sm8750.litertlm`, SM8750 = S25)이 HF 에 있지만
`Backend.NPU(...)` 는 **예외 없이 CPU 로 폴백**한다(실측 TTFT 7,065ms = CPU 수준).

막힌 지점 둘, 둘 다 열린 이슈다:
- `libLiteRtDispatch_Qualcomm.so` 가 AAR 에 없고 공개 배포 경로도 없다 — LiteRT #6889
- 전용 모델을 써도 S25 에서 QNN 버전 불일치 — LiteRT-LM #2226

---

## 설정 3층

| 층 | 클래스 | 바꾸려면 |
|---|---|---|
| 엔진 | `EngineConfig` (modelPath, backend, visionBackend, audioBackend, **maxNumTokens**, maxNumImages, **cacheDir**) | 재init 10초 |
| 대화 | `ConversationConfig` (systemInstruction, initialMessages, tools, samplerConfig, automaticToolCalling, thinkingConfig, maxOutputToken, …) | 새 Conversation |
| 메시지 | `sendMessage(...)` 인자 | 공짜 |

- **`maxNumTokens` 는 Engine 레벨이다.** 컨텍스트 창을 늘리려면 엔진을 다시 만들어야 한다.
- `cacheDir` 을 주면 2회차 init 이 빨라진다.
- MTP(`ExperimentalFlags.enableSpeculativeDecoding`)는 **엔진 생성 전에** 켜야 먹는다.
  GPU 면 켜서 손해 없다(chunk 119→69, 자당 ~9%).
- 툴·구조화 출력에는 `temperature = 0`.

## 스트리밍

`sendMessageAsync(String): Flow<Message>` — **`String` 이 아니라 `Message` 가 흘러온다.**
`toString()` 이 텍스트를 주긴 하지만 명시적으로 꺼내는 게 안전하다:

```kotlin
contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }
```

chunk 는 토큰과 1:1 이 아니다(평균 2~4자). 속도 비교용 근사치로만 쓴다.

---

## 배드 케이스

| 증상 | 원인 |
|---|---|
| 매 요청 10초 지연 | `Engine` 을 재생성 중 |
| TTFT 가 6~7초대 | GPU 미적용. 매니페스트 `<uses-native-library>` 확인 |
| 응답이 수십 초 | thinking 켜짐. 툴 왕복마다 다시 생각한다(3턴이면 3배) |
| 답변의 숫자가 틀림 | 모델에게 계산을 시킴. 2B 산술은 신뢰 불가 |
| JSON 파싱 실패 | 모델이 코드펜스·군말을 붙인다. `QuizJsonParser` 처럼 관대하게 파싱 |
| `CalledFromWrongThreadException` | 워커 스레드에서 View 접근 |
| 대화가 길어지며 느려짐 | 컨텍스트 누적. 요약해 새 `Conversation` 으로 |

## 디버깅

- **단계별 경과 시간을 찍는다.** 없으면 어디서 멈췄는지 모른다
- **툴 호출은 툴 함수 안에서 기록한다.** 답변만 보면 지어낸 것과 구분이 안 된다
- **액티비티 재실행 전 `am force-stop`.** 살아 있으면 `am start` 가 `onCreate` 를 안 부른다
- **`adb logcat` 스트림은 반드시 회수한다.** 이 기기 로그가 532만 줄이라 붙여두면 폰이 잠들지 못한다
- `channels["thought"]` 로 모델 의도를 볼 수 있지만, 이건 모델이 **써낸 글**이지 실제 연산 기록이 아니다
