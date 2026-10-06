---
name: write-note
description: >
  Save, update, or delete a study note in this repo's GitHub-backed note store. Use this
  whenever the user wants to record or change a note — phrasings like "save this as a note",
  "write/make a note", "turn this into a note", "add it to notes", "update/edit the note",
  "delete the note". Writes the note as notes/<group>/<id>.md — a title/date front matter plus
  a Markdown body, following notes/SCHEMA.md — and commits it via local git; it never touches
  index.json/hashes (a local pre-commit hook regenerates those). If there is any intent to
  capture a note, use this skill — do not hand-write the file or edit index.json yourself.
---

# write-note

The MyNote app is **server-driven**: it reads the GitHub repo's `notes/` and renders it. So
"add/update/delete a note" simply means **committing the note's Markdown file to the repo**. This
skill performs that commit in our format, safely.

## Division of labor (why it's built this way)

The expensive LLM (this session) does **only the part that needs intelligence** — turning the
conversation into a Markdown note and committing that single file. The **deterministic mechanical
work (regenerating index.json + sha256 hashes + front matter validation) is done automatically by
a local pre-commit hook** (`.githooks/pre-commit`, which runs `gen_index.py` at commit time — no
LLM tokens, and it lands in the *same* commit). Therefore:

> **This skill touches only `notes/<group>/<id>.md` files. It never touches
> `notes/index.json`, hashes, or `gen_index.py`.** The hook handles those; touching them here
> causes conflicts, duplicate work, and wasted tokens.

## Invariants (always hold)

1. **Always `git pull --rebase` before any operation** (create/update/delete) to sync with
   remote in case the note changed elsewhere.
2. **Stage and commit only the note file** — e.g. `git add notes/<group>/<id>.md`. The
   pre-commit hook adds the regenerated `index.json` to the commit for you; never stage it
   yourself, and don't mix in unrelated changes.
3. **Commit as the user, with no AI co-author footer.** Use `git` only. **Never bypass the
   pre-commit hook** — it builds the index and validates the front matter.
4. **Follow `notes/SCHEMA.md`**: the front matter, the write rules (below), and only syntax the
   app actually draws. Links, images, HTML, math, highlights, and alerts show up as plain text in
   the app — don't use them.
5. **The operation is NOT done until `git push` succeeds — this is the whole point.** The app is
   server-driven: it reads the GitHub **remote**, never your local clone. A local commit is
   completely invisible to the app, so committing without pushing is a silent no-op from the
   user's view. After pushing, **verify it landed** (`git status` reports the branch up to date
   with `origin/main`, or the push output shows the ref advanced). Only then tell the user the
   note is saved. If the push fails, say so plainly — never report success on an unpushed commit.

## Note format

`notes/SCHEMA.md` is the single source of truth — **read it before writing**. Summary:

- File: `notes/<group>/<id>.md`. `group` = folder name (an existing one or a new folder).
  `id` = the file name without `.md`, kebab-case, globally unique — check that no note under
  `notes/` already uses it, including older `.json` notes.
- Front matter — exactly two keys, nothing else inside the fences:

  ```markdown
  ---
  title: 노트 제목
  date: 2026.06.29
  ---
  ```

  `date` is today as `YYYY.MM.DD`. The title lives here only — don't repeat it as a `#` line.
- Body: `##` / `###` headings, paragraphs, fenced code, `>` quotes, `-` and `1.` lists (nested
  items, `- [ ]` / `- [x]` task items, a custom start number), GFM tables, `---` dividers, and
  inline `**bold**`, `*italic*`, `~~strike~~`, `` `code` ``.
- Reading an existing `.md` note under `notes/` is the clearest way to see the shape.

## Write rules (from SCHEMA.md — the app and GitHub must render the same text)

1. **Headings only with `#`.** No underline headings (`===` / `---` under a line).
2. **Never put `---` on the line right after a paragraph.** Leave a blank line first, or the
   paragraph turns into a heading.
3. **Bold/italic that ends in punctuation must not touch the next letter.** `**"인용"**이다` does
   not render bold — move the punctuation out (`"**인용**"이다`) or add a space
   (`**코드(code)** 를`).
4. **Escape literal `<` `~` `*` `_` `$` `==`, and `|` inside tables, with a backslash** —
   `Box\<T>`, `\~10`, `\$5`, `a \| b`. Never escape inside inline code or code blocks (the
   backslash would show).

## Authoring style — compose for readability

The syntax is small, so a note reads well or badly entirely by **how consistently you use it**.
The benchmark for "good" is the `generics` note in `notes/kotlin/` — read it before composing
anything substantial and mirror its rhythm.

### Emphasis — three styles, three jobs (this is where notes go wrong most)

- **Inline code** — anything you would type *as code*: identifiers, keywords, types, function/API
  names, literals, file paths, header names (`val`, `List<out E>`, `ClassCastException`,
  `Cache-Control`). This is the default for every technical token; reach for it freely.
- **Bold** — the **one term a sentence is defining**, or the single idea to remember. Use it
  sparingly: roughly one, at most two, per paragraph. Don't bold whole sentences — the one
  exception is a deliberate summary line.
- **Plain text** — all the connective prose. Most text is plain.
- Use *italic* and ~~strike~~ only when they carry meaning (a term being contrasted, a claim being
  retracted). Never stack bold on inline code.

### Emphasis hygiene

- Keep spaces **outside** the markers: `**타입 안전** 과`, not `** 타입 안전 **과`.
- Let blocks carry the vertical rhythm. A blank line already separates blocks — don't stack extra
  blank lines or `<br>` to fake spacing.

### Section rhythm

- **One idea per section.** `##` = a major section; `###` = a subsection under it.
- The reliable rhythm is **heading → a short concept paragraph (the *why*) → a code example →
  a bullet list of takeaways.** Not every section needs all four.
- **`---` only between major (`##`) sections**, with blank lines around it.
- Close a substantial note with a **bold one-line summary quote** (`> **요약: …**`).

### Code blocks

- Write **real, compilable-looking** snippets, not pseudocode, in a fenced block.
- **Comment the teaching point inline** (`// 위험: …`, `// OK`, `// 컴파일 에러`).
- Show **contrast** (wrong-vs-right) when a pitfall is the lesson.
- One block = one point. Split unrelated ideas into separate blocks under their own sections.

### Scale depth to the actual content

- **Quick capture** (a single concept): a heading + a paragraph or a short list, maybe a closing
  quote. Don't pad it into fake sections.
- **Deep dive** (a topic with parts): numbered `##` sections (`1. 왜 …`, `2. 기본 문법`), a `---`
  between them, a code block per section, a summary quote at the end.
- Match the note to what was actually discussed — never inflate a small idea, never cram a big
  topic into one wall of text.

## Workflow

Infer the operation from the request (save/add → CREATE, edit/change → UPDATE, remove/delete →
DELETE). **Step 1 is always `git pull --rebase`.**

### CREATE — new note
1. `git pull --rebase`
2. Read `notes/SCHEMA.md` for the format and the `generics` note for the style.
3. Compose the topic **following the write rules and authoring style above**. Check the `id`
   isn't already used under `notes/`.
4. **Preview**: show the title, group, and an outline, and get the user's confirmation.
5. On confirm, write `notes/<group>/<id>.md` → `git add notes/<group>/<id>.md`
   → `git commit -m "notes: add <title>"` → **`git push` → verify it landed** (invariant 5)

### UPDATE — edit an existing note
1. `git pull --rebase`
2. Read the target note file (already current) and apply the requested edits.
   If it is an older `.json` note, rewrite it as `notes/<group>/<id>.md` with the same `id`,
   then `git rm` the `.json` file — both changes go in the same commit (the hook stops on a
   duplicate `id` otherwise).
3. **Preview** → confirm.
4. `git add` the note → `git commit -m "notes: update <title>"` → **`git push` → verify** (invariant 5)

### DELETE — remove a note
1. `git pull --rebase`
2. Confirm the target, then `git rm` its file under `notes/<group>/`.
3. **Preview** (what's being removed) → confirm.
4. `git commit -m "notes: remove <title>"` → **`git push` → verify** (invariant 5)

### Afterward (automatic)
When you commit, the **pre-commit hook** runs `gen_index.py`, regenerating `index.json` (+hashes)
and including it in the *same* commit. If the front matter is wrong, the hook aborts the commit and
names the file and the reason (fix and retry). The app fetches the index on startup/refresh and
reconciles. **Do not touch the index in this skill.**

> One-time setup per machine: `git config core.hooksPath .githooks` (enables the hook).

## Conflicts / failures

- If the push is rejected (someone pushed in between), `git pull --rebase` and retry.
- If the front matter is invalid, the pre-commit hook **aborts the commit** (loud, local,
  immediate) — fix the note and commit again.

## Examples

### Flow example

**Input (user):** "save what we just discussed about coroutine dispatchers as a note"
**Action:** `git pull --rebase` → read SCHEMA + the `generics` note → group `kotlin`,
`id=coroutine-dispatchers`, today's date, compose with the section rhythm → preview & confirm →
commit `notes/kotlin/coroutine-dispatchers.md` → **push and verify it landed**. The pre-commit
hook regenerates the index in the same commit.

### Style example A — quick capture (single idea, don't pad)

```markdown
---
title: 코루틴 디스패처 고르기
date: 2026.06.29
---
## 어떤 디스패처를 언제

디스패처는 코루틴이 **어느 스레드에서 도는지** 를 정한다. 작업 성격에 맞춰 고른다.

- `Dispatchers.Default` — CPU 바운드(정렬 · 파싱).
- `Dispatchers.IO` — 블로킹 I/O(네트워크 · 디스크).
- `Dispatchers.Main` — UI 갱신.

> **고민되면 suspend 함수 안에서 withContext 로 필요한 구간만 바꾼다.**
```

### Style example B — deep dive (the section rhythm, one section shown)

A numbered `##`, a *why* paragraph (emphasis used with intent), a commented contrast snippet, a
bullet takeaway — then a `---` before the next section, and a summary quote to close the note.

````markdown
## 1. 왜 제네릭?

제네릭은 **타입 안전** 과 재사용을 동시에 얻는 장치다. 없으면 `Any` 로 담고 꺼낼 때마다 캐스팅해야 한다.

```kotlin
val box: MutableList<Any> = mutableListOf()
val n = box[0] as Int   // 컴파일 OK, 런타임 ClassCastException

val strings: MutableList<String> = mutableListOf()
val s: String = strings[0]  // 캐스팅 불필요
```

- 컴파일러가 타입을 끝까지 추적 → 런타임 캐스팅 제거.

---

> **요약: 기본은 무공변 → 생산만 out, 소비만 in (PECS) → 런타임 타입은 inline + reified.**
````
