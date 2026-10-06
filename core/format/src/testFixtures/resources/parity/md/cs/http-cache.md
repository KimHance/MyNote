---
title: HTTP 캐싱 기초
date: 2026.04.22
---
## 캐시 제어 헤더

- `Cache-Control: max-age` 로 신선도를 정한다.
- `ETag` 와 조건부 요청.

> 304 Not Modified 는 본문 없이 재검증만 성공한 응답이다.
