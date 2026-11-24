---
id: stats
name: Statistics
aliases: [통계, 학습 통계, 지난 요약, stats]
uses: [common, design-system, domain, navigation, ui]
units:
  - {id: stats, name: Stats state and figures, aliases: [통계 집계, 기간, 구역, 숫자 띠, 변화량, StatsViewModel], code: [src/]}
  - {id: screen, name: Stats screen, aliases: [통계 화면, 이번 주 · 이번 달 · 전체, 읽기 · 퀴즈 · 북마크, 읽다 멈춘 노트, 다시 푼 퀴즈, 오래 안 본 북마크, 틀린 문제 패턴], code: [src/main/java/com/hancekim/mynote/feature/stats/screen/, src/test/java/com/hancekim/mynote/feature/stats/screen/]}
  - {id: summary, name: Weekly summary card, aliases: [요약 카드, 이번 주 요약, 주간 요약, 정리됨, 초점], code: [src/main/java/com/hancekim/mynote/feature/stats/SummaryCardRules.kt, src/main/java/com/hancekim/mynote/feature/stats/GeneratedAgo.kt, src/main/java/com/hancekim/mynote/feature/stats/screen/SummaryCard.kt, src/test/java/com/hancekim/mynote/feature/stats/SummaryCardRulesTest.kt, src/test/java/com/hancekim/mynote/feature/stats/GeneratedAgoTest.kt]}
  - {id: history, name: Past summaries, aliases: [지난 요약, 주별 요약 목록, past summaries], code: [src/main/java/com/hancekim/mynote/feature/stats/screen/StatsHistoryScreen.kt]}
---
## Overview
Owns the Stats tab and the past-summaries screen: the period and section the person chose, the four headline numbers with their change, the reading, quiz and bookmark charts and lists, the weekly summary card and the wrong-answer pattern written by the on-device model, and the week-by-week history of summaries.
It shows figures; it does not compute them (period ranges, totals, lists and the history rows are `domain`), does not record study events (`notes`, `quiz`, the data modules), does not write summaries (the worker in `ai`), and does not draw the top bar, the floating navigation or the two-column decision (`app` shell).
## Structure
- `stats` is `StatsViewModel` (Activity owner) with its state, intents and the mapper from the domain figures to screen models; it attaches the aggregation, insights and history streams only while their screen is on top of the back stack.
- `screen` registers the `Stats` and `StatsHistory` destinations and draws the Stats tab in one or two columns from the `stats` state; list taps go back to `stats` as intents.
- `summary` decides whether the summary card and the pattern show and how old they read (pure rules used by `stats`) and draws the card inside the top item of `screen`.
- `history` draws the past-summaries list that the summary card's meta button opens.
```mermaid
flowchart LR
  domain[domain use cases] --> stats
  stats -- rules --> summary
  stats -- state --> screen
  screen --> summary
  screen --> history
  screen -- intents --> stats
  stats -- open reader, quiz request --> nav[navigation and ui signals]
```
