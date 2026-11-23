---
id: navigation
name: Navigation
aliases: [내비게이션, 네비게이션, 백스택, 목적지 키, nav3, Navigation 3]
uses: [common]
units:
  - {id: navigator, name: Navigator, aliases: [백스택 소유자, 쓰로틀, 떠나기 지킴, 이동 명령], code: [src/main/java/com/hancekim/mynote/core/navigation/Navigator.kt, src/main/java/com/hancekim/mynote/core/navigation/NavCommand.kt, src/main/java/com/hancekim/mynote/core/navigation/NavResult.kt, src/test/java/com/hancekim/mynote/core/navigation/NavigatorTest.kt]}
  - {id: keys, name: Destination keys, aliases: [목적지 키, NavKey, 화면 키], code: [src/main/java/com/hancekim/mynote/core/navigation/key/, src/test/java/com/hancekim/mynote/core/navigation/key/]}
  - {id: commands, name: Stack commands, aliases: [리더 열기, 탭 전환, 편집기 열기, 스택 계산], code: [src/main/java/com/hancekim/mynote/core/navigation/OpenReader.kt, src/main/java/com/hancekim/mynote/core/navigation/TabSwitch.kt, src/main/java/com/hancekim/mynote/core/navigation/NotesTab.kt, src/main/java/com/hancekim/mynote/core/navigation/EditorCommands.kt, src/test/java/com/hancekim/mynote/core/navigation/OpenReaderTest.kt, src/test/java/com/hancekim/mynote/core/navigation/TabSwitchTest.kt, src/test/java/com/hancekim/mynote/core/navigation/EditorCommandsTest.kt]}
  - {id: scenes, name: Scenes and transitions, aliases: [씬, 2단 씬, 목록 상세, 전환 모션, 예측 뒤로가기], code: [src/main/java/com/hancekim/mynote/core/navigation/scene/, src/main/java/com/hancekim/mynote/core/navigation/motion/, src/test/java/com/hancekim/mynote/core/navigation/scene/]}
  - {id: registry, name: Destination registry, aliases: [목적지 등록, 등록 틀], code: [src/main/java/com/hancekim/mynote/core/navigation/registry/, src/test/java/com/hancekim/mynote/core/navigation/registry/]}
  - {id: shared-elements, name: Shared elements, aliases: [공유 요소, 공유 전환], code: [src/main/java/com/hancekim/mynote/core/navigation/shared/]}
---
## Overview
Navigation is the contract module for moving between screens: the destination keys, the one owner of the back stack, the commands that compute new stacks, the scene strategy and transition motion, the registration frame for destinations and the shared-element wiring.
Keys live here so that feature modules, which must not know each other, can still go to each other's screens, and one shell can put all of them on one stack. The module imports no design system, domain, data, Material or Hilt; scenes take their panel, header and sizes as slots from the app shell.
It does not draw any screen and holds no theme. It depends only on the Navigation 3 libraries, Compose foundation, serialization, coroutines and `common` (for the throttler).
## Structure
- `keys` is the vocabulary; `navigator` holds the stack of keys and `commands` compute the next stack from the current one, so callers send one command per user action.
- `scenes` draws the stack: one display, with the strategy choosing between a stacked single pane and a two-pane list-detail scene; `registry` and `shared-elements` are what a feature uses to register a screen and take part in shared transitions.
- The navigator's throttle window comes from the scene motion numbers, so a longer transition lengthens the window with it.
- Callers: the app shell (navigator, display, strategy, provider), every feature module (registrations, commands, keys, shared-element modifiers) and the feature ViewModels that dispatch.
```mermaid
flowchart LR
  commands --> keys
  navigator --> keys
  navigator --> scenes
  registry --> shared-elements
  scenes --> shared-elements
  registry --> keys
```
