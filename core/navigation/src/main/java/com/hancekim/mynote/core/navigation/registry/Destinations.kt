package com.hancekim.mynote.core.navigation.registry

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.shared.ProvideNavAnimatedScope
import kotlin.reflect.KClass

/**
 * 기능 하나가 자기 목적지 화면을 등록한다. [F] 는 셸이 모든 목적지에 똑같이 넘기는 틀 값이다(셸 틀 계약).
 *
 * 등록은 묶음을 만들 때 한 번 돈다 — 틀 값은 화면 안에서 읽는다(등록 중에 읽으면 그때 값에 묶인다).
 */
typealias DestinationRegistration<F> = DestinationScope.(frame: F) -> Unit

/**
 * 등록만 열어 준다. nav3 의 `EntryProviderScope` 를 그대로 주지 않는 이유는 둘이다.
 *
 * - 모든 화면을 공유 전환 스코프로 감싼다 — 기능이 빠뜨려도 홈 ↔ 리더 전환이 꺼지지 않는다.
 * - 내용 키 · 메타데이터를 고르지 못하게 한다 — 넓은 창 판정이 연결 화면을 기본 내용 키로 알아본다(RC-14).
 */
class DestinationScope internal constructor(
    private val scope: EntryProviderScope<MyNoteNavKey>,
) {
    fun <K : MyNoteNavKey> destination(
        clazz: KClass<K>,
        content: @Composable (K) -> Unit,
    ) {
        scope.addEntryProvider(clazz, content = { key: K -> ProvideNavAnimatedScope { content(key) } })
    }

    inline fun <reified K : MyNoteNavKey> destination(noinline content: @Composable (K) -> Unit) {
        destination(K::class, content)
    }
}

/**
 * 등록 묶음으로 `NavDisplay` 의 entryProvider 를 만든다. 같은 목적지를 두 번 등록하면 여기서 멈추고,
 * 아무도 등록하지 않은 목적지는 [fallback] 으로 간다.
 */
fun <F> destinationProvider(
    frame: F,
    registrations: List<DestinationRegistration<F>>,
    fallback: (MyNoteNavKey) -> NavEntry<MyNoteNavKey> = { error("등록되지 않은 목적지: $it") },
): (MyNoteNavKey) -> NavEntry<MyNoteNavKey> =
    entryProvider(fallback) {
        val scope = DestinationScope(this)
        registrations.forEach { register -> scope.register(frame) }
    }
