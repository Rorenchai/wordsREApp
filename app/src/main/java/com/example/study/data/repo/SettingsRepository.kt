package com.example.study.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** 应用设置：当前词库、每日目标、发音口音（DataStore Preferences）。 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {

    private val safeData: Flow<Preferences> = dataStore.data.catch { error ->
        // 读失败时退回默认值，避免首页因设置读取异常崩溃
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val currentLibrary: Flow<String> = safeData.map { it[KEY_LIBRARY] ?: LIB_CET4 }

    val dailyGoal: Flow<Int> = safeData.map { it[KEY_DAILY_GOAL] ?: DEFAULT_DAILY_GOAL }

    /** 发音口音：US / UK */
    val accent: Flow<String> = safeData.map { it[KEY_ACCENT] ?: ACCENT_US }

    /** 最近一次词汇量评测结果（0 = 未测过）。 */
    val vocabEstimate: Flow<Int> = safeData.map { it[KEY_VOCAB_ESTIMATE] ?: 0 }

    /** 最近一次词汇量评测日期（yyyy-MM-dd）。 */
    val vocabTestDate: Flow<String> = safeData.map { it[KEY_VOCAB_DATE] ?: "" }

    /** 每日学习提醒开关。 */
    val reminderEnabled: Flow<Boolean> = safeData.map { it[KEY_REMINDER_ENABLED] ?: false }

    /** 每日学习提醒时间（小时，默认 20 点）。 */
    val reminderHour: Flow<Int> = safeData.map { it[KEY_REMINDER_HOUR] ?: 20 }

    /** 主题模式：system / light / dark。 */
    val themeMode: Flow<String> = safeData.map { it[KEY_THEME_MODE] ?: THEME_SYSTEM }

    suspend fun setCurrentLibrary(library: String) {
        dataStore.edit { it[KEY_LIBRARY] = library }
    }

    suspend fun setDailyGoal(goal: Int) {
        dataStore.edit { it[KEY_DAILY_GOAL] = goal.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL) }
    }

    suspend fun setAccent(accent: String) {
        dataStore.edit { it[KEY_ACCENT] = accent }
    }

    suspend fun saveVocabEstimate(estimate: Int, date: String) {
        dataStore.edit {
            it[KEY_VOCAB_ESTIMATE] = estimate
            it[KEY_VOCAB_DATE] = date
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int) {
        dataStore.edit { it[KEY_REMINDER_HOUR] = hour.coerceIn(0, 23) }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    companion object {
        const val LIB_CET4 = "CET-4"
        const val LIB_CET6 = "CET-6"
        const val ACCENT_US = "US"
        const val ACCENT_UK = "UK"
        const val DEFAULT_DAILY_GOAL = 20
        const val MIN_DAILY_GOAL = 5
        const val MAX_DAILY_GOAL = 200
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        val LIBRARIES = listOf(LIB_CET4, LIB_CET6)
        val DAILY_GOAL_OPTIONS = listOf(10, 20, 30, 50)

        private val KEY_LIBRARY = stringPreferencesKey("current_library")
        private val KEY_DAILY_GOAL = intPreferencesKey("daily_goal")
        private val KEY_ACCENT = stringPreferencesKey("accent")
        private val KEY_VOCAB_ESTIMATE = intPreferencesKey("vocab_estimate")
        private val KEY_VOCAB_DATE = stringPreferencesKey("vocab_test_date")
        private val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")

        fun create(context: Context): SettingsRepository {
            return SettingsRepository(context.applicationContext.settingsDataStore)
        }
    }
}
