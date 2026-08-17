package com.example.jangjakpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.jangjakpos.ui.PosApp
import com.example.jangjakpos.data.DataManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 데이터베이스 및 백업 관리자 초기화 (절대 누락되면 안 되는 핵심 로직)
        DataManager.init(applicationContext)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 방금 전 겹침 방지 처리를 완료한 반응형 UI 호출
                    PosApp()
                }
            }
        }
    }
}
