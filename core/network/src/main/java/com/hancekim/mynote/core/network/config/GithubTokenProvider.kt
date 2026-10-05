package com.hancekim.mynote.core.network.config

fun interface GithubTokenProvider {
    fun currentToken(): String?
}
