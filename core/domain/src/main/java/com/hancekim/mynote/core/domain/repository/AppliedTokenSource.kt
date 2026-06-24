package com.hancekim.mynote.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** 네트워크 클라(인터셉터)에 **현재 적용된** 토큰을 관찰한다. 구현은 :core:network 의 홀더. */
interface AppliedTokenSource {
    fun applied(): Flow<String?>
}
