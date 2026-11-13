package com.example.study

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.study.data.repo.SettingsRepository
import com.example.study.ui.navigation.AppNavHost
import com.example.study.ui.theme.StudyTheme

/** 单 Activity + Compose 导航。 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by appContainer.settingsRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = SettingsRepository.THEME_SYSTEM
            )
            val darkTheme = when (themeMode) {
                SettingsRepository.THEME_DARK -> true
                SettingsRepository.THEME_LIGHT -> false
                else -> isSystemInDarkTheme()
            }
            StudyTheme(darkTheme = darkTheme) {
                AppNavHost()
            }
        }
    }
}
