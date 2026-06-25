package com.hancekim.mynote.core.domain.model

/**
 * 동기화 결과 요약. 새로고침 시 변경 안내(스낵바)·내비(보던 노트 삭제 시 홈 이동)에 쓰인다.
 *
 * @param added 새로 추가된 노트 수
 * @param updated 해시가 바뀌어 갱신된 노트 수
 * @param deleted 삭제된 노트 id 목록(index 에서 사라진 것)
 */
data class SyncOutcome(
    val added: Int = 0,
    val updated: Int = 0,
    val deleted: List<String> = emptyList(),
) {
    /** 추가 + 갱신(둘 다 upsert) = 사용자 관점의 "갱신". */
    val changed: Int get() = added + updated

    val hasChanges: Boolean get() = changed > 0 || deleted.isNotEmpty()
}
