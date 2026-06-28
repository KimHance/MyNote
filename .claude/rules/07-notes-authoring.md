# 노트 작성 (서버드리븐 콘텐츠)

레포의 `notes/` 는 앱이 GitHub 에서 읽어가는 **서버드리븐 콘텐츠**다. 노트 추가/수정/삭제는
`write-note` 스킬로 한다(직접 손으로 파일 만들지 말 것).

## 불변식
- **노트 파일(`notes/<group>/<id>.json`)만 건드린다.** `notes/index.json`·hash·`gen_index.py` 는
  절대 손대지 않는다 — 그건 **pre-commit 훅(`.githooks/pre-commit`)** 이 커밋 순간 `gen_index.py` 로
  재생성해 같은 커밋에 포함한다(역할 분담: 작문=LLM, 결정적 변환=훅, 토큰 0).
- **작업(생성/수정/삭제) 전 항상 `git pull --rebase`** 로 최신화.
- 커밋: 사용자 신원, AI 공동저자 푸터 없음, `gh` 금지(`git` 만), **노트 파일만 stage**(index 는 훅이 추가).
  **`git commit --no-verify` 금지** — 훅을 건너뛰어 index 가 안 만들어진다.
- **`git push` 까지 해야 "저장 완료"다.** 앱은 서버드리븐 — GitHub **원격**을 읽지 로컬 클론을 안 읽는다.
  커밋만 하고 push 안 하면 앱엔 아무 변화도 안 보인다(조용한 no-op). push 후 `git status` 로 `origin/main`
  동기화 확인까지. push 실패 시 성공이라 보고하지 말 것.
- 훅 설치(머신당 1회): `git config core.hooksPath .githooks`.

## 형식
- `{ id, title, date, blocks[] }`. `group`=폴더명, `id`=kebab 유일, `date`=오늘 `YYYY.MM.DD`.
- 블록은 `notes/SCHEMA.md` 의 닫힌 집합만(heading/paragraph/code/quote/bulletList/orderedList/divider/spacer,
  span style text/bold/code). 작성 전 `SCHEMA.md` 를 읽는다.

## 조립 스타일 (가독성)
- 강조 규칙: **식별자/키워드/타입/API/리터럴 = `code` span**, **정의하는 핵심어 1~2개만 `bold`**, 나머지는 일반.
  bold 남발 금지, bold+code 한 토큰에 중첩 금지. 공백은 일반 span 에 두고 강조 span 안에 넣지 않는다.
- 섹션 리듬: `heading → 개념 문단(why) → code → bulletList 요약`. `divider` 는 H2 대섹션 사이에만. 긴 노트는
  마지막에 `quote`(bold) 한 줄 요약. `spacer` 로 줄바꿈 흉내 내지 말 것(블록이 간격을 만든다).
- 깊이는 내용에 맞춘다 — 한 개념이면 짧게(억지 섹션 금지), 큰 주제면 번호 H2 + divider + 요약 quote.
- **가독성 기준점은 `notes/kotlin/generics.json`.** 실질 노트 작성 전 이 파일을 본다. 상세 규칙은 `write-note` 스킬.
