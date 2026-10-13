# 자리표시자

공개 폴더 안의 `<…>` 꼴 대문자 자리표시자(꺾쇠 안이 대문자로 시작하는 대문자 · 숫자 · 밑줄 두 글자 이상)는 모두 이 표에 있습니다. 규격이 문법 예로 보이는 `<T>` 같은 한 글자는 자리표시자가 아닙니다. 표에 없는 자리표시자는 쓰지 않습니다(시험이 같은지 확인합니다).
공개하는 사람이 실제 값으로 바꿉니다. 실제 값을 이 레포에 되돌려 적지 않습니다.

| 자리표시자 | 뜻 | 쓰이는 곳 | 정하는 사람 |
|---|---|---|---|
| `<OWNER>` | 공개 레포를 만드는 GitHub 계정 또는 조직 이름 | 공개 안내 문서 · `notes-kit/.claude-plugin/marketplace.json` · `notes-kit/plugins/mynote-notes/.claude-plugin/plugin.json` | 공개하는 사람 |
| `<KIT_REPO>` | 도구 레포의 `소유자/저장소` 좌표(예: `<OWNER>/` 뒤에 레포 이름) | 도구 README(템플릿은 도구를 사본으로 들고 있어 쓰지 않는다) | 공개하는 사람 |
| `<TEMPLATE_REPO>` | 템플릿 레포의 `소유자/저장소` 좌표 | 공개 안내 문서 | 공개하는 사람 |
| `<LICENSE>` | 라이선스 이름과 본문 | 도구 README · `notes-kit/.claude-plugin/marketplace.json`(`license` 칸) | 공개하는 사람 |

값을 바꾸는 법은 [`README.md`](README.md) 의 "공개하는 사람이 할 일" 을 봅니다.
