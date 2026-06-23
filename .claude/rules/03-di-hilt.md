# DI (Hilt)

## 스코프 / 주입
- **Application:** `@HiltAndroidApp class MyNoteApplication`.
- **ViewModel:** `@HiltViewModel class XxxViewModel @Inject constructor(useCase: ...)`. 화면에서 `hiltViewModel()` (`androidx.hilt.lifecycle.viewmodel.compose`).
- **Repository:** `@Binds` 로 인터페이스→구현 매핑, `SingletonComponent`. 위치: `:core:data/di/`.
- **UseCase:** 순수 생성자 주입(`class GetCategoriesUseCase @Inject constructor(...)`). `:core:domain` 에는 Hilt plugin 불필요(javax.inject 만), unscoped.

## 규칙
- 인터페이스→구현은 `@Binds`(abstract). 생성 로직이 필요할 때만 `@Provides`.
- `@Module @InstallIn(SingletonComponent::class)`. `:core:data` 의 `DataModule` 은 `:core:domain → api(:core:data)` 로 그래프에 포함된다.
