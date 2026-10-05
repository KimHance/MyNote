package com.hancekim.mynote.feature.notes.element

enum class HomeBody { Notes, Receiving, NoNotes }

fun homeBodyOf(
    connected: Boolean,
    noteCount: Int,
    syncing: Boolean,
): HomeBody =
    when {
        noteCount > 0 -> HomeBody.Notes
        !connected -> HomeBody.NoNotes
        syncing -> HomeBody.Receiving
        else -> HomeBody.NoNotes
    }
