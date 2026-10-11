package com.hancekim.mynote.feature.connect.element

import androidx.annotation.StringRes
import com.hancekim.mynote.core.domain.connect.ConnectBranch
import com.hancekim.mynote.core.domain.connect.ConnectLinkRejection
import com.hancekim.mynote.core.domain.connect.ConnectLinkResult
import com.hancekim.mynote.core.domain.connect.parseConnectLink
import com.hancekim.mynote.feature.connect.R

sealed interface LinkCheck {
    data class Rejected(
        val reason: ConnectLinkRejection,
    ) : LinkCheck

    data class Found(
        val repoName: String,
        val branch: String?,
        val folder: String,
    ) : LinkCheck
}

val LinkCheck.canConnect: Boolean get() = this is LinkCheck.Found

fun linkCheckOf(link: String): LinkCheck =
    when (val result = parseConnectLink(link)) {
        is ConnectLinkResult.Rejected -> {
            LinkCheck.Rejected(result.reason)
        }

        is ConnectLinkResult.Accepted -> {
            val first = result.candidates.first()
            LinkCheck.Found(
                repoName = "${result.repo.owner}/${result.repo.repo}",
                branch = (first.branch as? ConnectBranch.Named)?.name,
                folder = first.indexPath.substringBeforeLast('/', "").ifEmpty { "/" },
            )
        }
    }

@StringRes
fun linkReasonRes(reason: ConnectLinkRejection): Int =
    when (reason) {
        ConnectLinkRejection.Empty -> R.string.connect_link_reason_empty
        ConnectLinkRejection.NotHttps -> R.string.connect_link_reason_scheme
        ConnectLinkRejection.NotGithub -> R.string.connect_link_reason_host
        ConnectLinkRejection.Gist -> R.string.connect_link_reason_gist
        ConnectLinkRejection.GithubPages -> R.string.connect_link_reason_pages
        ConnectLinkRejection.SingleFile -> R.string.connect_link_reason_file
        ConnectLinkRejection.MissingRepo -> R.string.connect_link_reason_no_repo
    }
