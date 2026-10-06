# AGENTS.md — :core:network

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈이 하는 일

`:core:data` 의 **`NoteSourceAdapter` 를 구현**한다 — `GithubManifestAdapter`(`kind = GitHub`, Hilt `@IntoSet`).
(`:core:network → :core:data`). 반환 타입은 전부 data 가 소유한 타입(`SourceEntry`, 원문 `String`)이라
**Retrofit/OkHttp 타입은 이 모듈 밖으로 나가지 않는다.** 리비전 비교·reconcile·**형식 해석**은 전혀 모른다 —
그건 `core/data/AGENTS.md` 에 있다. 여기는 "요청을 어떻게 만들고 응답을 어떻게 넘기는가" 만 본다.

엔드포인트는 두 개뿐이고 둘 다 GET 이다(앱에 write 없음):
`repos/{owner}/{repo}/contents/notes/index.json` 과 `repos/{owner}/{repo}/contents/{path}`, 둘 다 `?ref={branch}`.
`list()` 는 index 를 받아 `SourceEntry`(docId = index `id`, revision = index `hash`, category = 그룹 키)로 평탄화하고,
`fetch()` 는 노트 파일을 **해석하지 않은 원문 문자열**로 돌려준다(`ResponseBody.string()`).

## 건드리면 깨지는 것

- **`@Headers("Accept: application/vnd.github.raw+json")` 이 두 메서드의 반환을 성립시킨다.**
  이 헤더가 없으면 GitHub Contents API 는 파일 내용이 아니라 `{ name, sha, content: "<base64>", … }`
  메타데이터 envelope 을 준다. 그러면 index 직렬화가 실패하고 노트 원문 자리에 envelope 이 들어간다.
  헤더를 지우려면 base64 디코딩 계층을 새로 만들어야 한다는 뜻이다.
- **`getNote` 는 `ResponseBody` 를 돌려준다. `NoteDto` 같은 DTO 로 되돌리지 말 것.** 본문을 Retrofit 컨버터가 디코드하면
  깨진 노트 하나가 컨버터 예외 = 전송 실패가 되어 그 소스의 동기화 전체가 멈춘다(M0 이전의 F1–F3). 형식 해석은 `:core:data` 가
  `:core:format` 으로 노트 단위로 가둔다. 회귀 장치: `GithubManifestAdapterTest` 의 `본문은 해석하지 않고 원문 문자열로 돌려준다`.
  `@Streaming` 이 없어 Retrofit 이 본문을 메모리에 다 받은 뒤 돌려주므로 `string()` 은 네트워크를 다시 타지 않는다.
- **`@Path(value = "path", encoded = true)`.** 노트 경로는 `notes/<그룹>/<id>.md` 처럼 `/` 를 포함한다.
  `encoded = true` 를 빼면 `/` 가 `%2F` 로 인코딩돼 URL 이 깨진다. `path` 는 index 가 준 값을 그대로 쓴다.
- **index 평탄화는 이 모듈의 책임이다.** `index.json` 은 `{ "<group>": [ {id,title,date,path,hash}, … ] }`
  형태의 **그룹 키 오브젝트**고, 그룹 이름은 값이 아니라 **키**에 있다. `GithubManifestAdapter.list()` 가
  `flatMap` 으로 키를 `SourceEntry.category` 에 접어 넣는다. 파일 모양 DTO `IndexNoteDto` 와 경계 타입 `SourceEntry` 둘 다 `:core:data` 소유.
  index 는 지금처럼 Retrofit 컨버터가 디코드하고, 깨지면 그 소스의 동기화 실패(무반영)다 — 항목 단위로 관대하게 바꾸면
  빠진 항목의 노트가 **지워지므로** 하지 않는다.
- **`Json` 을 여기서 만들지 않는다.** `:core:data` 의 `DataModule` 이 제공하는 인스턴스를 주입받아
  Retrofit 컨버터에 꽂는다. 또 `@Provides Json` 하면 Hilt 중복 바인딩이다.
- **에러를 삼키지 않는다.** 어댑터는 실패를 `SyncFailure` 6종(`failure/GithubFailures.kt`: 401 · 404/403 · 한도 · 5xx/시간 초과 · 형식 · 오프라인)으로
  **바꿔 던진다** — 원인은 `cause` 에 남는다. 한도 = 429, 403+`x-ratelimit-remaining: 0`, 403+`retry-after`. `Result` 로 감싸는 것도,
  사용자 노출도 `:core:data`/`:feature:notes` 몫이다. 여기서 폴백을 넣으면 "전송 실패는 소스 단위로 무반영" 계약이 무성화된다.
- **연결 확인(`GithubConnectProbe`)은 `AuthInterceptor` 가 없는 별도 클라이언트(`@ProbeClient`)로 간다.** 토큰은 요청 인자 하나(`Authorization`)로만
  싣고 holder 를 의존하지 않는다 — 저장된 토큰이 무인증 확인에 실리면 공개 저장소 판정이 틀어진다(`ConnectProbeTest` `RC-4 …`).
  `ConnectProbeApi` 는 `Response<T>` 를 돌려줘 상태 코드와 헤더로 분류한다. 후보는 순서대로 시도하고 404 에서만 다음으로 넘어간다.
- **저장소 좌표는 이 모듈이 갖지 않는다.** 어댑터는 요청마다 `SourceRecord.configJson` 을 `:core:data` 의
  `ConnectionConfigCodec` 으로 읽어(저장소 · 브랜치 · 목록 위치) 요청을 만든다. 연결 정보가 없는 소스는
  `requireNotNull` 로 실패한다(동기화가 그런 소스를 아예 안 부른다). 브랜치가 빈 문자열이면 `ref` 를 보내지 않는다(저장소 기본 브랜치).
  목록 파일 경로도 `{path}`(인코딩 안 함)이고, 항목 `path` 는 저장소 루트 기준 그대로다.
- OkHttp 타임아웃은 connect 10s / read 15s / write 10s 로 고정돼 있다. **`okhttp-logging` 이 의존성에는
  있지만 인터셉터로 등록돼 있지 않다** — 네트워크 로그가 안 보이는 건 정상이고, 보려면 직접 추가해야 한다.

## 토큰이 흐르는 길 (이 모듈이 종착지)

토큰은 런타임에 바뀌는 값이다(release 빌드는 아무 토큰도 내장하지 않고 사용자가 입력한다).
그래서 **OkHttp 를 만들 때 헤더에 박을 수 없다.**

```
[입력] SaveTokenUseCase → TokenRepository(domain) → TokenStore(data) → DataStoreTokenStore(database, 영속)
[반영] :app TokenSync(@Singleton, ObserveTokenUseCase 구독, distinctUntilChanged)
         → GithubTokenHolder.update(저장값)   ← 폴백 없음. debug 내장 토큰은 시드 연결 때 저장 칸에만 쓴다(ConsumeSeedUseCase)
[사용] AuthInterceptor: 매 요청마다 holder.currentToken() → "Authorization: Bearer …"
[되돌림] GithubTokenHolder.applied() → AppliedTokenSource(domain) → NotesViewModel 이 재sync 트리거
```

- `GithubTokenHolder` 는 `@Singleton` 인메모리 `MutableStateFlow` 이고 **두 인터페이스를 동시에 구현**한다:
  `GithubTokenProvider`(이 모듈, 인터셉터가 읽는 쪽)와 **`AppliedTokenSource`(`:core:domain` 소유)**.
  그래서 `:core:network` 는 `:core:data` 뿐 아니라 `:core:domain` 도 의존한다.
- 두 인터페이스로 쪼갠 이유: UI 가 보는 "토큰 있음"은 *저장됐다* 가 아니라 **HTTP 클라이언트에 실제로
  적용됐다** 여야 한다. `NotesViewModel` 은 `appliedToken` 을 `combine` 해 두고 그 값이 바뀔 때 sync 를
  다시 돌린다 — 토큰 저장 직후 자동 재시도가 여기서 나온다. `applied()` 를 `observeToken()`(DataStore)으로
  바꿔 끼우면 아직 인터셉터에 안 붙은 토큰으로 요청을 보내게 된다.
- `update()` 는 `token?.ifBlank { null }` 로 정규화한다. **빈 문자열 = 토큰 없음**이 이 파이프라인 전체의
  불변식이고, 토큰이 없으면 `AuthInterceptor` 는 헤더를 아예 붙이지 않는다(private 레포라 401/404 가 난다).
- `TokenSync` 는 `:app` 의 `MyNoteApplication` 이 `@Inject lateinit var` 로 **잡아 두기만 해서** 살아 있다.
  그 필드를 지우면 아무도 holder 를 채우지 않아 **모든 요청이 조용히 인증 없이 나간다.**

## 테스트

- `GithubManifestAdapterTest`(MockWebServer, `okhttp-mockwebserver`) — index 평탄화, 본문을 원문 문자열로(깨진 본문에도 던지지 않음),
  요청 경로·`ref`·`Accept` 헤더와 경로의 `/` 가 인코딩되지 않음.
- `ConnectProbeTest`(확인 요청 · 응답 분류 · 후보) · `SyncFailureMappingTest`(SY-5) · `NetworkStatusResolverTest`.
- reconcile 검증은 `:core:data` 의 `NotesReconcileTest` 가 fake `NoteSourceAdapter` 로 한다.
