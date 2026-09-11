package com.hancekim.mynote.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppliedTokenSource {
    fun applied(): Flow<String?>
}
