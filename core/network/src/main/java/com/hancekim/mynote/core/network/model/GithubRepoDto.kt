package com.hancekim.mynote.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GithubRepoDto(
    @SerialName("full_name") val fullName: String = "",
    @SerialName("private") val isPrivate: Boolean = false,
    @SerialName("default_branch") val defaultBranch: String = "",
)
