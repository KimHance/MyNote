package com.hancekim.mynote.feature.notes

import android.content.Context
import android.view.accessibility.AccessibilityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 맞힌 문항을 스스로 넘길지 판단한다.
 *
 * **TalkBack 이 켜져 있으면 넘기지 않는다.** 화면 낭독은 눈으로 읽는 것보다 오래 걸리는데
 * 1.2초 뒤에 화면이 바뀌면 해설을 끝까지 들을 수 없다. 그때는 사용자가 직접 넘긴다.
 *
 * 이 판단을 화면이 아니라 여기서 하는 이유는 타이머가 ViewModel 에 있기 때문이다 —
 * 화면의 `LaunchedEffect` 에 두면 리컴포지션마다 중복 발행된다.
 */
class QuizAutoAdvancePolicy
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun isEnabled(): Boolean {
            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            return manager?.isTouchExplorationEnabled != true
        }
    }
