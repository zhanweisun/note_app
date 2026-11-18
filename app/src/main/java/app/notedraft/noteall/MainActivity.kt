package app.notedraft.noteall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.notedraft.noteall.ui.NoteDraftApp

/**
 * 应用唯一入口。对应原始 APK 的 MainActivity：
 *  - 处理 ACTION_SEND / ACTION_SEND_MULTIPLE / ACTION_PROCESS_TEXT 分享入参
 *  - PROCESS_TEXT 直接把选中文字建为新笔记
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { NoteDraftApp() }
    }
}