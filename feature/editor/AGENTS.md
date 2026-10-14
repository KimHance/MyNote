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
| `EditorUiState.kt` | 상태 · 세션 · 인텐트 · 이유 줄 종류(`SaveBlock` · `PasteBlock`) |
| `Utf8Size.kt` | 칸 글의 UTF-8 바이트 · KB 올림 — 이유 줄 숫자와 붙여넣기 512KB 판정용(저장 판정은 UseCase) |
| `EditorMessages.kt` | 스낵바 · 기본 그룹 문구(FM-8 — ViewModel 은 문구를 직접 읽지 않는다. `:app` `NotesModule` 이 묶는다) |
| `EditorDestinations.kt` | 편집기 · 붙여넣기 등록 |
| `EditorHost.kt` | 파일 고르기 런처 · 가져오기 대화상자(길 두 줄 · 읽는 중) · 나가기 확인 · 지우기 확인(`:core:ui` `GlassDialog` 틀). 셸이 시작 분기 위에서 늘 컴포즈한다 |
| `screen/EditorScreen.kt` | 편집기 — 자기 상단 바 · 제목 칸 · 그룹 줄 · (고치기) 메타 줄 · `쓰기`/`미리보기` 전환 · 이유 줄 · 본문 칸 또는 미리보기 |
| `screen/PasteScreen.kt` | 붙여넣기 — 자기 상단 바 · `가져올 글` 큰 칸(mono) · 이유 줄 |
| `screen/EditorParts.kt` | 두 화면이 같이 쓰는 상단 바 · 면 없는 칸 · 밑줄 · 이유 줄 · 전환 |

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

- JVM `EditorViewModelTest` — LN-3(⑥ · 이유 줄 · 한 줄 칸) · LN-6 · LN-7(붙여넣기 크기) · LN-8 · LN-9 · LN-10 · LN-11 의 ViewModel 쪽(가짜 Repository ·
  가짜 가져오기 규칙). `Utf8SizeTest` 가 이유 줄 숫자의 바이트 · KB 올림을 본다.
  가져오기 규칙 자체는 `:core:format` `DeviceNoteMarkdownTest`, 거절 판정은 `:core:data` `NoteImportTest` 가 본다.
- 스택 계산은 `:core:navigation` `EditorCommandsTest`, 셸 지킴 · 크롬은 `:app` `BackOrderTest`(계측) · `HeaderTest` 다.

## 화면 규칙

모양의 기준은 디자인 문서 `docs/design/local-notes.md`(22B · 제약 C1 ~ C19 · 문구)와 시안 해부(`labs/local-notes-design/r1/README.md` §3.2 ·
`README-2.md` §3.1 ~ §3.3)다.

- **미리보기는 리더 렌더러 그대로다.** `PreviewDeviceNoteUseCase` 의 블록을 `:core:ui` `toContentBlocks` 로 바꿔 `NoteContent` 로 그린다(한도 상자
  포함 — C5). 해석은 전환할 때 한 번이고 결과가 오기 전엔 진행 막대다(C10). 입력마다 해석하지 않는다.
- **이유 줄 값은 ViewModel 이 든다.** 화면은 `saveBlock` · `pasteBlock` · `bodyKilobytes` 를 받아 그린다. 본문 KB 는 크기를 넘었을 때만 센다 —
  512KB 본문을 글자마다 세지 않게.
- **키보드** — 루트가 `safeDrawing` 인셋(IME 포함)을 먹고, 상단 바만 고정 · 나머지 한 스크롤이다(C7). 본문 칸은 최소 높이로 빈 아래를 눌러도 닿게 한다.
  새 노트 · 가져온 글은 제목 칸, 붙여넣기는 큰 칸에 처음 한 번 포커스를 준다(고치기는 키보드를 열지 않는다).
- **아이콘은 디자인 시스템 손그림 한 벌**(`MyNoteIcons.Import` · `Paste` · `Device` · `Edit` · `Delete` — C2). Material 세트를 쓰지 않는다
  (붙여넣기 화면의 뒤로 화살표는 셸 상단 바와 같은 것).
- 가져오기 읽는 중에 `취소` 하면 읽던 일을 끊고 결과를 버린다(편집기 · 거절 스낵바 없음).
