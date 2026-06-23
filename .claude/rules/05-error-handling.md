# 에러 처리

## 비동기 / Flow
- 실패 가능한 IO/UseCase 호출은 `runCatching` 또는 Flow `catch` 로 감싸고 성공/실패를 분리한다. 예외가 `scope.launch { }` 밖으로 조용히 새지 않게 한다.
- 예: `getCategories().catch { emit(emptyList()) }` — 로드 실패 시 빈 목록으로 폴백.

## 사용자 노출
- 복구 가능한 안내는 `SnackbarHostState`(Scaffold 의 `snackbarHost`). **`Toast` 금지.**
- 치명적/빈 상태는 전용 empty/error Composable 로. 원시 예외 메시지를 그대로 노출하지 않는다.

## 로깅
- PoC 단계는 로깅 라이브러리 미도입. 네트워크 레이어 도입 시 Timber 등을 추가하고 `Log.d/e` 직접 호출은 지양한다.
