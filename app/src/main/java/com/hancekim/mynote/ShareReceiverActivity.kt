package com.hancekim.mynote

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.IntentCompat
import com.hancekim.mynote.core.ui.signal.SharedTextInbox
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 다른 앱의 공유를 받는 트램펄린(local-notes LN-8). 받은 것을 받은함에 넣고 [MainActivity] 를 연 뒤 바로 끝난다.
 *
 * MainActivity 가 공유를 직접 받지 않는 이유: 보낸 앱의 태스크에 둘째 인스턴스가 떠 싱글턴 Navigator 를 나눠 쓴다.
 * 글을 인텐트에 다시 싣지 않는 이유: 512KB 글이 프로세스 사이 전달 한도에 닿는다 — 받은함(메모리)으로 넘긴다.
 * 여는 플래그는 설정 알림과 같다(`NEW_TASK · CLEAR_TOP · SINGLE_TOP` — 살아 있는 화면이면 `onNewIntent`). 글 · 제목이 없으면
 * 빈 글로 넣는다 — 편집기 쪽이 "가져올 글이 없습니다" 로 알린다(LN-8 ④).
 *
 * 파일 공유(`EXTRA_STREAM`)는 파일 열기 규칙으로 받는다(plan D34) — 받은함에는 주소만 넣고 읽기는 편집기 쪽이 메인 스레드 밖에서 한다.
 * 이 화면에 온 읽기 권한은 이 화면과 함께 끝날 수 있어 MainActivity 를 열 때 같은 주소의 읽기 권한을 넘긴다. 여러 개 공유는 받지 않는다.
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {
    @Inject lateinit var inbox: SharedTextInbox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = intent
        if (savedInstanceState == null && shared?.action == Intent.ACTION_SEND) {
            val stream = IntentCompat.getParcelableExtra(shared, Intent.EXTRA_STREAM, Uri::class.java)
            val request =
                shareRequestOf(
                    text = shared.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
                    subject =
                        (
                            shared.getCharSequenceExtra(Intent.EXTRA_SUBJECT)
                                ?: shared.getCharSequenceExtra(Intent.EXTRA_TITLE)
                        )?.toString(),
                    stream = stream?.toString(),
                )
            val open =
                Intent(this, MainActivity::class.java)
                    .setAction(ACTION_OPEN_SHARED)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
            when (request) {
                is ShareRequest.Text -> {
                    inbox.put(request.text, request.subject)
                }

                is ShareRequest.File -> {
                    inbox.putFile(request.uri, request.subject)
                    open.clipData = ClipData.newRawUri(null, stream)
                    open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            startActivity(open)
        }
        finish()
    }

    private companion object {
        /** 런처 · 설정 알림의 인텐트와 `filterEquals` 로 갈리게 하는 전용 액션. MainActivity 는 이 인텐트에서 읽을 것이 없다. */
        const val ACTION_OPEN_SHARED = "com.hancekim.mynote.action.OPEN_SHARED"
    }
}

/** 공유 한 건을 무엇으로 받나(LN-8 · plan D34). */
internal sealed interface ShareRequest {
    data class Text(
        val text: String,
        val subject: String?,
    ) : ShareRequest

    data class File(
        val uri: String,
        val subject: String?,
    ) : ShareRequest
}

/**
 * 파일이 붙어 있으면 파일 공유다 — 글(파일 이름 · 설명을 같이 보내는 앱이 많다)보다 파일이 먼저다. 파일이 없으면 글 공유이고,
 * 글이 없으면 빈 글이다(편집기 쪽이 거절 이유로 알린다).
 */
internal fun shareRequestOf(
    text: String?,
    subject: String?,
    stream: String?,
): ShareRequest = if (stream != null) ShareRequest.File(stream, subject) else ShareRequest.Text(text.orEmpty(), subject)
