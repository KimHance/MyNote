---
id: notes
name: Notes
aliases: [노트, 노트 기능, 홈 · 북마크 · 리더]
uses: [common, design-system, domain, navigation, ui]
units:
  - {id: home, name: Home, aliases: [홈, 홈 화면, 첫 실행 빈 상태, 연결 안내 띠, 최근 노트, home screen, first-run empty state], code: [src/main/java/com/hancekim/mynote/feature/notes/screen/HomeScreen.kt, src/main/java/com/hancekim/mynote/feature/notes/screen/HomeEmpty.kt, src/main/java/com/hancekim/mynote/feature/notes/screen/EmptyStateTitle.kt, src/main/java/com/hancekim/mynote/feature/notes/screen/connect/, src/main/java/com/hancekim/mynote/feature/notes/element/HomeBody.kt, src/main/java/com/hancekim/mynote/feature/notes/element/HomeEntry.kt, src/test/java/com/hancekim/mynote/feature/notes/element/HomeBodyTest.kt, src/test/java/com/hancekim/mynote/feature/notes/screen/EmptyStateTitleTest.kt]}
  - {id: list-panel, name: List panel, aliases: [목록 패널, 사이드 시트, 드로어, 검색창, 설정 줄, side sheet, drawer, search], code: [src/main/java/com/hancekim/mynote/feature/notes/screen/ListPanelScreen.kt]}
  - {id: reader, name: Reader, aliases: [리더, 노트 읽기, 읽는 화면, 본문 견본, reading screen], code: [src/main/java/com/hancekim/mynote/feature/notes/screen/NoteReaderScreen.kt, src/main/java/com/hancekim/mynote/feature/notes/navigation/, src/test/java/com/hancekim/mynote/feature/notes/screen/ReaderTitleTest.kt, src/debug/]}
  - {id: bookmarks, name: Bookmarks, aliases: [북마크, 북마크 화면, 큰 카드, 번호 행, 북마크 해제, bookmark list, 북마크한 노트, 북마크 목록], code: [src/main/java/com/hancekim/mynote/feature/notes/screen/bookmarks/, src/main/java/com/hancekim/mynote/feature/notes/mapper/BookmarkMapper.kt, src/test/java/com/hancekim/mynote/feature/notes/mapper/BookmarkMapperTest.kt]}
  - {id: state, name: Notes state, aliases: [노트 ViewModel, 노트 상태, 동기화 구독, 읽기 기록 배선, 검색 필터, 북마크 토글, 북마크 해제, 북마크 되돌리기, notes view model, bookmark toggle, undo bookmark removal], code: [src/main/java/com/hancekim/mynote/feature/notes/NotesViewModel.kt, src/main/java/com/hancekim/mynote/feature/notes/NotesUiModel.kt, src/main/java/com/hancekim/mynote/feature/notes/element/CategoryLoad.kt, src/main/java/com/hancekim/mynote/feature/notes/element/NotesIntent.kt, src/main/java/com/hancekim/mynote/feature/notes/element/NotesUiState.kt, src/main/java/com/hancekim/mynote/feature/notes/mapper/NotesUiMapper.kt, src/test/java/com/hancekim/mynote/feature/notes/NotesViewModelTest.kt, src/test/java/com/hancekim/mynote/feature/notes/FeatureFixture.kt, src/test/java/com/hancekim/mynote/feature/notes/element/CategoryLoadTest.kt, src/test/java/com/hancekim/mynote/feature/notes/element/HeroRepoTest.kt, src/test/java/com/hancekim/mynote/feature/notes/mapper/NotesUiMapperTest.kt]}
  - {id: destinations, name: Notes destinations, aliases: [목적지 등록, 등록 파일, notesDestinations], code: [src/main/java/com/hancekim/mynote/feature/notes/NotesDestinations.kt]}
---
## Overview
Notes owns the screens a person reads notes on: the home grid with its empty states, the list panel with search, the reader body, and the Bookmarks tab. It also holds the one view model that lists notes, filters them, subscribes to sync, keeps bookmarks and feeds the reading record.
It deliberately does not own the shell (scaffold, drawer or fixed-pane frame, back order, reader top bar with the bookmark toggle, edit and delete buttons, snackbar host, start decision), the reader-end quiz card (a slot that the shell fills), the connect screens, settings, statistics or the editor.
Navigation keys, scene strategy and shared-element wiring live in `navigation`; reusable widgets (note card, rows, chips, hero, note renderer) live in `design-system`; screen pieces shared with other features live in `ui`.

## Structure
- `destinations` registers Home, Bookmarks and Reader for the shell; each destination reads the notes state inside its own lambda, so other features' state changes do not redraw it.
- `state` is the only owner of note data: it builds the state that `home`, `list-panel`, `reader` and `bookmarks` draw, and takes their intents.
- `home` and `bookmarks` are tab roots; `reader` opens from a card, a panel row or a bookmark; `list-panel` is shown by the shell as a drawer (Compact) or a fixed pane (Medium and up).
- Intents that leave this module go out as navigator commands or `ui` signals: new note → editor key, Import → a signal the editor's import sheet receives, settings and connect → navigator.
- The shell supplies scroll states, content padding, the two-column decision and the reader-end slot; this module reads them and does not decide them.
```mermaid
flowchart LR
  destinations --> home
  destinations --> bookmarks
  destinations --> reader
  home --> state
  list-panel --> state
  bookmarks --> state
  reader --> state
```
