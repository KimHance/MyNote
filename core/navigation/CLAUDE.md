# :core:navigation — Claude Code Instructions

**내비게이션 계약 모듈.** 백스택 키, 씬/씬 전략, 전환 모션, 공유 요소 배선만 둔다.
의존: nav3(`api` 로 전파) + compose foundation. **테마·도메인·데이터를 의존하지 않는다.**

## 존재 이유

feature 마다 자기 키를 들면 feature 끼리 서로의 목적지를 몰라 이동할 수 없고, 그렇다고
feature ↔ feature 직접 의존은 모듈 규칙 위반이다. **키만 이 계약 모듈로 올려** 각 feature 는
서로를 모른 채로 두고, `:feature:root` 가 하나의 백스택으로 전부 태운다.

## 하드 룰
- **새 feature 의 목적지 키도 여기에 추가한다.** `MyNoteNavKey` 가 `sealed` 라 모듈 경계를 못 넘는다.
  그 대가로 root 는 `when` 을 빠짐없이 검사받는다.
- **테마를 모른다.** 씬은 폭·색·구분선을 스스로 정하지 않고 `panel`/`header` 슬롯에 맡긴다.
  여기서 `MyNoteTheme` 를 import 하게 되면 설계가 틀어진 것이다.
- **feature 이름이 박힌 API 를 만들지 않는다**(`notesXxx` 금지). 예외는 키(`NotesNavKey`)와
  그 키에서 백스택을 만드는 함수뿐 — 그건 계약 자체라 여기 있는 게 맞다.
- Compose 의존은 foundation 까지. material3 를 끌어오지 않는다.

## 구성
| 경로 | 내용 |
|---|---|
| `key/` | `MyNoteNavKey`(sealed 루트) · `NotesNavKey` · 백스택 파생 함수 |
| `scene/` | `ListDetailScene`/`ListDetailSceneStrategy` · `rememberSceneEdgeTransform` |
| `motion/` | `SceneMotion` 수치 + `sceneTransitionSpec`/`scenePop…`/`scenePredictivePop…` |
| `shared/` | `LocalSharedTransitionScope` · `LocalNavAnimatedScope` · `ProvideNavAnimatedScope` · 공유 Modifier 헬퍼 |

## 씬 설계에서 반드시 지킬 것
- **`ListDetailScene` 의 `key` 는 상세가 바뀌어도 고정.** 그래야 `NavDisplay` 가 씬을 통째로
  슬라이드시키지 않고 씬이 본문만 페이드 스루한다. `previousEntries = entries.dropLast(1)` 이라
  뒤로가기는 살아 있다. — `ListDetailSceneStrategyTest` 가 고정한다.
- **씬 슬롯(`panel`/`header`/`panelExpanded`)은 호출부에서 `remember` 로 정체성을 고정**해 넘긴다.
  매 컴포지션 새 람다가 들어오면 전략이 바뀐 것으로 보여 씬이 통째로 재생성된다.
- **전략을 리스트에 넣고 빼는 것만으로 2단 레이아웃 ↔ 스택 레이아웃이 갈린다.** `null` 을 돌려주면
  `NavDisplay` 기본 `SinglePaneSceneStrategy` 로 떨어진다.
- 예측 뒤로가기는 `NavDisplay` 가 `SeekableTransitionState` 로 직접 seek 한다 —
  **`PredictiveBackHandler` 를 따로 달지 않는다.**
  > 커밋 이후 마무리 애니메이션도 `scenePredictivePopTransitionSpec` 그대로 이어서 재생된다 →
  > 진행률 1 에서 화면 밖으로 완전히 빠지는 값이어야 한다.
- `EnterTransition` 으로 못 그리는 값(모서리 반경·그림자)은 `rememberSceneEdgeTransform` 이
  엔트리의 `AnimatedContentScope` 트랜지션을 읽어 `graphicsLayer` **람다 안**에서 계산한다(draw phase 격리).

## 공유 요소
- `LocalNavAnimatedContentScope`(nav3)를 화면에서 직접 읽지 않는다 — 미설정 시 **예외를 던져**
  프리뷰가 깨진다. 엔트리를 `ProvideNavAnimatedScope` 로 감싸 nullable 인 `LocalNavAnimatedScope` 로 바꾼다.
- 스코프가 없으면 헬퍼가 빈 `Modifier` 를 돌려주므로 프리뷰에서도 그대로 렌더된다.
- 키 문자열의 도메인 의미(`chip-`/`title-` 등)는 feature 가 정한다. 여기는 배선만.

## Navigator — 스택 소유 + 입구 쓰로틀
- **스택을 소유하는 건 `Navigator` 하나.** ViewModel 은 `navigator.stack` 을 읽어 상태로 나를 뿐이고,
  `activeNoteId` 는 스택에서 파생한다. 소유자를 둘로 만들지 않는다.
- **스택을 바꾸는 길은 `dispatch(NavCommand)` 하나.** 화면 전환·`onBack`·시스템 뒤로가기가 전부
  여기로 들어온다. 입구가 하나여야 쓰로틀을 걸 자리가 하나가 된다.
- **버린다, 쌓지 않는다.** 전환 중 들어온 이동은 `NavResult.Throttled` 를 돌려주고 끝. 큐에 쌓으면
  누른 만큼 결국 다 이동해서 "연타로 빠르게 넘기지 못하게" 라는 목적이 사라진다.
- **`Throttled` 와 `Ignored` 를 절대 뭉개지 않는다.** 둘 다 "스택이 안 바뀌었다"지만 전자는 잠깐
  막힌 것이고 후자는 **더 뒤로 갈 데가 없다**는 뜻이라 앱 종료로 이어져야 한다. 하나로 합치면
  전환 중 뒤로가기가 종료로 샌다. — `NavigatorTest` 가 고정한다.
- **스택을 바꾸기 전에 막는다.** 바꾸고 되돌리면 그사이 `NavDisplay` 가 전환을 시작해 어긋난다.
- **no-op 은 창을 열지 않는다.** 맨 위와 같은 키를 또 push 하거나 루트에서 pop 하면 이동이 없었던
  것이니 막을 것도 없다. 창을 열어 버리면 다음 진짜 이동이 헛되이 죽는다.
- `dispatchImmediately` 는 **사용자 입력이 아닌 이동 전용**(삭제된 노트 정정·딥링크). 연타가 아니라
  정정이라 막으면 유효하지 않은 화면이 남는다.
- 창 길이는 `SceneMotion` 에서 끌어온다. 매직넘버로 두면 모션을 늘렸을 때 쓰로틀만 짧게 남는다.
- **Hilt 를 의존하지 않는다.** 바인딩은 composition root(`:app/di/NavigationModule`)가 소유한다.

## 앞으로 (`:feature:root`)
feature 가 여러 개가 되면 `MyNoteNavKey` 에 키를 추가하고 root 가 하나의 `Navigator` 로 전부
태운다. 스택 소유권은 이미 여기 있으므로 그때 옮길 것은 백스택이 아니라 **화면 조립**뿐이다.
