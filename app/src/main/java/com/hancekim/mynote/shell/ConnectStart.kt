package com.hancekim.mynote.shell

enum class ConnectionKnown { Unknown, None, Connected }

enum class NotesKnown { Unknown, Empty, Some }

enum class StartDestination { Reading, ConnectRoot, Shell }

fun startDestination(
    connection: ConnectionKnown,
    notes: NotesKnown,
    seedPending: Boolean,
): StartDestination =
    when {
        connection == ConnectionKnown.Unknown || notes == NotesKnown.Unknown -> StartDestination.Reading
        connection == ConnectionKnown.Connected -> StartDestination.Shell
        seedPending -> StartDestination.Reading
        notes == NotesKnown.Empty -> StartDestination.ConnectRoot
        else -> StartDestination.Shell
    }
