package com.hancekim.mynote.core.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 온디바이스 엔진을 **앱 전체에 하나만** 들고 있는다.
 *
 * 초기화가 8~10초다(2.5GB 를 GPU 로 올리고 셰이더를 컴파일한다). 요청마다 만들면 GPU 를 쓰는
 * 의미가 사라지므로 한 번 만들어 계속 재사용한다.
 *
 * 대신 **앱 시작 때 만들지 않는다.** 퀴즈를 한 번도 안 누르는 사용자가 대부분일 텐데 2.5GB 를
 * 메모리에 올려 둘 이유가 없다. 첫 요청에서 lazy 로 만든다.
 */
@Singleton
class ModelEngine
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val mutex = Mutex()
        private var engine: Engine? = null

        /** 모델 파일은 기기에 따로 넣는 2.5GB 라 없을 수 있다. 없으면 기능을 감춘다. */
        fun isAvailable(): Boolean = modelFile().exists()

        fun modelFile(): File = File(context.getExternalFilesDir(null), MODEL_NAME)

        /**
         * 엔진을 쓰는 유일한 통로.
         *
         * [Mutex] 로 직렬화하는 이유는 두 가지다 — 초기화가 두 번 겹치지 않게, 그리고
         * **생성이 동시에 돌지 않게.** `Conversation` 은 라이브러리에 동기화가 없다.
         */
        suspend fun <T> use(block: suspend (Engine) -> T): T =
            mutex.withLock {
                val ready = engine ?: withContext(Dispatchers.Default) { create() }.also { engine = it }
                withContext(Dispatchers.Default) { block(ready) }
            }

        @OptIn(ExperimentalApi::class)
        private fun create(): Engine {
            // 엔진 생성 **전에** 켜야 먹는다. GPU 에서는 켜서 손해 볼 게 없다(품질 동일, 디코드만 빨라짐).
            ExperimentalFlags.enableSpeculativeDecoding = true

            val started = System.currentTimeMillis()
            val created =
                Engine(
                    EngineConfig(
                        modelPath = modelFile().absolutePath,
                        backend = Backend.GPU(),
                        // 2회차 로딩을 줄인다. 컴파일된 셰이더 등을 여기에 캐싱한다.
                        cacheDir = context.cacheDir.path,
                    ),
                )
            created.initialize()
            Log.i(TAG, "엔진 init ${System.currentTimeMillis() - started}ms")
            return created
        }

        private companion object {
            const val TAG = "MyNoteAi"
            const val MODEL_NAME = "gemma-4-E2B-it.litertlm"
        }
    }
