---
id: ui
name: Shared UI
aliases: [공용 화면 조각, 코어 UI, core:ui]
uses: [common, design-system, domain, navigation]
units:
  - {id: signal, name: Feature signals, aliases: [기능 사이 신호, 공유 받은함, 마지막 동기화, 가져오기 요청, 공유 받은 텍스트, SharedTextInbox, FeatureSignals], code: [src/main/java/com/hancekim/mynote/core/ui/signal/]}
  - {id: notices, name: Snackbar notices and failure log, aliases: [스낵바 문구, 실패 로그, 일회성 신호, 알림, 북마크 해제 문구, NotesMessages], code: [src/main/java/com/hancekim/mynote/core/ui/FailureLog.kt, src/main/java/com/hancekim/mynote/core/ui/NotesMessages.kt, src/main/java/com/hancekim/mynote/core/ui/element/NoticeUi.kt, src/main/java/com/hancekim/mynote/core/ui/element/NotesSignal.kt, src/test/java/com/hancekim/mynote/core/ui/element/NotesSignalTest.kt]}
  - {id: shell, name: Shell frame and top bar, aliases: [셸 틀, 상단 바, 스크롤 상태, 맨 위로, 씬 가장자리, 뒤로 화살표, 새로고침 버튼, 고치기 지우기 버튼, ShellFrame], code: [src/main/java/com/hancekim/mynote/core/ui/frame/, src/main/java/com/hancekim/mynote/core/ui/shell/]}
  - {id: mapper, name: Block and font-scale mappers, aliases: [블록 변환, 리더 블록 매핑, 글자 크기 매핑, 중첩 목록, 표 정렬, toContentBlocks], code: [src/main/java/com/hancekim/mynote/core/ui/mapper/, src/test/java/com/hancekim/mynote/core/ui/mapper/, src/main/java/com/hancekim/mynote/core/ui/element/FontScaleUi.kt]}
  - {id: model-status, name: Quiz model status models, aliases: [모델 카드, 모델 알약, 퀴즈 CTA, 모델 상태, 용량 부족 문구, 진행률 알약, 받기 이어 받기 삭제, download pill], code: [src/main/java/com/hancekim/mynote/core/ui/element/ModelStatusUi.kt, src/main/java/com/hancekim/mynote/core/ui/screen/ModelCardParts.kt, src/test/java/com/hancekim/mynote/core/ui/element/ModelStatusUiTest.kt]}
  - {id: repo-card, name: Repository card state, aliases: [저장소 카드, 마지막 동기화 결과, 토큰 확인 필요, 연결 없음, 접근 불가, 요청 한도, 동기화 실패 종류, SyncFailureKind], code: [src/main/java/com/hancekim/mynote/core/ui/element/RepoCard.kt, src/test/java/com/hancekim/mynote/core/ui/element/RepoCardTest.kt]}
  - {id: token, name: Token draft and failures, aliases: [토큰 입력 홀더, 토큰 실패 종류, 토큰 draft], code: [src/main/java/com/hancekim/mynote/core/ui/element/TokenDraft.kt, src/main/java/com/hancekim/mynote/core/ui/element/TokenFailure.kt, src/test/java/com/hancekim/mynote/core/ui/element/TokenDraftTest.kt, src/test/java/com/hancekim/mynote/core/ui/element/NotesDraftsTest.kt]}
  - {id: parts, name: Shared screen parts, aliases: [공용 버튼, 알림 상자, 대화상자, 한글 어절 줄바꿈, 외부 브라우저 열기, 토큰 입력 칸, 유리 대화상자, ActionDialog, 진행 링, glass dialog], code: [src/main/java/com/hancekim/mynote/core/ui/screen/, src/test/java/com/hancekim/mynote/core/ui/screen/]}
  - {id: bookmark-time, name: Bookmark time label, aliases: [북마크 시각 라벨, 북마크 시간, 북마크 날짜, 어제 라벨], code: [src/main/java/com/hancekim/mynote/core/ui/screen/bookmarks/, src/test/java/com/hancekim/mynote/core/ui/screen/bookmarks/]}
---
## Overview
`ui` is where anything two or more feature modules share lives, because feature modules do not depend on each other: screen pieces that know feature meaning (model status, repository card, token input), the contract between the shell and registered screens, the signals features send each other, snackbar wording, and the mapper from domain note blocks to reader blocks.
It deliberately does not own navigation commands (tab kinds, open-reader, destination keys: `navigation`), the design system's meaning-free components (`design-system`), or the shell itself (`app`). It knows no feature module, data layer or model implementation, and uses no Hilt plugin (only `javax.inject` annotations; `app` builds the graph).
Strings are prefixed `ui_`; a string is here when this module's code uses it or two or more modules use it.
## Structure
- `shell` defines what a registered screen receives from the shell (`ShellFrame`) and owns the scroll-state holder and the top bar; `app` implements the frame and every feature's registration file consumes it.
- `signal` and `notices` carry what features say to each other and to the user; every feature ViewModel and the shell ViewModel read them, `app` binds the Android implementations.
- `model-status`, `repo-card` and `token` turn domain state into card, pill, badge and field state; `settings`, `notes`, `quiz`, `connect` and the shell draw them.
- `parts` holds the buttons, boxes, fields and dialog frame used by connect, settings, notes and editor; `bookmark-time` is a label formatter used by notes and stats.
- `mapper` is used by the reader (`notes`) and the editor preview (`editor`), so both draw a note the same way.
```mermaid
flowchart LR
  shell -->|ShellFrame carries drafts| token
  signal -->|LastSyncHolder holds LastSync| repo-card
```
