import hashlib
import json
import unittest

import support

NOTE_KEYS = {"id", "title", "date", "path", "hash"}


def copies(ext):
    """고정 사본 parity/<ext>/<그룹>/<id>.<ext> — notes 기준 경로 → 바이트."""
    root = support.PARITY / ext
    return {p.relative_to(root).as_posix(): p.read_bytes() for p in sorted(root.glob(f"*/*.{ext}"))}


def without(index, *keys):
    """그룹 순서와 그룹 안 순서를 지킨 채 항목에서 keys 를 뺀다."""
    return [(group, [{k: v for k, v in e.items() if k not in keys} for e in entries]) for group, entries in index.items()]


class MigrationTest(support.TempCase):
    def index_of(self, name, notes):
        root = self.notes_tree(name, notes)

        result = self.generate(root)

        self.assertEqual(result.returncode, 0, result.stderr)
        return json.loads((root / "notes" / "index.json").read_text(encoding="utf-8"))

    def test_NF_16_사본_두_벌의_목록은_경로와_해시만_다르다(self):
        """NF-16 사본 두 벌의 목록은 경로와 해시만 다르다"""
        json_copies = copies("json")
        md_copies = copies("md")
        self.assertEqual(len(json_copies), 22)
        self.assertEqual(
            sorted(rel.removesuffix(".json") for rel in json_copies),
            sorted(rel.removesuffix(".md") for rel in md_copies),
        )

        before = self.index_of("json", json_copies)
        after = self.index_of("md", md_copies)

        self.assertEqual(without(after, "path", "hash"), without(before, "path", "hash"))
        pairs = [pair for group in before for pair in zip(before[group], after[group])]
        self.assertEqual(len(pairs), 22)
        for old, new in pairs:
            with self.subTest(old["path"]):
                self.assertEqual(set(old), NOTE_KEYS)
                self.assertEqual(set(new), NOTE_KEYS)
                self.assertEqual(new["path"], old["path"].removesuffix(".json") + ".md")
                rel = new["path"].removeprefix("notes/")
                self.assertEqual(new["hash"], hashlib.sha256(md_copies[rel]).hexdigest())
                self.assertNotEqual(new["hash"], old["hash"])


if __name__ == "__main__":
    unittest.main()
