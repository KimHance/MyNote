# public-repos

이 폴더는 **공개할 레포들의 원본**입니다. 폴더 하나가 레포 하나이고, 각 폴더는 이 레포 없이 혼자 서 있습니다(상위 경로를 참조하지 않고, 폴더 안 시험이 폴더만으로 돕니다).
GitHub 에 레포를 만들거나 올리는 일은 이 폴더가 하지 않습니다. 아래 순서대로 사람이 합니다.

## 무엇이 어느 레포가 되나

| 폴더 | 공개 레포 | 하는 일 |
|---|---|---|
| `notes-kit/` | `<KIT_REPO>` | 노트 형식 규격(정본) · 재사용 Action · 검사 · 생성 도구 · 스킬 플러그인 마켓플레이스 |
| `notes-template/` | `<TEMPLATE_REPO>` | "Use this template" 로 노트 저장소를 만드는 템플릿 — 예시 노트 · 미리 만든 `notes/index.json` · 재사용 Action 을 부르는 workflow · 시작 안내 README |
| `tests/` | (공개하지 않음) | 폴더 밖을 보거나 비공개 이름을 들고 있어야 하는 시험 — 이 레포 전용 |

`<OWNER>` · `<KIT_REPO>` · `<TEMPLATE_REPO>` · `<LICENSE>` 는 자리표시자입니다. 전부 [`PLACEHOLDERS.md`](PLACEHOLDERS.md) 표에 있습니다.

## 시험

```bash
python3 -I -m unittest discover public-repos/tests && python3 -I -m unittest discover public-repos/notes-kit/tests
```

- `notes-kit/tests/` 는 그 폴더만으로 도는 시험입니다. 폴더를 떼어 내도 같은 명령(`python3 -I -m unittest discover tests`)이 통과합니다.
- `tests/` 는 이 레포 `gen_index.py` 의 JSON 닫힌 집합 · 출력이 공개 도구와 같은지 보고, 공개 폴더에 비공개 정보(소유자 이름 · 좌표 · 로컬 경로 · 토큰 · 이메일)가 새지 않았는지 훑습니다.
- 시험은 기기의 전역 git 설정을 무시하는 임시 저장소에서 돕니다.

## 공개하는 사람이 할 일

순서대로 합니다. 되돌리기 어려운 일이므로 공개 범위와 이름은 사람이 정합니다.

1. 레포 이름과 `<OWNER>` 를 정하고 [`PLACEHOLDERS.md`](PLACEHOLDERS.md) 의 값을 모두 정합니다. 라이선스도 이때 고릅니다.
2. GitHub 에서 도구 레포(`<KIT_REPO>`)를 새로 만들고 `notes-kit/` 폴더의 **내용**을 레포 루트로 올립니다.
3. 올린 파일에서 자리표시자를 실제 값으로 바꿉니다. 바꾼 뒤에도 `python3 -I -m unittest discover tests` 가 통과하는지 레포 안에서 확인합니다.
4. 도구 레포에 태그 `v1.0.0` 을 달고, 움직이는 주 버전 태그 `v1` 이 같은 커밋을 가리키게 합니다. 템플릿의 workflow 는 `<KIT_REPO>@v1` 로 Action 을 부릅니다.
5. 템플릿 레포(`<TEMPLATE_REPO>`)를 새로 만들어 `notes-template/` 폴더의 내용을 올리고, 설정의 **Template repository** 를 켭니다.
6. 스킬 플러그인을 쓰게 하려면 `notes-kit/` 의 마켓플레이스를 등록합니다(`.claude-plugin/marketplace.json` 이 가리키는 플러그인).
7. 처음부터 끝까지 한 번 확인합니다: 템플릿에서 저장소 생성 → 앱에 주소 붙이기 → 웹에서 노트 추가 → Actions 가 `index.json` 을 커밋 → 앱에서 새로고침.

## 지키는 규칙

- 이 폴더 안 파일에는 이 레포의 소유자 이름 · 이름 좌표 · 로컬 절대 경로 · 토큰 모양 문자열 · 이메일을 적지 않습니다.
- 폴더 밖을 가리키는 상대 경로를 쓰지 않습니다.
- `<…>` 꼴 대문자 자리표시자는 `PLACEHOLDERS.md` 표에 먼저 적습니다.
