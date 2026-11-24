# Decisions

### SC-D5 · 2026-10 · ui:token, SC-K3
- decision: The token guide words (the fine-grained token name and the "contents read-only" permission name) stay as they are in the app text.
- why: They pair with the names on GitHub's own screen, so rewording them would leave the user unable to find them on GitHub; the template README test also looks for those words.
- rejected: Rewording them into plain Korean like the rest of the screen text.
- evidence: `notes-tests/test_screen_copy.py` (frozen list read from `docs/design/screen-copy.md`), `public-repos/tests/test_template.py`, `core/ui` strings `ui_token_howto_*`.
