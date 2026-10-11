# 화면 문구 — 디자인

| 항목 | 값 |
|---|---|
| 기능 | screen-copy |
| 기능 문서 | docs/spec/screen-copy.md |
| 등급 | 문구만 |
| 바꾼 문구 표 | docs/design/screen-copy-rows.md (이 문서가 20 KB 를 넘지 않게 나눴다) |
| 마지막 변경 | 2026-10-11 · 처음 — 용어 표 · 끝말 · 쓰지 않는 표현 · 한 줄 고정 자리 · 같이 바뀔 것 · 바꾼 문구 56행 |

## 문구

용어표 원본이다. 대상은 `strings.xml` 넷(`:feature:notes` · `:core:design-system` · `:app` · `:core:ai`)의 문구 395개다
(배열 항목은 하나씩 센다). 전수로 읽고 바꿀 행만 바꾼 문구 표에 올렸다 — 표에 없는 339개는 값이 그대로다.

화면 글은 이 절의 말만 쓴다. 새 문구를 더할 때도 아래 넷(용어 표 · 끝말 · 끝말 예외 · 쓰지 않는 표현)을 먼저 본다.
넷은 시험(`notes-tests/test_screen_copy.py` — copy-apply 가 만든다)이 이 문서에서 직접 읽는다.

### 시험이 읽는 꼴

| 규칙 | 값 |
|---|---|
| 어느 표 | 아래 `### 용어 표` · `### 끝말` · `### 끝말 예외` · `### 쓰지 않는 표현` 제목 바로 밑의 첫 표. 제목 글자를 바꾸면 시험이 깨진다(의도) |
| 읽는 행 | `꼴` 칸이 `글자` 또는 `정규식` 인 행만. 그 밖(`—` 등)은 사람이 읽는 행이다 |
| 글자 | 값에 그 글자가 들어 있으면 걸린다 |
| 정규식 | 파이썬 `re.search` 로 건다. 칸 안에 세로줄을 쓰지 않는다 — 고를 말이 여럿이면 행을 나눈다 |
| 표현 · 이름 칸 | 백틱 하나에 하나. 이름이 여럿이면 ` · ` 로 잇는다. 배열 항목은 `이름[번호]` |
| 값 다듬기 | XML 값에서 태그(`<b>` 등)를 걷고, 두 글자 `\n` 을 줄바꿈으로 본다. 형식 자리(`%1$s` · `%2$,d` · `%%`)는 그대로 둔다 |

### 용어 표

같은 개념은 같은 말로 부른다. `버린 말` 이 값에 있으면 SC-3 이 걸린다 — `동결 예외` 칸에 이름을 적은 문구만 빼고.
노트를 저장소에서 데려오는 일은 `불러오기`, 퀴즈 모델 파일을 기기로 가져오는 일은 `받기` 로 갈린다.

| 개념 | 쓸 말 | 버린 말 | 꼴 | 동결 예외 | 왜 |
|---|---|---|---|---|---|
| 노트를 저장소에서 데려옴 | 불러오기 · 불러오는 중 · 불러왔습니다 · 불러오지 못했습니다 | `가져오` | 글자 | — | 한 일에 다섯 말(불러오기 · 받기 · 가져오기 · 동기화 · 반영)이 섞였다. 가장 많이 쓰던 말로 |
| 〃 | 〃 | `동기화` | 글자 | — | 내부 용어. 저장소 카드 줄도 불러오기로 |
| 〃 | 〃 | `반영` | 글자 | — | 명사 나열 |
| 〃 | 〃 | `노트를? 받` | 정규식 | — | `받` 은 모델 말이다 |
| 노트를 다시 불러오라고 누르는 버튼 | 새로고침 | — | — | — | 버튼 이름. 누르면 불러온다 |
| 퀴즈 모델 파일을 기기로 가져옴 | 받기 · 받는 중 · 이어 받기 · 받던 파일 · 받았습니다 · 받지 못했습니다 | `내려받` | 글자 | `ai_model_download_channel` | model-download 디자인의 `받` 하나. 스낵바 둘도 이번에 맞춘다. 알림 채널 이름은 사용자 알림 설정에 남아 동결 |
| 받은 모델을 돌림 | 실행 | `모델을 불러오` | 글자 | — | `불러오기` 는 노트 말이다 |
| 없앰 | 지우기 · 지웁니다 · 지웠습니다 · 지울까요? | `삭제` | 글자 | `notes_model_delete` · `notes_model_delete_title` · `notes_model_deleted` | 지우기가 열네 곳, 삭제가 네 곳이었다. 모델 카드 `삭제` 버튼은 `지우기` 로 바꾸면 한 글자 늘어 SC-6(버튼 글자 수)에 걸린다. 버튼과 같은 일을 말하는 대화상자 제목 · 스낵바도 같이 둔다 |
| 북마크에서 뺌 | 해제 (추가 ↔ 해제) | `뺐` | 글자 | — | 버튼 · 통계는 이미 해제 |
| 퀴즈 한 개 | 문제 | `문항` | 글자 | — | 문제가 열여덟 곳, 문항이 네 곳이었다 |
| 화면에 나타냄 | 보이다 · 보여 줍니다 | `표시` | 글자 | — | 보이다 · 보여 주다가 네 곳. 표시했습니다는 지난 일처럼 읽힌다 |
| 퀴즈를 만듦 | 만들기 · 만듦 | `생성` | 글자 | — | 버튼 · 제목이 만들기 |
| 이 자리 | 여기 | `이곳` | 글자 | — | 빈 상태 문구 셋이 여기 |
| 오래 열지 않은 북마크 | 안 본 | `묵은` | 글자 | — | 구역 제목이 안 본 |
| 노트를 담은 GitHub 저장소 | 저장소 | `레포` | 글자 | — | repo-connect 디자인 |
| 앱 이름 | MyNote | `공부 기록` | 글자 | — | brand 디자인 |
| 토큰 안내 낱말 | 세분화된 토큰 · 이 저장소 하나만 · 콘텐츠 읽기 전용 | — | — | `notes_token_howto_kind` · `notes_token_howto_scope` · `notes_token_howto_perm` | 동결(기능 문서 결정 5). GitHub 화면 이름과 짝이고 템플릿 README 시험이 본다 |
| 화면 제목 | MyNote · 설정 · 북마크 · 통계 · 지난 요약 | — | — | `notes_app_title` · `app_name` · `notes_settings` · `notes_nav_bookmark` · `notes_nav_stats` · `notes_stats_history_title` | 동결(BR-7) |
| 저장소를 앱에 묶음 | 연결 · 연결 끊기 · 저장소 바꾸기 | — | — | — | 그대로 |

### 끝말

말투는 합니다체 하나다. 권유는 `~하세요` 하나로 모으고 `~해 주세요` 는 쓰지 않는다. `~나요?` 도 쓰지 않는다(작업 요청서 정하지 않은 것 18 의 기본값).

문장 자르기: 다듬은 값을 줄바꿈에서, 그리고 `.` · `?` 뒤 공백에서 자른다. 조각마다 끝의 한글 아닌 글자(형식 자리 · 숫자 · 영문 · 기호 · 마침표)를 걷어 내되 `?` 는 남긴다. 남은 글이 비면 건너뛴다.

판정 차례: `허용` 꼴 중 하나에 맞으면 통과. 아니면 `막음` 꼴 중 하나에 맞으면 실패. 둘 다 아니면 통과한다 — 라벨 · 명사구 · 명사형(`받는 중` · `멈춤` · `북마크됨` · `다시 시도`)은 문장 끝말이 없어서 막음 꼴에 안 걸린다.

| # | 판정 | 꼴 | 끝 | 종류 | 예 |
|---|---|---|---|---|---|
| 1 | 허용 | 정규식 | `니다$` | 서술 | 받았습니다 · 남습니다 |
| 2 | 허용 | 정규식 | `(?<!주)세요$` | 권유 | 확인하세요 · 읽어 보세요 |
| 3 | 허용 | 정규식 | `까요\?$` | 확인 질문 | 저장소를 바꿀까요? |
| 4 | 허용 | 정규식 | `필요$` | 낱말 필요로 끝나는 라벨 | 토큰 확인 필요 |
| 5 | 막음 | 정규식 | `주세요$` | 권유 둘째 꼴 | 다시 시도해 주세요 |
| 6 | 막음 | 정규식 | `요\??$` | 해요체 · `~나요?` | 열려요 · 없나요? |
| 7 | 막음 | 정규식 | `(?<!니)다$` | 해라체 | 계산한다 |
| 8 | 막음 | 정규식 | `[어아야지자네래걸줘군]$` | 반말 | 다 맞혔어 · 해 보자 |
| 9 | 막음 | 정규식 | `[까니냐]\?$` | 반말 질문 | 지울까? |

`가` · `게` 는 막음에 넣지 않는다 — `추가 %1$d` · `접근 불가` · `본문 글자 크게` · `14일 넘게` 같은 라벨이 그 글자로 끝난다.

### 끝말 예외

SC-1 이 보지 않는 문구다. 이름으로만 뺀다.

| 이름 | 왜 |
|---|---|
| `notes_settings_preview_text` | 노트 본문 견본이라 노트의 해라체를 그대로 보인다 |
| `notes_app_title` · `app_name` · `notes_model_name` · `notes_settings_token_placeholder` | 고유명사 · 토큰 머리 견본 |
| `notes_bookmark_time` · `notes_bookmark_date_this_year` · `notes_bookmark_date_other_year` · `notes_bookmark_date_other_year_short` · `notes_stats_date` · `notes_stats_bucket_week` · `notes_stats_bucket_month` | 날짜 · 시각 형식 문구 |
| `notes_bookmark_row_sub` · `notes_stats_row_sub` · `notes_stats_row_sub3` · `notes_stats_cell_desc` · `notes_stats_bars_desc` · `notes_stats_summary_desc` · `notes_stats_history_row_desc` · `notes_stats_range` · `notes_stats_retry_value` · `notes_model_bytes` · `notes_model_percent` | 자리만 있는 형식 문구 |

### 쓰지 않는 표현

값 전체에 건다(문장으로 자르지 않는다). DESIGN 9.4 목록에 이번에 번역투 · 피동 · 말버릇 · 표기를 더했다.
범위 약속(11)은 양쪽이 숫자일 때만 걸린다 — 형식 자리 사이의 기간 줄표(`지난주 월–%1$s 대비` · `지난달 1–%1$d일 대비` · `%1$s – %2$s` · `%1$s – %2$d일`)는 세지 않는다.

| # | 꼴 | 표현 | 무엇 | 출처 |
|---|---|---|---|---|
| 1 | 글자 | `!` | 느낌표 | DESIGN 9.4 |
| 2 | 정규식 | `[\U0001F000-\U0001FAFF️]` | 이모지 | DESIGN 9.4 |
| 3 | 글자 | `클릭` | 터치 기기다 — 누르다 | DESIGN 9.4 |
| 4 | 글자 | `JSON` | 내부 용어 | DESIGN 9.4 |
| 5 | 글자 | `파싱` | 내부 용어 | DESIGN 9.4 |
| 6 | 글자 | `WorkManager` | 내부 용어 | DESIGN 9.4 |
| 7 | 글자 | `해시` | 내부 용어 | DESIGN 9.4 |
| 8 | 글자 | `오류가 발생` | 막연한 오류 문장 | DESIGN 9.4 |
| 9 | 글자 | `알 수 없는 오류` | 막연한 오류 문장 | DESIGN 9.4 |
| 10 | 글자 | `곧` | 지킬 수 없는 시점 약속 | DESIGN 9.4 · model-download 디자인 |
| 11 | 정규식 | `[0-9]+ *[~～–-] *[0-9]+ *[문개분초일회]` | 범위 약속(3~5문제) | DESIGN 9.4 |
| 12 | 글자 | `에 도달` | 번역투 | 이번 |
| 13 | 정규식 | `[을를] 가집니다` | 번역투(넓은 권한을 가집니다) | 이번 |
| 14 | 글자 | `되어 있지 않` | 피동 | 이번 |
| 15 | 글자 | `완료됩니다` | 한자어 꼬리 · 피동 | 이번 |
| 16 | 글자 | `거부됐` | 피동 — 누가 거부했는지 말한다 | 이번 |
| 17 | 정규식 | `지워[지집]` | 피동 — 앱이 지운다 | 이번 |
| 18 | 정규식 | `비워[지집]` | 피동 | 이번 |
| 19 | 정규식 | `만들어[지집]` | 피동 | 이번 |
| 20 | 글자 | `것 같` | 추측 말버릇 | 이번 |
| 21 | 글자 | `버전이 모르` | 앱을 사람처럼 | 이번 |
| 22 | 글자 | `—` | 줄표(긴 줄표) | 이번 |
| 23 | 글자 | `추이` | 한자어 꼬리 | 이번 |
| 24 | 글자 | `응시` | 한자어 꼬리 | 이번 |
| 25 | 정규식 | `[A-Za-z0-9☰)] [은는이가을를에로와과의도](?![가-힣])` | 영문 · 숫자 · 기호 뒤 조사 띄움(GitHub 에서 → GitHub에서) | 이번 |
| 26 | 정규식 | `[A-Za-z0-9☰)] 에서` | 같음 | 이번 |
| 27 | 정규식 | `[A-Za-z0-9☰)] 으로` | 같음 | 이번 |

시험이 못 보는 것은 리뷰가 바꾼 문구 표로 본다 — 고유명사 밖의 영어 섞기, 명사 나열, 지어낸 진행 단계, 비유.

### 한 줄 고정 자리

`maxLines = 1` 로 그리는 자리다. 문구가 길어지면 조용히 말줄임 · 잘림이 난다. 이 자리 문구는 길이를 늘리지 않는다(SC-6) —
바꾼 문구 표의 `길이` 칸이 이 자리 행에서 0 이하다.

| 자리 | 파일 | 리소스 | 이번에 바꾼 행 |
|---|---|---|---|
| 상단 바 제목 | `shell/NotesTopBar.kt` | `notes_app_title` · `notes_settings` · `notes_nav_bookmark` · `notes_nav_stats` · `notes_stats_history_title` · `notes_connect_change_title` · `notes_connect_attach_title` | 없음 |
| 플로팅 내비 라벨(12sp) | ds `FloatingNav.kt` | `notes_nav_home` · `notes_nav_bookmark` · `notes_nav_stats` | 없음 |
| 상태 알약 | ds `FloatingStatusPill.kt` | `notes_pill_*` 다섯 · `notes_model_percent` | 없음 |
| 퀴즈 CTA 부제 | `quiz/component/QuizCtaCard.kt` | `notes_quiz_cta_subtitle` · `notes_quiz_cta_need_model` · `notes_quiz_cta_paused` · `notes_quiz_cta_downloading` · `notes_pill_failed` | 없음 |
| 퀴즈 설정 부제 | `quiz/component/QuizSetupDialog.kt` | `notes_quiz_setup_subtitle` | `notes_quiz_setup_subtitle` +0 |
| 모델 카드 상태 · 세부 | `screen/ModelCard.kt` | `notes_model_missing` · `waiting` · `preparing` · `downloading` · `retrying` · `verifying` · `paused` · `ready` · `failed` 와 그 `_desc` · `notes_model_failed_space` · `notes_model_failed_generic` | `notes_model_failed_generic` −2 |
| 저장소 카드 상태 라벨 | `screen/RepoSection.kt` | `notes_repo_state_none` · `check_token` · `idle` · `token` · `public` | `notes_repo_state_idle` −3 |
| 버튼(설정 · 연결 · 모델 · 대화상자) | `screen/SettingsScreen.kt` 버튼 라벨 · 퀴즈 설정 버튼 · 연결 띠 | `notes_*` 버튼 문구 스물아홉 · `ds_action_save` · `ds_action_cancel` · `notes_quiz_create` · `notes_home_connect_action` | 없음 — `notes_model_delete` 를 그대로 둔 까닭(용어 표) |
| 통계 머리 기간 · 숫자 칸 · 구역 칩 | `screen/stats/StatsHeader.kt` | `notes_stats_period_*` · `notes_stats_num_*` · `notes_stats_unit_*` · `notes_stats_first_week` · `notes_stats_questions` · `notes_stats_section_*` | `notes_stats_questions` +0 |
| 통계 차트 제목 · 단위 · 범례 초점 | `screen/stats/StatsSections.kt` | `notes_stats_time_by_*` · `notes_stats_accuracy_by_category` · `notes_stats_bookmark_trend` · `notes_stats_pattern` · `notes_stats_minutes_value` · `notes_stats_generated_*` · `notes_stats_focus` · `notes_stats_retry_value` · `notes_stats_bookmark_age` | 없음 |
| 정답률 막대 행 부제 | ds `HBarRow.kt` | `notes_stats_questions` · `notes_stats_not_taken` | `notes_stats_not_taken` +0 |
| 번호 행 부제 | ds `NumberedRow.kt` | `notes_stats_row_sub` · `row_sub3` · `stalled_at` · `retry_count` · `notes_stats_bookmark_last_opened` · `unread` · `unread_since_log` | 없음 |
| 요약 카드 머리 · 정리 시각 | `screen/stats/SummaryCard.kt` | `notes_stats_summary_*` · `notes_stats_range*` · `notes_stats_generated_*` | 없음 |
| 북마크 행 · 큰 카드 시각 | `screen/bookmarks/` | `notes_bookmark_row_sub` · `notes_bookmark_added_*` · 날짜 형식 | 없음 |
| 목록 패널 설정 줄 · 배지 | `screen/ListPanelScreen.kt` | `notes_settings` · `notes_model_badge_*` · `notes_settings_token_check_badge` · `notes_model_percent` | 없음 |
| 원문 보기 버튼 | ds `UnsupportedBlock.kt` | `ds_unsupported_show_raw` · `hide_raw` · `more_lines` | 없음 |
| 주소 · 토큰 칸 힌트 | `screen/connect/ConnectParts.kt` | `notes_connect_link_hint` · `notes_settings_token_placeholder` | 없음 |

한 줄 자리는 아니지만 줄 수를 늘리면 안 되는 곳:

| 자리 | 왜 | 이번에 바꾼 행 |
|---|---|---|
| 첫 실행 연결 화면(가로 800 × 336 · 두 단) | BR-8 — 스크롤 없이 한 화면 | `notes_entry_title` −1 · `notes_entry_step_1` −1 · `notes_connect_token_desc` −3 · `notes_connect_syncing_desc` −4 · `notes_connect_err_offline` −5. 늘어나는 행은 `notes_token_err_rejected` +8 하나 — 토큰을 넣은 뒤의 오류 상자라 BR-8 이 보는 첫 화면에는 없다. SC-9 캡처로 본다 |
| 접힌 알림 | 접히면 한 줄 | `ai_model_download_no_space` −3 · `ai_model_download_failed_body` −2 |

### 같이 바뀔 것

copy-apply 가 `strings.xml` 과 한 묶음으로 고친다. 인용 글자가 `strings.xml` 값과 다르면 SC-8 이 걸린다.

| # | 갈래 | 파일 · 자리 | 무엇 | 누가 |
|---|---|---|---|---|
| ① | 템플릿 README | `public-repos/notes-template/README.md` 앱 용어 일곱 · 문제 해결의 연결 실패 문장 | 바뀌는 값 없음 — 일곱(`notes_entry_create` · `notes_connect_link_label` · `notes_connect_action` · `notes_connect_token_title` · `notes_token_howto_*` 셋)과 `notes_connect_err_no_list` 는 그대로. 확인만 | copy-apply |
| ② | DESIGN.md | 9.2 `ds_unsupported_*` 다섯 · 9.3 권유 끝말 · 9.4 옮긴 문구 표의 ☰ 줄 · 확인 다이얼로그 예 · 실패 예 · 쓰지 않는 표현 머리 | 이 문서와 같은 커밋에서 고쳤다 | 디자인(끝남) |
| ③ | 공개 규격 | `public-repos/notes-kit/SPEC.md` 와 플러그인 사본 | 인용 낱말(`원문 보기` · `노트 목록` · `노트 저장소`)이 그대로라 고칠 것 없음. 확인만 | copy-apply |
| ④ | 기능 문서 | `docs/spec/sync.md` SY-6 `노트 변경 반영 — …` → `노트를 불러왔습니다 · …` | 인용 글자만 · 바뀐 기록 `인용 문구만` | copy-spec |
| ④ | 기능 문서 | `docs/spec/model-download.md` 열린 질문 1 `내려받기를 취소했습니다` → `받기를 취소했습니다` | 같음 | copy-spec |
| ④ | 기능 문서 | `docs/spec/repo-connect.md` 결정 30 "저장된 토큰이 지워집니다" 한 줄 → `저장된 토큰도 지웁니다` | 같음. RC-15 · RC-17 은 인용이 아니라 풀어 쓴 말이라 그대로 | copy-spec |
| ⑤ | 디자인 문서 | `docs/design/repo-connect.md` 입구 제목 · 단계 ① | 이 문서와 같은 커밋에서 고쳤다 | 디자인(끝남) |
| ⑤ | 디자인 문서 | `docs/design/model-download.md` 용어 줄(스낵바도 `받`) · 알림 표 받지 못함 본문 | 이 문서와 같은 커밋에서 고쳤다 | 디자인(끝남) |
| ⑤ | 디자인 문서 | `docs/design/note-format.md` · `docs/design/brand.md` | 문구표의 값이 바뀌지 않는다(낭독 낱말 · 한도 상자 · 제목) | 없음 |
| ⑥ | 모듈 문서 | `feature/notes/AGENTS.md` 실패 이유 절의 "잠시 뒤 다시 시도해 주세요" | `잠시 뒤 다시 시도하세요` 로 | copy-apply |
| ⑦ | 시험 | 값을 단언하는 파이썬 둘(`public-repos/tests/test_template.py` 일곱 · `notes-tests/test_brand_title.py` 다섯) | 값이 그대로라 고칠 것 없음. Kotlin 시험은 `R.string` 으로 읽어 안 깨진다 — 글자로 적은 `MyNoteHeroTest` 의 안내는 견본이고 바뀌지 않는 앞 문장이다 | copy-apply(돌려 확인) |
| — | 시안 기록 | `labs/repo-connect-design/r1` · `r2` README 문구표 · `labs/brand-design` 생성기 | 그때의 시안 기록이라 고치지 않는다. 정본은 `strings.xml` 과 이 문서 | 없음 |

## 기능 문서 항목

| 항목 ID | 이 문서의 자리 |
|---|---|
| SC-1 | `### 끝말` · `### 끝말 예외` |
| SC-2 | `### 쓰지 않는 표현` |
| SC-3 | `### 용어 표` |
| SC-4 · SC-5 | 바꾼 문구 표(`docs/design/screen-copy-rows.md`) — 지운 문구 0 · 형식 자리 그대로 |
| SC-6 | 바꾼 문구 표의 자리 · 길이 칸 · `### 한 줄 고정 자리` |
| SC-7 | 바꾼 문구 표의 낭독 행 · 문장 안 나열을 쉼표로 바꾼 행 |
| SC-8 | `### 같이 바뀔 것` |
| SC-9 | `### 한 줄 고정 자리` 의 자리 목록 — 캡처는 copy-apply |

## 정한 것

| # | 날짜 · run | 결정 | 확인 거리 |
|---|---|---|---|
| 1 | 2026-10-11 · feature-modules | 권유 끝말은 `~하세요` 하나 · `~나요?` 는 바꾼다(작업 요청서 정하지 않은 것 18 기본값) | 끝말 표 2 · 5 · 6 |
| 2 | 2026-10-11 · feature-modules | 노트 = `불러오기` · 모델 = `받기` · 모델 돌리기 = `실행` · 버튼 = `새로고침` | 용어 표 |
| 3 | 2026-10-11 · feature-modules | 없앰은 `지우기`. 모델 카드 `삭제` 버튼과 그 대화상자 제목 · 스낵바 셋만 동결(버튼 글자 수) | 용어 표 동결 예외 |
| 4 | 2026-10-11 · feature-modules | 토큰 안내 낱말 · 알림 채널 이름 · 화면 제목은 그대로(기능 문서 결정 5 · BR-7) | 용어 표 동결 예외 |

## 검토

| 날짜 · run | 검토 | 누가 | 판정 | 왕복 |
|---|---|---|---|---|
| 2026-10-11 · feature-modules | 실현성(기능 문서 screen-copy) | 디자인 | 조건부 5 — 이 문서의 끝말 · 쓰지 않는 표현 · 동결 예외 · 기존 디자인 문서 맞춤 · 같이 바뀔 것으로 푼다 | 1 |

## 바뀐 기록

| 날짜 | run | 무엇이 |
|---|---|---|
| 2026-10-11 | feature-modules | 처음 — 395개 전수, 바꾼 문구 56행(screen-copy-rows.md) |
