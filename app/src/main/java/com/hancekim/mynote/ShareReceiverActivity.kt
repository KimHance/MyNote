package com.hancekim.mynote

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.hancekim.mynote.core.ui.signal.SharedTextInbox
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 다른 앱의 글 공유를 받는 트램펄린(local-notes LN-8). 글을 받은함에 넣고 [MainActivity] 를 연 뒤 바로 끝난다.
 *
 * MainActivity 가 공유를 직접 받지 않는 이유: 보낸 앱의 태스크에 둘째 인스턴스가 떠 싱글턴 Navigator 를 나눠 쓴다.
 * 글을 인텐트에 다시 싣지 않는 이유: 512KB 글이 프로세스 사이 전달 한도에 닿는다 — 받은함(메모리)으로 넘긴다.
 * 여는 플래그는 설정 알림과 같다(`NEW_TASK · CLEAR_TOP · SINGLE_TOP` — 살아 있는 화면이면 `onNewIntent`). 글 · 제목이 없으면
 * 빈 글로 넣는다 — 편집기 쪽이 "가져올 글이 없습니다" 로 알린다(LN-8 ④). 파일 · 여러 개 공유는 인텐트 필터가 받지 않는다.
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {
    @Inject lateinit var inbox: SharedTextInbox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent?.action == Intent.ACTION_SEND) {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
            val subject =
                (
                    intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)
                        ?: intent.getCharSequenceExtra(Intent.EXTRA_TITLE)
                )?.toString()
            inbox.put(text, subject)
            startActivity(
                Intent(this, MainActivity::class.java)
                    .setAction(ACTION_OPEN_SHARED)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    ),
            )
        }
        finish()
    }

    private companion object {
        /** 런처 · 설정 알림의 인텐트와 `filterEquals` 로 갈리게 하는 전용 액션. MainActivity 는 이 인텐트에서 읽을 것이 없다. */
        const val ACTION_OPEN_SHARED = "com.hancekim.mynote.action.OPEN_SHARED"
    }
}
