import shutil
import unittest

import support


class RepoIndexTest(support.TempCase):
    maxDiff = None

    def test_NF_13_레포_노트로_만든_목록이_커밋된_index_json_과_바이트가_같다(self):
        """NF-13 레포 노트로 만든 목록이 커밋된 index.json 과 바이트가 같다"""
        committed = (support.REPO_ROOT / "notes" / "index.json").read_bytes()
        copy = self.tmp / "repo-copy"
        shutil.copytree(
            support.REPO_ROOT / "notes",
            copy / "notes",
            ignore=shutil.ignore_patterns("index.json", "__pycache__"),
        )
        self.assertFalse((copy / "notes" / "index.json").exists())

        result = self.generate(copy)

        self.assertEqual(result.returncode, 0, result.stderr)
        produced = (copy / "notes" / "index.json").read_bytes()
        self.assertEqual(
            produced.decode("utf-8"),
            committed.decode("utf-8"),
            "notes/index.json 이 notes/ 와 다르다 — 훅 없이 커밋했으면 python3 notes/gen_index.py 로 다시 만든다",
        )


if __name__ == "__main__":
    unittest.main()
