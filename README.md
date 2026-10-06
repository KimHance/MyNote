# MyNote

> GitHub 저장소의 마크다운 노트를 받아 "블로그 글처럼" 보는 **읽기 전용** 안드로이드 학습 노트 앱.
> 노트로 온디바이스 LLM 퀴즈를 풀고, 공부 기록을 통계로 본다.

다크 단일 테마 · 멀티 폼팩터(반응형 · 폴더블) · 앱 내 쓰기 없음.

콘텐츠는 **서버드리븐**이다. 앱 안에서 GitHub 저장소 링크를 붙여 연결하고(공개는 바로 · 비공개는 읽기 전용 토큰),
`notes/index.json`(그룹 → 노트 + 해시)과 노트 파일을 **GitHub Contents API** 로 받아 **Room 에 캐싱**한다.
해시를 비교해 변경분만 동기화한다(없으면 받고 · 해시가 다르면 갱신 · index 에 없으면 삭제).
노트는 앞머리(제목 · 날짜) + GFM 마크다운 본문이고, 앱이 블록(제목 · 문단 · 코드 · 인용 · 목록 · 표 · 구분선)으로 그린다.
형식은 [`notes/SCHEMA.md`](notes/SCHEMA.md) 에 있다. 옛 JSON 노트도 읽는다.

## 기능 (`feature:notes`)

| 기능 | 내용 |
|---|---|
| 홈 · 목록 · 리더 | 카테고리 · 최근 노트, 목록-상세(2-pane), 서버드리븐 블록 렌더 |
| 북마크 | 노트 북마크 화면 |
| 통계 | 읽은 시간 · 퀴즈 결과 대시보드, 온디바이스 요약 카드 |
| 퀴즈 | 노트에서 4지선다 퀴즈를 온디바이스 LLM(LiteRT-LM · Gemma 4 E2B)으로 생성 |
| 퀴즈 모델 | 2.6GB 모델을 앱에서 받는다(WorkManager · 포그라운드 알림 · 이어 받기 · sha256 검증) |
| 저장소 연결 | 링크로 연결 · 바꾸기 · 끊기, 템플릿으로 새 저장소 만들기 입구 |
| 설정 | 글자 크기 · 학습 기록 · 모델 · 저장소 · 토큰 |

## 기술 스택

| 영역 | 기술 |
|---|---|
| 언어 / UI | Kotlin · Jetpack Compose · Material3 |
| 프레젠테이션 | MVI (`MviViewModel` · `UiState` + `Intent`) |
| 내비게이션 | Navigation 3 1.2.0 + Scenes(목록-상세) |
| DI | Hilt |
| 네트워크 | Retrofit · OkHttp · GitHub Contents API |
| 로컬 | Room 3 (노트 캐시 · 학습 기록 · SSOT) · DataStore |
| 온디바이스 AI | LiteRT-LM (퀴즈 · 통계 요약) · WorkManager (모델 받기 · 요약 워커) |
| 마크다운 | JetBrains markdown(GFM) |
| 직렬화 | kotlinx.serialization |
| 빌드 | Gradle 9.8 · AGP 9.4.1 · Kotlin 2.4.20 · KSP 2.3.12 · convention plugins(`:build-logic`) · version catalog |

compileSdk 37 · targetSdk 37 · minSdk 32. 버전의 정본은 [`gradle/libs.versions.toml`](gradle/libs.versions.toml) 이고
Gradle 은 `gradle/wrapper/gradle-wrapper.properties` 다.

## 모듈 구조

```
app                     composition root — Application(Hilt), MainActivity, 구현체 바인딩, 모델 받기 알림 조정
feature/notes           화면 · ViewModel · 퀴즈 · 통계 · 저장소 연결
core/
  ai                    LiteRT-LM 퀴즈 생성 · 통계 요약 워커 · 모델 받기
  network               GitHub Contents API · 저장소 확인 요청
  database              Room 캐시 · 학습 기록 · DataStore
  data                  DataSource 인터페이스 + reconcile(동기화)
  format                순수 JVM — 노트 원문 → 블록(형식 고르기 · 한도 · 마크다운 변환 · 옛 JSON 형식 1)
  domain                순수 JVM — 모델 + Repository 인터페이스(DIP 소유) + UseCase
  navigation            Navigation 3 계약
  design-system         MyNoteTheme · 공용 컴포넌트 · 본문 블록 렌더
  design-foundation     디자인 토큰(색 · 타이포 · AdaptiveTokens)
  common                MVI base · Throttler
public-repos/           남에게 공개할 노트 규격 · 도구 · 템플릿 저장소 원본(아직 공개 전)
```

의존 방향은 항상 바깥 → 안쪽이다. 인터페이스는 안쪽이 소유하고 바깥이 구현하며, `:app` 만 구현체를 알고
Hilt 로 묶는다(DIP). **SSOT 는 Room.** 에이전트용 안내는 [`AGENTS.md`](AGENTS.md)(모듈마다 따로),
디자인 기준은 [`DESIGN.md`](DESIGN.md), 기능 문서는 [`docs/spec/`](docs/spec/) 에 있다.

## 빌드 & 실행

```bash
./gradlew ktlintCheck :app:assembleDebug test lintDebug   # 한 번에 검증 (test — testDebugUnitTest 아님)
python3 -I -m unittest discover notes-tests && python3 -I -m unittest discover public-repos/tests && python3 -I -m unittest discover public-repos/notes-kit/tests
./gradlew :app:installDebug                               # 기기 설치 (uninstall 하면 기기의 모델 파일이 지워진다)
git config core.hooksPath .githooks                       # 머신당 1회 — 노트를 커밋하면 index.json 을 다시 만든다
```

**저장소 · 토큰**: 노트 저장소는 앱 안에서 링크로 연결한다. 비공개 저장소는 읽기 전용 fine-grained 토큰이 필요하다.
debug 빌드는 `local.properties` 의 `github.owner` · `github.repo` · `github.token` 이 있으면 첫 실행에 시드로 연결한다
(git 에 올라가지 않는다 · release 에는 들어가지 않는다).

**Package:** `com.hancekim.mynote`
