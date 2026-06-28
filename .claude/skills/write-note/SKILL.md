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

## Workflow

Infer the operation from the request (save/add → CREATE, edit/change → UPDATE, remove/delete →
DELETE). **Step 1 is always `git pull --rebase`.**

### CREATE — new note
1. `git pull --rebase`
2. Read `notes/SCHEMA.md` and one or two existing notes to confirm the shape.
3. Compose the conversation/topic into blocks. Check the `id` isn't already used under `notes/`.
4. **Preview**: show the title, group, and a block summary, and get the user's confirmation.
5. On confirm, write `notes/<group>/<id>.json` → `git add notes/<group>/<id>.json`
   → `git commit -m "notes: add <title>"` → `git push`

### UPDATE — edit an existing note
1. `git pull --rebase`
2. Read the target note file (already current). Apply the requested edits to the blocks.
3. **Preview** → confirm.
4. `git add` the same file → `git commit -m "notes: update <title>"` → `git push`

### DELETE — remove a note
1. `git pull --rebase`
2. Confirm the target, then `git rm notes/<group>/<id>.json`
3. **Preview** (what's being removed) → confirm.
4. `git commit -m "notes: remove <title>"` → `git push`

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

## Example

**Input (user):** "save what we just discussed about coroutine dispatchers as a note"
**Action:** `git pull --rebase` → read SCHEMA → group `kotlin`, `id=coroutine-dispatchers`,
today's date, compose heading/paragraph/bulletList → preview & confirm →
commit & push `notes/kotlin/coroutine-dispatchers.json`. The Action regenerates the index.
