package com.hancekim.mynote.core.domain.model

data class RepoConnection(
    val owner: String,
    val repo: String,
    val branch: String,
    val indexPath: String,
    val isPublic: Boolean,
) {
    val fullName: String get() = "$owner/$repo"

    fun isSameRepositoryAs(other: RepoConnection): Boolean =
        owner.equals(other.owner, ignoreCase = true) && repo.equals(other.repo, ignoreCase = true)

    companion object {
        const val DEFAULT_INDEX_PATH = "notes/index.json"
    }
}
