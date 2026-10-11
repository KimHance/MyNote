package com.hancekim.mynote.feature.notes.quiz

import android.content.Context
import android.view.accessibility.AccessibilityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

fun interface QuizAutoAdvancePolicy {
    fun isEnabled(): Boolean
}

class AccessibilityAutoAdvancePolicy @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : QuizAutoAdvancePolicy {
    override fun isEnabled(): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return manager?.isTouchExplorationEnabled != true
    }
}
