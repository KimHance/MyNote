# :core:network — Claude Code Instructions

원격(GitHub) 노트 소스 모듈. 의존 방향: `:core:network → :core:data`.

## 하드 룰
- `:core:data` 의 `RemoteNotesDataSource` 인터페이스를 **구현**한다(DIP). 반환 타입은 data 가 소유한
  DTO(`IndexEntryDto`/`NoteDto`). Retrofit/OkHttp 타입은 이 모듈 밖으로 내보내지 않는다.
- 책임: `notes/index.json`(카탈로그) GET + 개별 노트 `path` GET 2가지. 앱은 해시를 **비교만** 하고
  생성하지 않는다.
- `Json` 직렬화 설정은 **`:core:data` 가 단일 제공**한다. 네트워크는 그 `Json` 을 주입받아
  Retrofit 컨버터에 쓴다(중복 제공 금지 — Hilt 중복 바인딩).
- 에러는 삼키지 않고 호출자(`:core:data` reconcile)까지 예외로 전파한다. 사용자 노출/폴백은 상위에서.
- private repo + 토큰(Contents API) 분기는 후속. 지금은 `BASE_URL` 설정값만 두고 구조 유지.
