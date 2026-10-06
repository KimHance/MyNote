package com.hancekim.mynote.core.domain.connect

data class TemplateRepo(
    val owner: String,
    val repo: String,
) {
    val generateUrl: String get() = "$introUrl/generate"
    val introUrl: String get() = "https://github.com/$owner/$repo"
}

fun parseTemplateRepo(value: String): TemplateRepo? {
    val parts = value.split('/')
    if (parts.size != 2) return null
    val (owner, repo) = parts
    val valid =
        ownerPattern.matches(owner) &&
            repoPattern.matches(repo) &&
            repo != "." &&
            repo != ".." &&
            !repo.endsWith(".git")
    return if (valid) TemplateRepo(owner, repo) else null
}
