# :core:domain — Claude Code Instructions

가장 안쪽 레이어. 순수 Kotlin 모델 + Repository 인터페이스 + UseCase. 의존 없음(가장 안쪽).

## 하드 룰
- **Android/Compose 의존 금지.** Hilt 플러그인도 불필요 — DI 는 `javax.inject`(`@Inject`)만 사용,
  UseCase 는 unscoped 생성자 주입.
- **인터페이스를 domain 이 소유한다(DIP).** `NotesRepository`(observe/sync)는 여기에 두고,
  구현은 `:core:data` 가 domain 을 의존하며 구현한다. domain 은 data 를 모른다.
- 도메인 모델(`Category`/`Note`/`NoteBlock`)은 순수 데이터 클래스. 직렬화 어노테이션·Compose stability
  어노테이션을 붙이지 않는다(그건 각각 data DTO / UI 레이어의 책임). 여기선 평범한 `List` 사용 가능.
- UseCase 는 단일 책임 `operator fun invoke()`. 지금은 Repository 위임뿐이어도, 정렬·필터·병합 같은
  도메인 규칙이 생기면 여기에 둔다. feature 는 이 UseCase(+도메인 모델)만 본다.

## 구성
- `model/` — `Category`, `Note`, `NoteBlock`(sealed) 등 순수 모델
- `repository/NotesRepository.kt` — `observeCategories(): Flow<List<Category>>` + `suspend sync(): Result<Unit>`
- `GetCategoriesUseCase`(observe 위임), `SyncNotesUseCase`(sync 위임)
