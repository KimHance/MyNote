# AGENTS.md — :core:navigation

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

---

## 이 모듈이 있는 이유

**내비게이션 계약 모듈.** 목적지 키 · 씬/씬 전략 · 전환 모션 · 공유 요소 배선 · 백스택 소유자만 둔다.

feature 마다 자기 키를 들면 feature 끼리 서로의 목적지를 몰라 이동할 수 없고, feature ↔ feature
직접 의존은 모듈 규칙 위반이다. 그래서 **키를 계약 모듈로 올려** 각 feature 가 서로를 모른 채 두고,
나중에 root 가 하나의 백스택으로 전부 태울 수 있게 했다. `MyNoteNavKey` 가 `sealed` 인 대가로
호출부의 `when` 은 빠짐없이 검사받는다 — **새 feature 의 키도 여기에 추가한다.**

### 경계 (실측)

`src` 전체에 design-system/design-foundation/domain/data/material3/Hilt import 가 **0건**이다.
씬은 폭·색·구분선을 스스로 정하지 않고 `panel`/`header` 슬롯에 맡긴다 — 실제로 `paneWidth`·
`colors.border` 는 feature 쪽 슬롯이 그린다. 여기서 `MyNoteTheme` 를 import 하게 되면 설계가 틀어진 것이다.

의존은 nav3(`api` 로 전파) + compose foundation + kotlinx-serialization + coroutines,
그리고 **`implementation(projects.core.common)`** — 쓰로틀 판정을 `:core:common` 의 `Throttler`/`Clock` 이
맡는다. 그래서 이 모듈은 완전한 leaf 가 아니다(`:core:common` 하나만 안쪽으로 본다).

Hilt 는 모른다. `Navigator` 인스턴스는 composition root(`:app` 의 `NavigationModule`)가
`@Singleton` + app scope(`Dispatchers.Default`)로 만든다.

---

## Navigator — 백스택의 유일한 소유자

nav3 도 ViewModel 도 스택을 들지 않는다. `stack: StateFlow<List<MyNoteNavKey>>` 가 진짜 값이고,
ViewModel 은 그걸 `triggerIn` 으로 읽어 상태로 나를 뿐이다. 화면이 쓰는 `activeNoteId`·`canPop` 은
저장값이 아니라 **스택에서 파생된** 값이다(`List<MyNoteNavKey>.activeNoteId()` 는 맨 위가 `Reader` 일 때만 답한다).

**스택을 바꾸는 길은 `dispatch(NavCommand)` 하나다.** `push`/`pop` 은 그 얇은 래퍼일 뿐이다.
입구가 하나여야 거기서 쓰로틀을 걸 수 있다.

### 쓰로틀 — 순서가 전부다

```
dispatch: throttle.isIdle() 로 게이트  →  reduce(현재 스택, 명령)  →  apply(새 스택) 안에서 tryAccept()
```

- **스택을 바꾸기 전에 막는다.** 먼저 바꾸고 되돌리면 그사이 `NavDisplay` 가 전환을 시작해
  화면과 스택이 어긋난다. 게이트가 `reduce` 보다 앞에 있는 건 우연이 아니다.
- **버린다, 쌓지 않는다.** 목적이 "연타로 화면을 빠르게 넘기지 못하게" 라서다. 큐에 쌓으면
  누른 만큼 결국 다 이동해 막는 의미가 사라진다.
- **게이트(`isIdle()`)와 소비(`tryAccept()`)를 나눈 이유.** 게이트에서 소비하면 **no-op 명령도 창을 먹는다.**
  `reduce` 가 `null` 을 돌려주는 경우(맨 위와 같은 키를 또 push, 루트에서 pop, `ReplaceAll` 이
  현재와 같거나 빈 목록)는 이동이 없었던 것이니 막을 것도 없다. 여기서 창을 열면 루트에서
  뒤로가기 두 번 = 앱 종료가 성립하지 않는다 — 첫 back 이 창을 열어 두 번째가 `Throttled` 로 죽는다.
- **`Throttled` 와 `Ignored` 를 절대 뭉개지 않는다.** 둘 다 "스택이 안 바뀌었다"지만 전자는 잠깐
  막힌 것이고 후자는 **더 뒤로 갈 데가 없다**는 뜻이라 앱 종료로 이어져야 한다. 합치면 전환 중
  뒤로가기가 종료로 샌다. 호출부는 실제로 `if (navigator.pop() == NavResult.Ignored)` 로만 종료 카운터를 돈다.
- **창 길이는 `SceneMotion` 에서 파생한다** — `max(DURATION_MILLIS, BODY_FADE_OUT + BODY_FADE_IN)`.
  매직넘버로 두면 모션을 늘렸을 때 쓰로틀만 짧게 남는다.

### `dispatchImmediately`

게이트를 우회한다. **사용자 입력이 아닌 이동 전용**(정정·딥링크). 호출부는 동기화로 지워진 노트가 열려 있을 때의
`Pop`, 연결이 바뀔 때 스택을 `[Home]` 으로 되돌리는 `ReplaceAll`, 알림이 여는 설정 `Push` 셋이다. 연타가 아니라
정정이라서, 막으면 지워진 노트의 화면이 남는다.

사용자 입력 하나를 `dispatchImmediately` 와 `dispatch` 로 나눠 부르지 않는다. 앞의 `apply()` 가 창을 열어 뒤의
`dispatch` 가 `Throttled` 로 버려진다. 바뀔 스택은 한 번에 계산해 `dispatch` 하나로 보낸다.

관찰해 둘 어긋남: `dispatchImmediately` 도 `apply()` 를 공유하므로 `tryAccept()` 를 부르지만, 창이
열려 있으면 `tryAccept()` 는 `false` 를 돌려주고 기준 시점을 밀지 않는다. 반면 `settleJob` 은 취소·재시작해
`isSettled` 를 처음부터 다시 잡는다. 그래서 전환 중 정정이 일어난 직후에는 "쓰로틀은 통과하는데
`isSettled` 는 아직 false" 인 짧은 구간이 생긴다.

### 스레드 · 수명

- **메인 스레드 전용이다.** `Throttler` 는 평범한 `var` 를 쓰고 `dispatch` 는 check-then-act 라 동기화가
  없다. 현재 모든 호출부가 `viewModelScope`(Main.immediate) 안이라 성립한다 — 여기서 벗어나게 바꾸면 깨진다.
  (`isSettled` 를 켜고 끄는 `settleJob` 만 주입된 app scope 에서 도는데, 그쪽은 `StateFlow` 라 안전하다.)
- **스택은 메모리에만 있다.** `@Singleton` + app scope 라 구성 변경·ViewModel 소멸은 버티지만
  **프로세스 사망 복원은 없다.** 키에 `@Serializable` 이 붙어 있어도 현재 아무도 직렬화하지 않는다
  (`rememberNavBackStack`/SavedState 미사용). 복원을 붙이려면 그 배선부터 새로 만들어야 한다.
- **스택은 절대 비지 않는다.** 루트 pop 과 `ReplaceAll(empty)` 를 `reduce` 가 막는다. `NavDisplay` 의
  `backStack` 이 비면 안 되므로 이 불변식을 깨지 말 것.

---

## 씬 — `NavDisplay` 는 하나, 갈리는 건 전략뿐

폼팩터에 따라 바뀌는 건 `sceneStrategies` 리스트뿐이다. `ListDetailSceneStrategy` 를 넣으면 2단 고정
패널, 빈 리스트면 nav3 기본 `SinglePaneSceneStrategy` 로 떨어져 씬이 스택처럼 쌓인다.

- **`key` 가 상세에 따라 바뀌면 안 된다.** 전략이 받은 `sceneKey` 를 그대로 쓴다. 키가 같아야
  `NavDisplay` 가 씬을 통째로 슬라이드시키지 않고, 씬이 자기 안의 `AnimatedContent` 로 본문만
  페이드 스루로 바꾼다. 키를 상세에서 만들면 노트를 고를 때마다 화면 전체가 슬라이드한다.
- `entries` 는 맨 위 하나, `previousEntries = entries.dropLast(1)`.
  **`previousEntries` 가 비면 `NavDisplay` 가 뒤로가기를 가져가지 않는다** — nav3 가
  `NavigationBackHandler(isBackEnabled = scene.previousEntries.isNotEmpty())` 로 등록한다. 홈 단독일 때
  back 이 우리 종료 핸들러까지 내려오는 건 이 성질에 기대고 있다.
- **맨 위가 `NotesNavKey.Connect` 이면 `null` 을 돌려 기본 한 칸으로 떨어진다** — 넓은 창에서도 목록 칸·헤더 없이
  전체 폭이다(RC-14). 판정은 `entry.contentKey` 를 같은 기본값으로 만든 `Connect` 엔트리의 것과 비교한다
  (`NavEntry.key` 는 private 이고 기본 `contentKey` 는 문자열이 아니라 (문자열, 클래스) 쌍이다). 엔트리에
  `contentKey` 를 직접 주는 코드가 생기면 이 판정이 조용히 빗나간다. 시험: `RC-14 연결 화면은 …`.
- **슬롯(`panel`/`header`/`panelExpanded`)은 호출부에서 `remember` 로 정체성을 고정**해 넘긴다.
  매 컴포지션 새 람다가 들어오면 전략이 달라진 것으로 보여 씬이 통째로 다시 만들어진다. 안의 값은
  `rememberUpdatedState` 로 읽는다. 특히 `panelExpanded` 는 `remember` 키에서 빼야 한다 —
  패널을 접었다 펼 때마다 전략이 새로 생기면 안 된다.

---

## 모션 · 예측 뒤로가기

공개된 건 세 spec(`sceneTransitionSpec`/`scenePopTransitionSpec`/`scenePredictivePopTransitionSpec`)뿐이고
수치를 담은 `SceneMotion` 은 `internal` 이다 — 수치가 feature 로 새지 않고, 쓰로틀 창이 같은 출처를 본다.

- 예측 뒤로가기는 `NavDisplay` 가 `SeekableTransitionState` 로 이 전환을 직접 seek 한다.
  **별도 `PredictiveBackHandler` 를 달지 않는다.**
- **`predictivePopTransitionSpec` 은 진행률 1 에서 화면 밖으로 완전히 빠져야 한다.** 제스처가
  커밋되면 `NavDisplay` 가 남은 구간을 `animate` → `seekTo` → `snapTo` 로 **같은 spec 그대로** 이어서
  재생한다. 1 에서 덜 빠지는 값이면 마무리 애니메이션이 이전 화면을 남긴 채 끝난다.
  현재 predictive spec 은 pop spec 을 그대로 재사용하고 `swipeEdge` 는 무시한다.
- **`onBack` 은 애니메이션을 커밋한 뒤에 불린다.** nav3 의 `onBackCompleted` 안에서 호출되므로,
  거기서 이동을 버리면 화면은 닫혔는데 스택은 그대로라 어긋난다. 그래서 전환 중 뒤로가기를 삼키는
  핸들러는 `NavDisplay` **호출 뒤**에 놓아야 한다(LIFO — 나중에 컴포즈된 핸들러가 이긴다).
  `activity-compose` 의 `BackHandler` 도 `NavigationEventDispatcher` 가 있으면 그쪽에 등록되므로
  nav3 의 핸들러와 같은 LIFO 를 탄다(activity-compose 1.13.0 소스 확인). 실제 배선은 feature 쪽
  셸에서 `NotesNavDisplay(...)` 바로 다음 줄의 `BackHandler(enabled = !isNavSettled)` 다.

---

## `sceneEdge` — `EnterTransition` 으로 못 그리는 값

모서리 반경·그림자는 `EnterTransition` 으로 표현할 수 없다. `rememberSceneEdgeProgress()` 가 현재
스코프의 트랜지션을 읽어 `() -> Float` 를 주고, `Modifier.sceneEdge(progress)` 가 배치 단계에서
`placeWithLayer` 로 적용한다.

- **`graphicsLayer { }` 확장이 아니라 `ModifierNodeElement` 를 직접 구현한 이유:** 비-composable 확장으로
  두면 람다가 매 컴포지션 새 인스턴스가 되고 `BlockGraphicsLayerElement` 는 람다를 `!==` 로 비교하므로
  skippability 가 깨진다. 여기서는 노드가 `layerBlock` 을 한 번만 들고, `shouldAutoInvalidate = false` +
  `update` 에서 `invalidatePlacement()` 만 한다.
- `rememberSceneEdgeProgress()` 의 `remember { { 1f - entered } }` 는 **키 없는 `remember`** 다.
  람다 정체성이 고정돼야 `SceneEdgeElement`(data class, `progress` 로 equals)가 매 프레임 달라지지 않는다.
  애니메이션 값은 람다 **안에서** 읽어 draw/place 로 미룬다.
- 스코프가 없으면 항상 `0f` 인 상수 람다를 돌려준다 → 프리뷰가 깨지지 않는다.
- **넓은 창에서는 켜지 않는다.** 2단 씬 안에서 `LocalNavAnimatedScope` 는 본문 `AnimatedContent` 의
  스코프라, 진행률이 "씬 진입"이 아니라 "본문 교체"를 가리킨다. 호출부가 스택처럼 쌓이는 Compact 에서만
  이 modifier 를 건다.

---

## 공유 요소 — 프리뷰 안전성이 가시성으로 강제돼 있다

nav3 의 `LocalNavAnimatedContentScope` 는 미설정 시 **예외를 던진다**(소스에 `error("Unexpected access…")`).
화면에서 직접 읽으면 `NavDisplay` 밖(=프리뷰)에서 바로 깨진다.

그래서 nullable 한 `LocalNavAnimatedScope` 를 하나 두는데, **그 CompositionLocal 은 `internal`** 이다.
화면이 손댈 수 있는 건 `ProvideNavAnimatedScope`(엔트리 래퍼)와 `navSharedElement`/`navSharedBounds` 뿐이고,
스코프가 없으면 헬퍼가 받은 `Modifier` 를 그대로 돌려준다. 규율이 아니라 가시성으로 막혀 있으니
이걸 `public` 으로 열지 말 것.

- `ProvideNavAnimatedScope` 는 `LocalNavAnimatedScope.current ?: LocalNavAnimatedContentScope.current` —
  **이미 제공된 안쪽 스코프가 이긴다.** 2단 씬이 자기 `AnimatedContent` 스코프를 제공하므로
  넓은 창에서는 본문 크로스페이드가, Compact 에서는 `NavDisplay` 의 씬 전환이 공유 요소의 기준이 된다.
- 그 fallback 때문에 **`ProvideNavAnimatedScope` 자체는 엔트리 블록 안에서만 부른다.** 프리뷰에서 부르면 던진다.
- `LocalSharedTransitionScope` 는 공개다 — `SharedTransitionLayout` 이 `NavDisplay` 를 감싸고 그 scope 를
  내려 주는 건 feature 의 일이다.
- 키 문자열의 도메인 의미(`chip-`/`date-`/`title-{noteId}`)는 feature 가 정한다. 여기는 배선만.

---

## 테스트가 고정하는 것

`Navigator` 가 `scope`·`minIntervalMillis`·`clock` 을 **전부 생성자로 받는 이유가 테스트다.**
기본 `SystemClockSource` 는 `android.os.SystemClock` 을 쓰고 이 프로젝트는
`unitTests.returnDefaultValues` 를 켜지 않았으므로, 주입 없이는 JVM 단위 테스트에서 돌지 않는다.
`Clock` 이 `fun interface` 라 테스트는 `Navigator(this, home, WINDOW) { currentTime }` 로 가상시간에 붙인다.

- `NavigatorTest` — 창 경계(`WINDOW - 1` 은 막힘), **`Throttled` vs `Ignored`**(전환 중 루트 pop 은
  `Throttled` 여야 한다 / no-op 은 창을 열지 않아야 한다), `dispatchImmediately` 통과,
  창이 마지막 이동 기준으로 재시작.
- `ListDetailSceneStrategyTest` — 상세가 바뀌어도 씬 키 고정, `entries` 1개, `previousEntries` 파생.
  `SceneStrategyScope()` 를 직접 만들어 Compose 없이 돈다.
- `NotesNavKeyTest` — `activeNoteId()` 는 맨 위가 `Reader` 일 때만 답한다.

여기 손대면 **변이 테스트로 확인할 것.** 예: 게이트에서 `tryAccept()` 를 부르게 바꾸면
"no-op 은 창을 열지 않는다" 가 빨간불이 돼야 한다.

---

## 새 목적지를 붙일 때

1. 키를 `MyNoteNavKey` 하위에 추가한다(sealed 이므로 다른 모듈에서 못 만든다).
2. **feature 이름이 박힌 API 를 만들지 않는다.** 현재 예외는 `NotesNavKey` 와 `activeNoteId()` 뿐이고
   그건 계약 자체라 여기 있는 게 맞다. 씬·모션·공유요소 헬퍼는 feature 를 타지 않게 유지한다.
3. Compose 의존은 foundation 까지. material3 를 끌어오지 않는다.
4. 화면 조립·슬롯·테마는 feature 가 한다. 이 모듈은 "어디로 갈 수 있는가"와 "어떻게 전환되는가"만 안다.
