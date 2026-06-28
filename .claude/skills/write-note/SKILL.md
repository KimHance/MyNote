---
name: write-note
description: >
  Save, update, or delete a study note in this repo's GitHub-backed note store. Use this
  whenever the user wants to record or change a note — phrasings like "save this as a note",
  "write/make a note", "turn this into a note", "add it to notes", "update/edit the note",
  "delete the note". Composes the content into our notes/<group>/<id>.json format using the
  closed block set in SCHEMA.md and commits it via local git; it never touches
  index.json/hashes (a local pre-commit hook regenerates those). If there is any intent to
  capture a note, use this skill — do not hand-write the file or edit index.json yourself.
---

# write-note

The MyNote app is **server-driven**: it reads the GitHub repo's `notes/` and renders it. So
"add/update/delete a note" simply means **committing the note JSON to the repo**. This skill
performs that commit in our format, safely.

## Division of labor (why it's built this way)

The expensive LLM (this session) does **only the part that needs intelligence** — turning the
conversation into note blocks and committing a single note file. The **deterministic mechanical
work (regenerating index.json + sha256 hashes + schema validation) is done automatically by a
local pre-commit hook** (`.githooks/pre-commit`, which runs `gen_index.py` at commit time — no
LLM tokens, and it lands in the *same* commit). Therefore:

> **This skill touches only `notes/<group>/<id>.json` files. It never touches
> `notes/index.json`, hashes, or `gen_index.py`.** The hook handles those; touching them here
> causes conflicts, duplicate work, and wasted tokens.

## Invariants (always hold)

1. **Always `git pull --rebase` before any operation** (create/update/delete) to sync with
   remote in case the note changed elsewhere.
2. **Stage and commit only the note file** — e.g. `git add notes/<group>/<id>.json`. The
   pre-commit hook adds the regenerated `index.json` to the commit for you; never stage it
   yourself, and don't mix in unrelated changes.
3. **Commit as the user, with no AI co-author footer.** Use `git` only — no `gh` CLI. **Never
   use `git commit --no-verify`** — it skips the pre-commit hook that builds the index.
4. Use **only the closed block set** from `notes/SCHEMA.md` (below). Unknown types get silently
   dropped by the app, and the pre-commit hook's validation will abort the commit.
5. **The operation is NOT done until `git push` succeeds — this is the whole point.** The app is
   server-driven: it reads the GitHub **remote**, never your local clone. A local commit is
   completely invisible to the app, so committing without pushing is a silent no-op from the
   user's view. After pushing, **verify it landed** (`git status` reports the branch up to date
   with `origin/main`, or the push output shows the ref advanced). Only then tell the user the
   note is saved. If the push fails, say so plainly — never report success on an unpushed commit.

## Note format

`notes/SCHEMA.md` is the single source of truth — **read it before writing** and follow the
current spec. Summary:

- File: `notes/<group>/<id>.json`, content `{ "id", "title", "date", "blocks": [...] }`
- `group` = folder name (existing `android`/`kotlin`/`cs`, or a new folder). `id` = kebab-case,
  globally unique. `title` = note title. `date` = today as `YYYY.MM.DD`.
- Block types (8): `heading` (level 2|3, spans) · `paragraph` (spans) · `code` (code) ·
  `quote` (spans) · `bulletList` (items) · `orderedList` (items) · `divider` · `spacer`
- Inline span: `{ "text", "style" }`, style = `text` (default) | `bold` | `code`
- Reading an existing note (`notes/<group>/*.json`) is the clearest way to see the shape.

## Authoring style — compose for readability

The block set is small, so a note reads well or badly entirely by **how consistently you use
it**. Past notes drifted (random bolding, fragmented spans, dividers everywhere), which is why
this section exists. The benchmark for "good" is **`notes/kotlin/generics.json`** — read it
before composing anything substantial and mirror its rhythm.

### Emphasis — three styles, three jobs (this is where notes go wrong most)

- **`code` span** — anything you would type *as code*: identifiers, keywords, types, function/API
  names, literals, file paths, header names (`val`, `List<out E>`, `ClassCastException`,
  `Cache-Control`, `notes/index.json`). This is the default for every technical token; reach for
  it freely.
- **`bold`** — the **one term a sentence is defining**, or the single idea to remember. Use it
  sparingly: roughly one, at most two, per paragraph. If half the paragraph is bold, nothing
  stands out. Don't bold whole sentences — the one exception is a deliberate summary line.
- **plain `text`** — all the connective prose. Most spans are plain.
- Never stack `bold` + `code` on the same token. If a token is both an identifier and the key
  concept, prefer `code`.

### Span hygiene (kills the "멋대로 줄바꿈" feel)

A sentence is one *sequence* of spans; switch style only where the emphasis actually changes.

- Keep **spaces in the plain spans**, not glued onto an emphasized token. Good:
  `"제네릭은 "` + `{bold "타입 안전"}` + `" 과 "`. Bad: `{bold " 타입 안전 "}` (leading/trailing
  space inside the emphasis looks broken when rendered).
- Don't fragment a plain run into many spans — one continuous plain stretch is one span.
- Let blocks carry the vertical rhythm. Paragraphs already render with spacing, so **don't sprinkle
  `spacer`** to fake line breaks; reach for `spacer` only for a rare deliberate breath.

### Section rhythm

- **One idea per section.** `heading` level 2 = a major section; level 3 = a subsection under it.
- The reliable rhythm is **heading → a short concept paragraph (the *why*) → a `code` example →
  a `bulletList` of takeaways.** Not every section needs all four — lead with the idea, show code
  when it clarifies, distill to bullets when there are discrete points.
- **`divider` only between major (H2) sections.** It's a strong break; one between every block
  turns the note into confetti.
- Close a substantial note with a **`quote` (bold) one-line summary** — the single takeaway
  (see the end of `generics.json`).

### Code blocks

- Write **real, compilable-looking** snippets, not pseudocode. Use real `\n` newlines.
- **Comment the teaching point inline** (`// 위험: …`, `// OK`, `// 컴파일 에러`).
- Show **contrast** (wrong-vs-right) when a pitfall is the lesson.
- One block = one point. Split unrelated ideas into separate blocks under their own sections.

### Scale depth to the actual content

- **Quick capture** (a single concept): heading + a paragraph or a short list, maybe a closing
  quote. Don't pad it into fake sections (`sealed.json` / `http-cache.json` are fine being small).
- **Deep dive** (a topic with parts): numbered H2 sections (`1. 왜 …`, `2. 기본 문법`), a
  `divider` between them, a code block per section, a summary `quote` at the end (`generics.json`).
- Match the note to what was actually discussed — never inflate a small idea, never cram a big
  topic into one wall of text.

## Workflow

Infer the operation from the request (save/add → CREATE, edit/change → UPDATE, remove/delete →
DELETE). **Step 1 is always `git pull --rebase`.**

### CREATE — new note
1. `git pull --rebase`
2. Read `notes/SCHEMA.md` for the shape and **`notes/kotlin/generics.json` for the style** (the
   readability benchmark).
3. Compose the topic into blocks **following the Authoring style above** — match its depth to what
   was discussed, use emphasis with intent, keep the section rhythm. Check the `id` isn't already
   used under `notes/`.
4. **Preview**: show the title, group, and a block summary, and get the user's confirmation.
5. On confirm, write `notes/<group>/<id>.json` → `git add notes/<group>/<id>.json`
   → `git commit -m "notes: add <title>"` → **`git push` → verify it landed** (see invariant 5)

### UPDATE — edit an existing note
1. `git pull --rebase`
2. Read the target note file (already current). Apply the requested edits to the blocks.
3. **Preview** → confirm.
4. `git add` the same file → `git commit -m "notes: update <title>"` → **`git push` → verify** (invariant 5)

### DELETE — remove a note
1. `git pull --rebase`
2. Confirm the target, then `git rm notes/<group>/<id>.json`
3. **Preview** (what's being removed) → confirm.
4. `git commit -m "notes: remove <title>"` → **`git push` → verify** (invariant 5)

### Afterward (automatic)
When you commit, the **pre-commit hook** runs `gen_index.py`, regenerating `index.json` (+hashes)
and including it in the *same* commit. If a note violates SCHEMA, the hook aborts the commit
(fix and retry). The app fetches the index on startup/refresh and reconciles. **Do not touch the
index in this skill.**

> One-time setup per machine: `git config core.hooksPath .githooks` (enables the hook).

## Conflicts / failures

- If the push is rejected (someone pushed in between), `git pull --rebase` and retry.
- If blocks violate SCHEMA, the pre-commit hook **aborts the commit** (loud, local, immediate) —
  fix the note and commit again. For personal use, concurrent-edit conflicts are effectively
  nonexistent.

## Examples

### Flow example

**Input (user):** "save what we just discussed about coroutine dispatchers as a note"
**Action:** `git pull --rebase` → read SCHEMA + `generics.json` → group `kotlin`,
`id=coroutine-dispatchers`, today's date, compose with the section rhythm → preview & confirm →
commit `notes/kotlin/coroutine-dispatchers.json` → **push and verify it landed**. The
pre-commit hook regenerates the index in the same commit.

### Style example A — quick capture (single idea, don't pad)

```jsonc
{
  "id": "coroutine-dispatchers",
  "title": "코루틴 디스패처 고르기",
  "date": "2026.06.29",
  "blocks": [
    { "type": "heading", "level": 2, "spans": [ { "text": "어떤 디스패처를 언제" } ] },
    { "type": "paragraph", "spans": [
      { "text": "디스패처는 코루틴이 " },
      { "text": "어느 스레드에서 도는지", "style": "bold" },
      { "text": " 를 정한다. 작업 성격에 맞춰 고른다." }
    ] },
    { "type": "bulletList", "items": [
      { "spans": [ { "text": "Dispatchers.Default", "style": "code" }, { "text": " — CPU 바운드(정렬·파싱)." } ] },
      { "spans": [ { "text": "Dispatchers.IO", "style": "code" }, { "text": " — 블로킹 I/O(네트워크·디스크)." } ] },
      { "spans": [ { "text": "Dispatchers.Main", "style": "code" }, { "text": " — UI 갱신." } ] }
    ] },
    { "type": "quote", "spans": [
      { "text": "고민되면 suspend 함수 안에서 withContext 로 필요한 구간만 바꾼다.", "style": "bold" }
    ] }
  ]
}
```

### Style example B — deep dive (the section rhythm, one section shown)

A numbered H2, a *why* paragraph (emphasis used with intent), a commented contrast snippet, a
bullet takeaway — then a `divider` before the next section, and a summary `quote` to close the
note. Repeat the section block per topic; see `generics.json` for the full-length shape.

```jsonc
{ "type": "heading", "level": 2, "spans": [ { "text": "1. 왜 제네릭?" } ] },
{ "type": "paragraph", "spans": [
  { "text": "제네릭은 " },
  { "text": "타입 안전", "style": "bold" },
  { "text": " 과 재사용을 동시에 얻는 장치다. 없으면 " },
  { "text": "Any", "style": "code" },
  { "text": " 로 담고 꺼낼 때마다 캐스팅해야 한다." }
] },
{ "type": "code", "code": "val box: MutableList<Any> = mutableListOf()\nval n = box[0] as Int   // 컴파일 OK, 런타임 ClassCastException\n\nval strings: MutableList<String> = mutableListOf()\nval s: String = strings[0]  // 캐스팅 불필요" },
{ "type": "bulletList", "items": [
  { "spans": [ { "text": "컴파일러가 타입을 끝까지 추적 → 런타임 캐스팅 제거." } ] }
] },
{ "type": "divider" },
{ "type": "quote", "spans": [
  { "text": "요약: 기본은 무공변 → 생산만 out, 소비만 in (PECS) → 런타임 타입은 inline + reified.", "style": "bold" }
] }
```
