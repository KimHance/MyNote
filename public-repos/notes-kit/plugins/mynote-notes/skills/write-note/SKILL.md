---
name: write-note
description: 노트 저장소(노트 앱이 읽는 GitHub 저장소)에 노트를 저장 · 수정 · 삭제한다. "노트로 저장해줘", "노트 만들어줘", "이 내용 노트로 남겨줘", "노트 고쳐줘", "노트 지워줘" 같은 요청에 쓴다. 대화 내용을 규격의 닫힌 블록 집합으로 구성해 `notes/<그룹>/<id>.json` 한 파일만 커밋하고 push 한다. `index.json` 은 만들지 않는다 — 저장소의 Action 이 만든다.
---

# write-note

노트 앱은 **GitHub 저장소의 `notes/` 를 읽어서** 그립니다. 그래서 노트를 저장·수정·삭제한다는 것은 노트 JSON 파일을 저장소에 커밋하고 push 하는 일입니다. 이 스킬은 그 일을 규격에 맞게, 안전하게 합니다.

## 역할 나눔

- 이 스킬은 **대화 내용을 노트 블록으로 구성하는 일**과 **노트 파일 하나를 커밋하는 일**만 합니다.
- `notes/index.json` 과 해시는 **만들지도 고치지도 않습니다.** 저장소의 Action 이 push 를 보고 대신 만듭니다.

## 규격

규격의 정본은 이 플러그인이 든 레포의 `SPEC.md` 입니다. **노트를 쓰기 전에 읽고 그대로 따릅니다.** 이 스킬에는 규격을 옮겨 적지 않았습니다. 옮겨 적으면 규격이 바뀔 때 어긋나기 때문입니다.

- 쓰는 곳은 `notes/<그룹>/<id>.json` 입니다. 그룹은 폴더 이름이고, `id` 는 저장소 전체에서 유일해야 합니다.
- 블록 종류 · 인라인 스타일 · 헤딩 레벨은 규격의 닫힌 집합만 씁니다. 집합 밖의 값은 만들지 않습니다.
- 이미 있는 노트를 하나 읽어 보면 모양과 문체를 가장 빨리 알 수 있습니다.

## 지킬 것

1. **작업 전에 `git pull --rebase` 를 합니다.** 저장 · 수정 · 삭제 모두 같습니다.
2. **`notes/<그룹>/<id>.json` 파일만 스테이징하고 커밋합니다.** 다른 변경을 섞지 않습니다. `index.json` 은 스테이징하지 않습니다.
3. 커밋은 사용자의 git 신원으로 합니다. `git` 만 씁니다. 커밋 훅을 건너뛰는 옵션은 권하지도 쓰지도 않습니다.
4. **미리보기를 보여 주고 확인을 받은 뒤에 씁니다.** 제목 · 그룹 · `id` · 블록 요약을 보여 줍니다.
5. **push 가 성공해야 끝입니다.** 앱은 로컬 클론이 아니라 GitHub 를 읽으므로 push 되지 않은 커밋은 앱에서 보이지 않습니다. push 뒤에 올라갔는지(`git status` 가 브랜치가 앞서 있지 않다고 말하는지) 확인하고 나서야 저장됐다고 말합니다. push 가 안 됐으면 안 됐다고 그대로 말합니다.
6. **저장소가 템플릿에서 왔는지 봅니다.** `.github/workflows/` 에 노트 index 를 만드는 workflow 가 없으면 "index 를 만들어 줄 Action 이 없어서 앱에 새 노트가 안 보일 수 있습니다" 라고 알립니다. 이때도 `index.json` 을 직접 만들지는 않고, 검사 도구의 `index` 명령을 안내만 합니다.

## 작업별 순서

### 저장

1. `git pull --rebase`
2. `SPEC.md` 를 읽고 `id` 가 이미 쓰이지 않는지 `notes/` 에서 확인합니다.
3. 대화 내용을 블록으로 구성합니다. 작은 생각은 작게, 큰 주제는 절 단위로 나눕니다.
4. 미리보기를 보이고 확인을 받습니다.
5. `notes/<그룹>/<id>.json` 을 씁니다. `date` 는 오늘 날짜(`YYYY.MM.DD`)입니다.
6. 쓴 파일이 규격에 맞는지 `check-notes` 스킬로(또는 `python3 "${CLAUDE_PLUGIN_ROOT}/scripts/note_tool.py" check`) 확인합니다.
7. `git add notes/<그룹>/<id>.json` → `git commit -m "notes: add <제목>"` → `git push` → 올라갔는지 확인

### 수정

1. `git pull --rebase`
2. 대상 노트를 읽고 요청한 부분만 고칩니다.
3. 미리보기 → 확인 → 같은 파일만 `git add` → `git commit -m "notes: update <제목>"` → `git push` → 확인

### 삭제

1. `git pull --rebase`
2. 대상을 확인하고 지우기 전에 무엇이 사라지는지 보여 준 뒤 확인을 받습니다.
3. `git rm notes/<그룹>/<id>.json` → `git commit -m "notes: remove <제목>"` → `git push` → 확인

## 실패했을 때

- push 가 거절되면 `git pull --rebase` 를 하고 다시 push 합니다.
- 규격 검사가 실패하면 파일 경로와 이유를 그대로 전하고, 노트를 고친 뒤 다시 확인합니다.
- 저장소의 Action 이 push 뒤에 `index.json` 을 커밋하므로, 다음 작업 전에 `git pull --rebase` 를 하면 그 커밋이 들어옵니다.

## 예시

**요청:** "방금 얘기한 코루틴 디스패처 내용을 노트로 저장해줘"

**흐름:** `git pull --rebase` → `SPEC.md` 확인 → 그룹 `kotlin`, `id` `coroutine-dispatchers` 로 구성 → 미리보기 · 확인 → 아래 파일 하나를 커밋 → push 하고 올라갔는지 확인

```json
{
  "id": "coroutine-dispatchers",
  "title": "코루틴 디스패처 고르기",
  "date": "2026.01.01",
  "blocks": [
    { "type": "heading", "level": 2, "spans": [{ "text": "어떤 디스패처를 언제" }] },
    { "type": "paragraph", "spans": [{ "text": "디스패처는 코루틴이 " }, { "text": "어느 스레드에서 도는지", "style": "bold" }, { "text": " 를 정합니다." }] },
    { "type": "bulletList", "items": [
      { "spans": [{ "text": "Dispatchers.Default", "style": "code" }, { "text": " — CPU 작업" }] },
      { "spans": [{ "text": "Dispatchers.IO", "style": "code" }, { "text": " — 블로킹 I/O" }] }
    ] },
    { "type": "quote", "spans": [{ "text": "작업 성격에 맞춰 고릅니다.", "style": "bold" }] }
  ]
}
```
