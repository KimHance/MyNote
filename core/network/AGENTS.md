# AGENTS.md — :core:network

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈이 하는 일

`:core:data` 의 **`RemoteNotesDataSource` 를 구현**한다(`:core:network → :core:data`). 반환 타입은 전부
data 가 소유한 DTO 라서 **Retrofit/OkHttp 타입은 이 모듈 밖으로 나가지 않는다.**
해시 비교·reconcile 은 전혀 모른다 — 그건 `core/data/AGENTS.md` 에 있다. 여기는 "요청을 어떻게
만들고 응답을 어떻게 DTO 로 만드는가" 만 본다.

엔드포인트는 두 개뿐이고 둘 다 GET 이다(앱에 write 없음):
`repos/{owner}/{repo}/contents/notes/index.json` 과 `repos/{owner}/{repo}/contents/{path}`, 둘 다 `?ref={branch}`.

## 건드리면 깨지는 것

- **`@Headers("Accept: application/vnd.github.raw+json")` 이 두 메서드의 반환 타입을 성립시킨다.**
  이 헤더가 없으면 GitHub Contents API 는 파일 내용이 아니라 `{ name, sha, content: "<base64>", … }`
  메타데이터 envelope 을 준다. 그러면 `Map<String, List<IndexNoteDto>>` / `NoteDto` 직렬화가 통째로
  실패한다. 헤더를 지우려면 base64 디코딩 계층을 새로 만들어야 한다는 뜻이다.
- **`@Path(value = "path", encoded = true)`.** 노트 경로는 `notes/android/nav3.json` 처럼 `/` 를 포함한다.
  `encoded = true` 를 빼면 `/` 가 `%2F` 로 인코딩돼 URL 이 깨진다. `path` 는 index 가 준 값을 그대로 쓴다.
- **index 평탄화는 이 모듈의 책임이다.** `index.json` 은 `{ "<group>": [ {id,title,date,path,hash}, … ] }`
  형태의 **그룹 키 오브젝트**고, 그룹 이름은 값이 아니라 **키**에 있다. `RemoteNotesDataSourceImpl` 이
  `flatMap` 으로 키를 `IndexEntryDto.group` 에 접어 넣는다. 그래서 DTO 가 두 개다 —
  `IndexNoteDto`(파일 모양, group 없음) / `IndexEntryDto`(경계 타입, group 있음). 둘 다 `:core:data` 소유.
- **`Json` 을 여기서 만들지 않는다.** `:core:data` 의 `DataModule` 이 제공하는 인스턴스를 주입받아
  Retrofit 컨버터에 꽂는다. 또 `@Provides Json` 하면 Hilt 중복 바인딩이다.
- **에러를 삼키지 않는다.** 예외를 그대로 위로 던진다 — `Result` 로 감싸는 것도, 사용자 노출도
  `:core:data`/`:feature:notes` 몫이다. 여기서 폴백을 넣으면 "전부 아니면 전무" 계약이 무성화된다.
- **`GithubConfig`(owner/repo/branch)는 이 모듈이 정의하지만 제공하지 않는다.** `:app` 의
  `RemoteConfigModule` 이 `BuildConfig`(= `local.properties`)에서 읽어 제공한다. 그래서 `:core:network`
  단독으로는 Hilt 그래프가 닫히지 않는다.
- OkHttp 타임아웃은 connect 10s / read 15s / write 10s 로 고정돼 있다. **`okhttp-logging` 이 의존성에는
  있지만 인터셉터로 등록돼 있지 않다** — 네트워크 로그가 안 보이는 건 정상이고, 보려면 직접 추가해야 한다.

## 토큰이 흐르는 길 (이 모듈이 종착지)

토큰은 런타임에 바뀌는 값이다(release 빌드는 아무 토큰도 내장하지 않고 사용자가 입력한다).
그래서 **OkHttp 를 만들 때 헤더에 박을 수 없다.**

```
[입력] SaveTokenUseCase → TokenRepository(domain) → TokenStore(data) → DataStoreTokenStore(database, 영속)
[반영] :app TokenSync(@Singleton, ObserveTokenUseCase 구독, distinctUntilChanged)
         → GithubTokenHolder.update(저장값 ?: BuildConfig.GITHUB_TOKEN)   ← debug 만 값이 있음
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

이 모듈에는 테스트가 없다(컨벤션이 `failOnNoDiscoveredTests(false)` 로 풀어 뒀다).
평탄화·reconcile 검증은 `:core:data` 의 `NotesReconcileTest` 가 fake `RemoteNotesDataSource` 로 대신한다.
