package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

internal const val HERO_MARK_TAG = "hero_mark"

/**
 * 홈 맨 위 히어로. 마크 · 워드마크 · 저장소 줄 · 안내를 세로로 쌓는다.
 * 마크와 안내는 heroLogoSize 가 있을 때만 그린다.
 *
 * @param title 워드마크가 제목으로 읽히는 이름
 * @param repoName 연결된 저장소 이름. null 이면 줄을 그리지 않는다
 * @param hint 마크 아래 안내 문장
 */
@Composable
fun MyNoteHero(
    title: String,
    repoName: String?,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    val colors = MyNoteTheme.colors
    val adaptive = MyNoteTheme.adaptive
    val markHeight = adaptive.heroLogoSize
    val wordmarkScale = adaptive.heroTitleSize.value / WORDMARK_X_HEIGHT
    val mark = remember(colors.accent) { heroMark(colors.accent) }
    val wordmark = remember(colors.text) { heroWordmark(colors.text) }

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        if (markHeight != null) {
            Image(
                imageVector = mark,
                contentDescription = null,
                modifier =
                    Modifier
                        .testTag(HERO_MARK_TAG)
                        .size(width = markHeight * (MARK_WIDTH / MARK_HEIGHT), height = markHeight),
            )
            Spacer(Modifier.height(16.dp))
        }
        Image(
            imageVector = wordmark,
            contentDescription = null,
            modifier =
                Modifier
                    .size(width = (WORDMARK_WIDTH * wordmarkScale).dp, height = (WORDMARK_HEIGHT * wordmarkScale).dp)
                    .semantics {
                        contentDescription = title
                        heading()
                    },
        )
        if (repoName != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = repoName,
                style = MyNoteTheme.textStyles.mono,
                color = colors.text2,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
        }
        if (markHeight != null && hint != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = hint,
                style = MyNoteTheme.textStyles.body,
                color = colors.text2,
            )
        }
    }
}

@Preview(name = "Hero · 연결 있음", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun MyNoteHeroPreview() {
    MyNoteTheme {
        MyNoteHero(
            title = "MyNote",
            repoName = "study-team/android-notes",
            hint = "읽고 싶은 노트를 고르세요.",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Hero · 연결 없음", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun MyNoteHeroNoRepoPreview() {
    MyNoteTheme {
        MyNoteHero(
            title = "MyNote",
            repoName = null,
            hint = "읽고 싶은 노트를 고르세요.",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Hero · 긴 이름 · 높이 Compact", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 520)
@Composable
private fun MyNoteHeroShortPreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Medium, heightClass = MyNoteHeightClass.Compact) {
        MyNoteHero(
            title = "MyNote",
            repoName = "study-group-of-the-android-platform/kotlin-coroutines-and-flow-notes-and-reading-log-2026",
            hint = "읽고 싶은 노트를 고르세요.",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Hero · Expanded", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 560)
@Composable
private fun MyNoteHeroTallPreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Expanded, heightClass = MyNoteHeightClass.Expanded) {
        MyNoteHero(
            title = "MyNote",
            repoName = "study-team/android-notes",
            hint = "읽고 싶은 노트를 고르세요.",
            modifier = Modifier.padding(16.dp),
        )
    }
}
