---
id: common
name: Common
aliases: [공통, MVI 베이스, 쓰로틀, core common]
uses: []
units:
  - {id: mvi, name: MVI base, aliases: [MviViewModel, 전체 재조립, 상태 조립, trigger], code: [src/main/java/com/hancekim/mynote/core/common/mvi/, src/test/java/com/hancekim/mynote/core/common/mvi/]}
  - {id: throttle, name: Throttler and clocks, aliases: [쓰로틀, 연타 막기, Clock, WallClock], code: [src/main/java/com/hancekim/mynote/core/common/throttle/, src/test/java/com/hancekim/mynote/core/common/throttle/]}
  - {id: result, name: Cancellation-safe Result, aliases: [runCatchingCancellable, 취소 안 삼키는 runCatching], code: [src/main/java/com/hancekim/mynote/core/common/result/, src/test/java/com/hancekim/mynote/core/common/result/]}
---
## Overview
Common holds three thin pieces that the outermost layers share: the base class every screen ViewModel extends, a throttler with injectable clocks, and a `Result` wrapper that does not swallow coroutine cancellation.
It is a leaf: no other project module and no Compose. It is an Android library, not pure JVM, only because the default monotonic clock reads the Android system clock. Anything screen-related and shared goes to the design system, not here.
## Structure
- Every feature ViewModel and the app shell's ViewModel extend `mvi`; its failure boundary uses `result`.
- `throttle` is used by the navigator (`navigation:navigator`), by the design system's throttled click helper, by the model download worker for progress updates, and, through the wall clock, by ViewModels that stamp stored times.
- `result` is called by sync, quiz generation and the device-note and connection repositories, and by `mvi` itself.
- Feature modules get this module from the feature convention; the other users declare it themselves.
```mermaid
flowchart LR
  mvi --> result
  throttle ~~~ result
```
