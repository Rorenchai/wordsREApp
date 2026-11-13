package com.example.study

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.study.ui.navigation.AppNavHost
import com.example.study.ui.theme.StudyTheme

/** 单 Activity + Compose 导航。 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            StudyTheme {
                AppNavHost()
            }
        }
    }
}
