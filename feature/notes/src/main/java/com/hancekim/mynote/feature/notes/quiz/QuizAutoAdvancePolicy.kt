package com.hancekim.mynote.feature.notes.quiz

import android.content.Context
import android.view.accessibility.AccessibilityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class QuizAutoAdvancePolicy @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun isEnabled(): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return manager?.isTouchExplorationEnabled != true
    }
}
