package com.hancekim.mynote.core.ui.signal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 다른 앱이 공유한 글 한 건(local-notes LN-8). [subject] 는 공유 제목 — 이름 후보다.
 *
 * @property uri 파일 공유(`EXTRA_STREAM`)면 그 파일 주소 — 파일 열기 규칙으로 읽는다(plan D34). 글 공유면 null 이고 [text] 가 본문이다
 */
data class SharedText(
    val text: String,
    val subject: String?,
    val id: Int,
    val uri: String? = null,
)

/**
 * 공유 받은 글의 받은함 — 공유를 받는 화면(`:app`)이 넣고 편집기 ViewModel 이 꺼낸다.
 *
 * 글은 메모리에만 있다. 인텐트 · 목적지 키에 다시 싣지 않는 이유는 512KB 글이 프로세스 사이 전달 한도에 닿기 때문이다.
 * 싱글턴이라 화면이 다시 만들어져도 아직 안 꺼낸 글이 남고, 꺼내면 비어 같은 글을 두 번 열지 않는다.
 * 새 글이 오면 안 꺼낸 앞 글을 갈아 끼운다(공유 위 공유는 마지막 것 — LN-8 ③).
 */
@Singleton
class SharedTextInbox @Inject constructor() {
    private val seq = AtomicInteger()

    private val latest = MutableStateFlow<SharedText?>(null)
    val pending: StateFlow<SharedText?> = latest.asStateFlow()

    fun put(
        text: String,
        subject: String?,
    ) {
        latest.value = SharedText(text, subject, seq.incrementAndGet())
    }

    /** 파일 공유 — 글 대신 파일 주소를 넣는다. 읽기는 꺼내는 쪽이 메인 스레드 밖에서 한다. */
    fun putFile(
        uri: String,
        subject: String?,
    ) {
        latest.value = SharedText(text = "", subject = subject, id = seq.incrementAndGet(), uri = uri)
    }

    /** [id] 글을 꺼낸다. 그 사이 새 글이 왔으면 null — 새 글은 다음에 꺼낸다. */
    fun take(id: Int): SharedText? {
        val current = latest.value ?: return null
        if (current.id != id) return null
        return if (latest.compareAndSet(current, null)) current else null
    }
}
