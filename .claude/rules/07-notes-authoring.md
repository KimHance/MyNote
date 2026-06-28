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
- 훅 설치(머신당 1회): `git config core.hooksPath .githooks`.

## 형식
- `{ id, title, date, blocks[] }`. `group`=폴더명, `id`=kebab 유일, `date`=오늘 `YYYY.MM.DD`.
- 블록은 `notes/SCHEMA.md` 의 닫힌 집합만(heading/paragraph/code/quote/bulletList/orderedList/divider/spacer,
  span style text/bold/code). 작성 전 `SCHEMA.md` 를 읽는다.
