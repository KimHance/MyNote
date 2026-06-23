package com.hancekim.mynote

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hancekim.mynote.feature.notes.NotesRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 앱이 다크 단일 테마이므로 시스템바를 항상 "어두운 배경용"(밝은 아이콘 + 투명)으로 고정한다.
        // 인자 없는 enableEdgeToEdge() 는 기기 라이트/다크 모드에 따라 아이콘 색이 바뀌어
        // 라이트 모드에서 다크 배경 위 어두운 아이콘이 안 보이는 문제가 생긴다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            // 테마는 NotesRoute 내부에서 폼팩터/폰트스케일과 함께 적용된다.
            NotesRoute()
        }
    }
}
