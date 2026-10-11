# notes-kit

노트 저장소를 위한 공개 도구 모음입니다. 노트는 `notes/<그룹>/<id>.md` 의 앞머리(`title` · `date`) + 마크다운 본문이고, 옛 JSON 노트(`.json`, 형식 1)도 읽습니다. 지금 형식은 `2` 입니다.

| 무엇 | 어디 |
|---|---|
| 노트 형식 규격(정본) | [`SPEC.md`](SPEC.md) |
| 재사용 Action | `action.yml` |
| 검사 · index 생성 도구 | `tools/note_tool.py` (파이썬 표준 라이브러리만) |
| 도구 시험 | `tests/` |

## Action 으로 쓰기

저장소에 workflow 한 개를 두면 노트를 올릴 때마다 GitHub 가 노트를 검사하고 `notes/index.json` 을 대신 만들어 같은 브랜치에 올립니다. 검사에서 걸리면 `index.json` 은 그대로이고 작업이 실패해 어느 파일이 왜 틀렸는지 알려 줍니다. 마크다운 노트는 앞머리만 검사하고 본문은 보지 않습니다.

```yaml
name: notes
on:
  push:
    branches: ['**']
    paths:
      - 'notes/**'
      - '!notes/index.json'
concurrency:
  group: notes-${{ github.ref }}
  cancel-in-progress: false
permissions:
  contents: write
jobs:
  index:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: <KIT_REPO>@v1
```

| 입력 | 기본값 | 뜻 |
|---|---|---|
| `notes-dir` | `notes` | 저장소 루트 기준 노트 폴더 |
| `commit` | `true` | `false` 면 `index.json` 만 쓰고 커밋하지 않습니다 |

체크아웃은 사용자 workflow 가 먼저 합니다. 브랜치가 아닌 ref(태그 등)에서 불리면 커밋 · push 없이 끝납니다.

## 로컬에서 쓰기

```bash
python3 tools/note_tool.py check
python3 tools/note_tool.py index
```

## 시험

```bash
python3 -I -m unittest discover tests
```

## 라이선스

<LICENSE>
