---
id: connect
name: Repository connect
aliases: [저장소 연결, 연결 화면, 저장소 바꾸기, 연결 확인 대화상자]
uses: [common, design-system, domain, navigation, ui]
units:
  - {id: flow, name: Connect screen and flow, aliases: [연결 화면, 저장소 연결, 저장소 바꾸기, 연결 흐름, 토큰 카드], code: [src/main/java/com/hancekim/mynote/feature/connect/screen/, src/test/java/com/hancekim/mynote/feature/connect/screen/, src/main/java/com/hancekim/mynote/feature/connect/element/ConnectBack.kt, src/test/java/com/hancekim/mynote/feature/connect/element/ConnectBackTest.kt]}
  - {id: issues, name: Link checks and failure mapping, aliases: [링크 판정, 연결 실패 갈래, 주소 이유], code: [src/main/java/com/hancekim/mynote/feature/connect/element/, src/test/java/com/hancekim/mynote/feature/connect/element/]}
  - {id: entry, name: Template entry, aliases: [입구 구역, 저장소 만들기, 형식 안내 보기, 목록 없음 상자의 길], code: [src/main/java/com/hancekim/mynote/feature/connect/screen/ConnectEntry.kt, src/main/java/com/hancekim/mynote/feature/connect/element/ConnectEntry.kt, src/test/java/com/hancekim/mynote/feature/connect/element/ConnectEntryTest.kt]}
  - {id: confirm, name: Change and take-over dialog, aliases: [바꾸기 대화상자, 받아들이기 대화상자, 이어받기, 저장소를 바꿀까요], code: [src/main/java/com/hancekim/mynote/feature/connect/screen/ConnectDialogs.kt, src/main/java/com/hancekim/mynote/feature/connect/element/ConnectConfirm.kt, src/test/java/com/hancekim/mynote/feature/connect/element/ConnectConfirmTest.kt]}
  - {id: reading, name: Start reading screen, aliases: [읽는 중 화면, 연결 정보 읽는 중], code: [src/main/java/com/hancekim/mynote/feature/connect/screen/ConnectReading.kt]}
---
## Overview
`connect` owns the screen where a person pastes a GitHub repository or list address, the check that follows, the token request when the repository is not visible, the first sync, and the two confirm dialogs (change, take over). It owns the pure decisions behind them: what a pasted link looks like, which failure becomes which screen state, when the template entry shows.
It does not own the check, the first sync, the confirm transaction or disconnecting (use cases in `domain`, implemented in `data`), the home connect band and hint (`notes`), the settings repository card, disconnect dialog and token editor (`settings`), or which screen shows at start and back order (`app`).
It is not a start result: the screen opens only as the stacked destination `Connect` from home's first-run button, the home hint and settings `바꾸기`.
## Structure
- `flow` is the screen, its Activity-scoped ViewModel and the registration function `connectDestinations`; it asks `issues` how to read a pasted link and a failure, shows `entry` and, when a replacement is pending, `confirm`.
- `issues` and the pure parts of `entry`, `confirm` and the back rule are plain functions over domain results; the ViewModel calls them and the screen only draws their answer.
- `reading` is a separate screen the shell shows while the start branch is undecided; it has no state and no input.
- Shared boxes, fields, buttons, dialog frame and browser opening come from `ui:parts`; token draft and failure kinds from `ui:token`; the top bar from `ui:shell`.
- Callers: `app` registers `connectDestinations` and calls `ConnectReadingScreen`; the ViewModel is obtained with the Activity as owner so the three entry points share one state.
```mermaid
flowchart LR
  flow --> issues
  flow --> entry
  flow --> confirm
  app --> flow
  app --> reading
```
