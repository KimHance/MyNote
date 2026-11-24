# Decisions

### D-45 · 2026-10 · ST-9
- decision: Aggregation is a pure Kotlin function in the domain module; SQL only filters by time range and picks each note's latest session.
- why: Day, week and month buckets follow the device time zone and week start, which would hide in SQL date functions; a year of rows is a few thousand, cheap in memory; a pure function is tested without a device.
- rejected: Bucketing and summing in SQL.
- evidence: `core/domain/.../stats/StudyStatsCalculator.kt`, `StudyStatsCalculatorTest`.

### D-46 · 2026-10 · ST-10
- decision: Comparison periods end at the same moment of the previous period (last Monday to now minus seven days; last month's first day to the same day and time, clamped to month end).
- why: Comparing a whole previous Thursday with a Thursday morning would always say "less".
- rejected: Comparing against the whole previous week or month.
- evidence: `StatsPeriodRange.kt`, `StatsPeriodRangeTest`.

### D-47 · 2026-10 · ST-11
- decision: The category axis is the current note categories joined with categories seen in events in the period, ordered by name; colours follow that order (five in the palette, outlines from the sixth).
- why: Categories are folder names and there is no fixed list in code.
- rejected: A fixed category list as in the design handoff.
- evidence: `StudyStatsCalculatorTest` "카테고리 축은 현재 노트와 기간 이벤트의 합집합을 이름순으로".
