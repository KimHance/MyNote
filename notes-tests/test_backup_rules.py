import unittest
import xml.etree.ElementTree as ET

import support

XML = support.REPO_ROOT / "app" / "src" / "main" / "res" / "xml"
FULL_BACKUP = XML / "backup_rules.xml"
EXTRACTION = XML / "data_extraction_rules.xml"
# 앱 데이터에서 빠져야 하는 것 — 토큰 두 파일과 퀴즈 모델(받는 중 조각 포함)
KEPT_OUT = {
    ("file", "datastore/github_token.preferences_pb"),
    ("file", "datastore/connection_attempt_token.preferences_pb"),
    ("file", "gemma-4-E2B-it.litertlm"),
    ("file", "gemma-4-E2B-it.litertlm.part"),
    ("file", "gemma-4-E2B-it.litertlm.plan"),
}


def rule_sets():
    """(이름, 규칙 묶음 요소) — 전체 백업 하나와 데이터 추출의 클라우드 백업 · 기기 이전."""
    full = ET.parse(FULL_BACKUP).getroot()
    extraction = ET.parse(EXTRACTION).getroot()
    sets = [("backup_rules", full)]
    for tag in ("cloud-backup", "device-transfer"):
        found = extraction.find(tag)
        assert found is not None, f"data_extraction_rules 에 {tag} 가 없음"
        sets.append((f"data_extraction_rules/{tag}", found))
    return sets


def excludes(rules):
    return {(e.get("domain"), e.get("path")) for e in rules.findall("exclude")}


class BackupRulesTest(unittest.TestCase):
    def test_LN_15_백업_규칙은_노트_DB_를_빼지_않는다(self):
        for name, rules in rule_sets():
            with self.subTest(name):
                # include 가 하나라도 있으면 적은 것만 백업된다 — 노트 DB 가 빠지는 길이다
                self.assertEqual([], rules.findall("include"), name)
                database = [path for domain, path in excludes(rules) if domain in ("database", "root")]
                self.assertEqual([], database, name)
                self.assertNotIn(("file", "."), excludes(rules), name)

    def test_LN_15_토큰과_모델_제외는_그대로다(self):
        for name, rules in rule_sets():
            with self.subTest(name):
                self.assertTrue(KEPT_OUT <= excludes(rules), name)
                self.assertIn(("external", "."), excludes(rules), name)


if __name__ == "__main__":
    unittest.main()
