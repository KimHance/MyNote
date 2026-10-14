# AGENTS.md — :feature:editor

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈의 역할

**기기 노트를 쓰고 가져오는 기능**(기능 문서 `local-notes`)이다 — 새 노트 · 고치기 편집기, 붙여넣기 화면, 가져오기 그릇(파일 열기 ·
붙여넣기를 고르는 대화상자), 나가기 확인 · 지우기 확인, 공유 받은 글을 편집기로 잇는 일. 목적지 키는 `NotesNavKey.Editor` ·
`NotesNavKey.Paste`(`:core:navigation`), 등록은 `EditorDestinations.kt`, 셸 위에 띄우는 것은 `EditorHost.kt` 다.
데이터는 `:core:domain` 의 UseCase 만 쓴다(저장 · 고침 · 지움 · 고치기용 원문 · 미리보기 · 가져오기 — 구현은 `:core:data`).

| 파일 | 무엇 |
|---|---|
| `EditorViewModel.kt` | 편집 세션 · 가져오기 그릇 · 붙여넣기 칸 · 공유 받기 · 지우기를 한 ViewModel 이 든다(Activity owner — 결정 7) |
| `EditorUiState.kt` | 상태 · 세션 · 인텐트 |
| `EditorMessages.kt` | 스낵바 · 기본 그룹 문구(FM-8 — ViewModel 은 문구를 직접 읽지 않는다. `:app` `NotesModule` 이 묶는다) |
| `EditorDestinations.kt` | 편집기 · 붙여넣기 등록 |
| `EditorHost.kt` | 파일 고르기 런처 · 가져오기 그릇 · 나가기 확인 · 지우기 확인. 셸이 시작 분기 위에서 늘 컴포즈한다 |
| `screen/` | 편집기 · 붙여넣기 화면 |

## 지킬 것

- **세션 = 스택의 편집기 키.** ViewModel 이 스택을 구독해 편집기 키가 새로 나타나면 세션을 열고, 빠지면 버린다. 편집기는 스택에 하나다
  (`openEditorCommand` 가 갈아 끼운다 — LN-8 ③). 본문은 상태 복원 번들에 넣지 않는다(결정 6) — `rememberSaveable` 로 칸 글을 들지 말 것.
- **미리 채운 글은 키에 싣지 않는다.** 가져오기 결과는 ViewModel 안 맵에 두고 키에는 `draftId` 만 싣는다. 공유 글은 `:app` 의
  `ShareReceiverActivity` 가 `:core:ui` 의 `SharedTextInbox` 에 넣고 이 ViewModel 이 꺼낸다(512KB 글이 프로세스 사이 전달 한도에 닿는다).
- **나가기 확인은 셸 뒤로가기 ④ 의 지킴이다.** 바뀐 편집기면(`EditorUiState.guardsExit`) 셸이 `NavDisplay` 뒤에 둔 핸들러가
  `EditorIntent.Close` 를 보낸다. 편집기 화면 안에 `BackHandler` 를 두지 않는다 — 7단 표에 새 단이 생기고 예측 뒤로 미리보기가 돈 뒤에 막힌다.
- **저장 · 지움 뒤 스택은 `:core:navigation` 의 순수 함수**(`savedNoteCommand` · `withoutNoteCommand`)로 한 번에 계산해 보낸다.
  지운 뒤 퀴즈는 `FeatureSignals.closeQuiz()` 로 닫는다. 연결 전이 정리(`:app` `ScreenCleanup`)와는 따로다.
- **파일 고르기 런처는 `EditorHost` 에만.** 대화상자 안처럼 사라지는 자리에 두면 고르기 결과가 돌아올 곳이 없다. MIME 은 넓게(`*/*`) 열고
  받는 종류는 표시 이름 확장자로 판정한다(`ImportNoteUseCase`).
- 문구 접두사는 `editor_`(feature 컨벤션). 리더 상단 바의 `고치기` · `지우기` 와 지우기 확인 버튼은 `:core:ui` 의 `ui_action_*` 를 쓴다.

## 시험

- JVM `EditorViewModelTest` — LN-3 ⑥ · LN-6 · LN-7 · LN-8 · LN-9 · LN-10 · LN-11 의 ViewModel 쪽(가짜 Repository · 가짜 가져오기 규칙).
  가져오기 규칙 자체는 `:core:format` `DeviceNoteMarkdownTest`, 거절 판정은 `:core:data` `NoteImportTest` 가 본다.
- 스택 계산은 `:core:navigation` `EditorCommandsTest`, 셸 지킴 · 크롬은 `:app` `BackOrderTest`(계측) · `HeaderTest` 다.

## 아직 골격인 것

화면 몸통(칸 모양 · 쓰기/미리보기 전환 · 이유 줄 · 키보드 규칙 · 대화상자 모양 · 새 아이콘)은 화면 단위가 채운다. 지금 화면은 목적지 · 배선을
세우는 자리다.
