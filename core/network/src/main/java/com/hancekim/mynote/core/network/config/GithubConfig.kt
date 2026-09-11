package com.hancekim.mynote.core.network.config

data class GithubConfig(
    val owner: String,
    val repo: String,
    val branch: String,
)

fun interface GithubTokenProvider {
    fun currentToken(): String?
}
