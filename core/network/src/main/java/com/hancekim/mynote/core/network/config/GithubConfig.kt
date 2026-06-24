package com.hancekim.mynote.core.network.config

/**
 * 노트 레포 좌표(비밀 아님). owner/repo/branch 로 Contents API URL 을 동적 조립한다.
 * 값은 :app 이 BuildConfig 에서 읽어 Hilt 로 제공한다(레포 갈아끼우면 설정만 변경).
 */
data class GithubConfig(
    val owner: String,
    val repo: String,
    val branch: String,
)

/**
 * 현재 GitHub 토큰을 돌려준다(없으면 null). 인증 인터셉터가 매 요청에 쓴다.
 *
 * 구현은 :app 이 제공한다. 지금은 BuildConfig(local.properties) 소스이지만,
 * 다음 작업에서 **런타임 입력 + 암호화 저장** 구현으로 교체한다(이 인터페이스는 그대로).
 */
fun interface GithubTokenProvider {
    fun currentToken(): String?
}
