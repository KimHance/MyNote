# MyNote

> GitHub 의 JSON 노트를 받아 "블로그 글처럼" 보는 **읽기 전용** 안드로이드 학습 노트 앱.

다크 단일 테마 · 멀티 폼팩터(반응형) · 앱 내 쓰기 없음.

콘텐츠는 **서버드리븐**이다. 레포 루트 `notes/` 의 `index.json`(group→노트 + 해시)과 노트별 JSON 을
**GitHub Contents API**(private 레포 + 토큰)로 받아 **Room 에 캐싱**하고, 해시를 비교해 변경분만 동기화한다
(없으면 받고 · 해시 다르면 갱신 · index 에 없으면 삭제).

## 화면 (`feature:notes`)

| 화면 | 역할 |
|---|---|
| HomeScreen | 카테고리 · 최근 노트 진입점 |
| ListPanelScreen | 노트 목록 (목록-상세의 list pane) |
| NoteReaderScreen | 노트 본문 렌더 (서버드리븐 블록) |

본문 블록: `heading · paragraph · code · quote · bulletList · orderedList · divider · spacer` (닫힌 집합, 스키마는 [`notes/SCHEMA.md`](notes/SCHEMA.md)).

## 기술 스택

| 영역 | 기술 |
|---|---|
| 언어 / UI | Kotlin · Jetpack Compose · Material3 |
| 프레젠테이션 | MVI (`UiState` + `Intent` + `onIntent()`) |
| 내비게이션 | Navigation 3 + Material3 adaptive (`ListDetailSceneStrategy`, 2-pane) |
| DI | Hilt |
| 네트워크 | Retrofit · OkHttp(`AuthInterceptor`) · GitHub Contents API |
| 로컬 | Room 3 (노트 캐시 · SSOT) · DataStore (토큰) |
| 직렬화 | kotlinx.serialization (polymorphic blocks) |
| 비동기 / 컬렉션 | Coroutines · Flow · kotlinx-collections-immutable |
| 빌드 | AGP 9.2.1 · Kotlin 2.3.21 · convention plugins(`:build-logic`) · version catalog |

compileSdk 37 · targetSdk 36 · minSdk 32

## 모듈 구조

```
app                     Application(Hilt), MainActivity, NotesRoute 호스트, 레포좌표·TokenSync
core/
  design-foundation     디자인 토큰(색·타이포·간격), AdaptiveTokens(반응형 5단계)
  design-system         MyNoteTheme, 공용 컴포넌트, 본문 블록 렌더
  domain                모델 + Repository 인터페이스(DIP 소유) + UseCase
  data                  DataSource 인터페이스 + DTO·Mapper + reconcile + Hilt
  network               GitHub Contents API, 토큰 인터셉터/홀더
  database              Room 노트 캐시 + DataStore 토큰 저장
feature/notes           NotesViewModel/UiState/Intent, 화면 3개, Nav3 그래프
```

의존 방향은 항상 바깥→안쪽: `network`/`database` → `data` → `domain`. 인터페이스는 안쪽이 소유하고
바깥이 구현하며, `:app` 만 구현체를 알고 Hilt 로 바인딩한다(클린 아키텍처 / DIP). **SSOT 는 Room.**

## 빌드 & 실행

```bash
./gradlew :app:compileDebugKotlin      # 컴파일 검증
./gradlew :core:data:testDebugUnitTest # 단위 테스트(reconcile·직렬화)
./gradlew :app:assembleDebug           # 디버그 APK
python3 notes/gen_index.py             # 노트 수정 후 index.json 재생성(검증 + 해시)
```

**토큰**: private 레포 read 용 fine-grained PAT 가 필요하다. release 는 앱에서 런타임 입력(자물쇠 아이콘),
debug 는 `local.properties` 의 `github.token` 을 내장한다(둘 다 git 에 올라가지 않음).

**Package:** `com.hancekim.mynote`
