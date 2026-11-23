---
id: settings
name: Settings
aliases: [설정, 설정 화면, settings screen]
uses: [common, design-system, domain, navigation, ui]
units:
  - {id: settings, name: Settings screen, aliases: [설정, 읽기, 본문 글자 크게, 글자 크기, 학습 기록 남기기, 기록 지우기, SettingsViewModel], code: [src/]}
  - {id: repo, name: Repository card and token, aliases: [노트 저장소, 저장소 칸, 액세스 토큰, 토큰 편집, 저장소 바꾸기, 연결 끊기], code: [src/main/java/com/hancekim/mynote/feature/settings/screen/RepoSection.kt, src/main/java/com/hancekim/mynote/feature/settings/screen/DisconnectDialog.kt, src/main/java/com/hancekim/mynote/feature/settings/element/, src/test/java/com/hancekim/mynote/feature/settings/element/]}
  - {id: model, name: Quiz model card, aliases: [퀴즈 모델, 모델 카드, 모델 받기, 모델 삭제, 퀴즈 모델을 받을까요], code: [src/main/java/com/hancekim/mynote/feature/settings/screen/ModelCard.kt, src/main/java/com/hancekim/mynote/feature/settings/screen/ModelDialogs.kt]}
---
## Overview
Owns the Settings destination and its view model: the reading text size, the study-log switch and its clearing, the quiz model card with its download actions and confirm dialogs, and the notes repository card with the access-token editor, repository change entry and disconnect.
It keeps the raw token out of its state (only a masked form) and keeps the previous token for undo only in memory.
It does not store anything itself (stores and use cases are `domain` and the data modules), does not run the model download (`ai`), does not hold the token draft (the `ui` drafts holder kept by the `app` shell), and does not connect repositories (`connect`).
## Structure
- `settings` is `SettingsViewModel` (Activity-scoped), its state and intents, the registration `settingsDestinations` for the `Settings` key, and the screen frame that stacks the sections in the order reading, study log, quiz model, notes repository.
- `repo` draws the repository card and the token editor from the view model state and holds the pure token-edit transition table, the token mask, the link used for a token check and the check-failure mapping the view model applies.
- `model` draws the model card from the shared card decision in `ui` and the three glass confirm dialogs; `ModelConfirmDialog` is also called by the `app` shell for the reader-end quiz card.
- The `app` shell supplies whether system notifications are on, opens the notification settings, and receives the view model's snackbar notices.
```mermaid
flowchart LR
  app[app shell] --> settings
  settings --> repo
  settings --> model
  app -- ModelConfirmDialog --> model
  repo -- intents --> settings
  model -- intents --> settings
```
