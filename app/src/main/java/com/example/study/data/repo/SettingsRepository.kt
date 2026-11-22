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

    val currentLibrary: Flow<String> = safeData.map { prefs ->
        val library = prefs[KEY_LIBRARY] ?: LIB_CET4
        when {
            library == LEGACY_PETS -> LIB_PETS
            library in LEGACY_TEST_LIBRARIES -> LIB_CET4
            else -> library
        }
    }

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

    /** 全局主题色预设 id（见 ui/theme/AccentPresets）。 */
    val themeAccent: Flow<String> = safeData.map { it[KEY_THEME_ACCENT] ?: THEME_ACCENT_CREAM }

    /** 小游戏/背词切到下一词时是否自动朗读。 */
    val autoReadEnabled: Flow<Boolean> = safeData.map { it[KEY_AUTO_READ] ?: true }

    /** 小游戏是否优先使用计划词池。 */
    val gamePlanMode: Flow<Boolean> = safeData.map { it[KEY_GAME_PLAN_MODE] ?: false }

    /** 最近一次打卡日期（yyyy-MM-dd，空串表示还没打卡）。 */
    val lastCheckInDate: Flow<String> = safeData.map { it[KEY_LAST_CHECK_IN] ?: "" }

    /** 用户昵称（默认「学习者」，可在设置页修改）。 */
    val userNickname: Flow<String> = safeData.map { it[KEY_NICKNAME] ?: DEFAULT_NICKNAME }

    /** 自定义头像的 content:// URI，空串表示使用默认头像。 */
    val userAvatarUri: Flow<String> = safeData.map { it[KEY_AVATAR_URI] ?: "" }

    /** 用户简介（默认英文短句）。 */
    val userBio: Flow<String> = safeData.map { it[KEY_USER_BIO] ?: DEFAULT_BIO }

    /** 当前积分。 */
    val points: Flow<Int> = safeData.map { it[KEY_POINTS] ?: 0 }

    /** 已拥有的头像框 id 集合（默认框始终拥有）。 */
    val ownedFrames: Flow<Set<String>> = safeData.map { prefs ->
        parseFrames(prefs[KEY_OWNED_FRAMES]).ifEmpty { setOf(FRAME_NONE) }
    }

    /** 当前佩戴的头像框 id。 */
    val equippedFrame: Flow<String> = safeData.map { it[KEY_EQUIPPED_FRAME] ?: FRAME_NONE }

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

    suspend fun setThemeAccent(presetId: String) {
        dataStore.edit { it[KEY_THEME_ACCENT] = presetId }
    }

    suspend fun setAutoReadEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_READ] = enabled }
    }

    suspend fun setGamePlanMode(enabled: Boolean) {
        dataStore.edit { it[KEY_GAME_PLAN_MODE] = enabled }
    }

    /** 记录今日已打卡，避免同一天重复领取奖励。 */
    suspend fun setLastCheckInDate(date: String) {
        dataStore.edit { it[KEY_LAST_CHECK_IN] = date }
    }

    suspend fun setUserNickname(nickname: String) {
        dataStore.edit { it[KEY_NICKNAME] = nickname.trim().ifBlank { DEFAULT_NICKNAME }.take(20) }
    }

    suspend fun setUserAvatarUri(uri: String) {
        dataStore.edit { it[KEY_AVATAR_URI] = uri }
    }

    private fun parseFrames(raw: String?): Set<String> {
        return raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    suspend fun setUserBio(bio: String) {
        dataStore.edit { it[KEY_USER_BIO] = bio.trim().take(MAX_BIO_LENGTH) }
    }

    suspend fun addPoints(amount: Int) {
        if (amount == 0) return
        dataStore.edit { prefs ->
            val current = prefs[KEY_POINTS] ?: 0
            prefs[KEY_POINTS] = (current + amount).coerceAtLeast(0)
        }
    }

    /** 原子兑换头像框：积分足够且未拥有时扣分并加入资产，返回是否成功。 */
    suspend fun purchaseFrame(frameId: String, cost: Int): Boolean {
        var purchased = false
        dataStore.edit { prefs ->
            val owned = parseFrames(prefs[KEY_OWNED_FRAMES]).ifEmpty { setOf(FRAME_NONE) }
            val points = prefs[KEY_POINTS] ?: 0
            if (frameId !in owned && points >= cost) {
                prefs[KEY_POINTS] = points - cost
                prefs[KEY_OWNED_FRAMES] = (owned + frameId).joinToString(",")
                purchased = true
            }
        }
        return purchased
    }

    suspend fun equipFrame(frameId: String) {
        dataStore.edit { prefs ->
            val owned = parseFrames(prefs[KEY_OWNED_FRAMES]).ifEmpty { setOf(FRAME_NONE) }
            if (frameId in owned) prefs[KEY_EQUIPPED_FRAME] = frameId
        }
    }

    companion object {
        const val LIB_CET4 = "CET-4"
        const val LIB_CET6 = "CET-6"
        const val LIB_PETS = "PETS"
        const val DEFAULT_NICKNAME = "学习者"
        const val ACCENT_US = "US"
        const val ACCENT_UK = "UK"
        const val DEFAULT_DAILY_GOAL = 20
        const val MIN_DAILY_GOAL = 5
        const val MAX_DAILY_GOAL = 200
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        const val THEME_ACCENT_CREAM = "cream"
        const val DEFAULT_BIO = "Little strokes fell great oaks～"
        const val MAX_BIO_LENGTH = 50
        const val FRAME_NONE = "none"
        const val FRAME_PRICE = 500

        val LIBRARIES = listOf(LIB_CET4, LIB_CET6, LIB_PETS)
        val DAILY_GOAL_OPTIONS = listOf(10, 20, 30, 50)

        /** 旧版本词库名与测试用词库名，读取设置/启动时自动清理。 */
        private const val LEGACY_PETS = "PETS-B"
        private val LEGACY_TEST_LIBRARIES = setOf("custom_test", "test", "测试", "测试词库")

        private val KEY_LIBRARY = stringPreferencesKey("current_library")
        private val KEY_DAILY_GOAL = intPreferencesKey("daily_goal")
        private val KEY_ACCENT = stringPreferencesKey("accent")
        private val KEY_VOCAB_ESTIMATE = intPreferencesKey("vocab_estimate")
        private val KEY_VOCAB_DATE = stringPreferencesKey("vocab_test_date")
        private val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_THEME_ACCENT = stringPreferencesKey("theme_accent")
        private val KEY_AUTO_READ = booleanPreferencesKey("auto_read_enabled")
        private val KEY_USER_BIO = stringPreferencesKey("user_bio")
        private val KEY_POINTS = intPreferencesKey("points")
        private val KEY_OWNED_FRAMES = stringPreferencesKey("owned_frames")
        private val KEY_EQUIPPED_FRAME = stringPreferencesKey("equipped_frame")
        private val KEY_GAME_PLAN_MODE = booleanPreferencesKey("game_plan_mode")
        private val KEY_LAST_CHECK_IN = stringPreferencesKey("last_check_in_date")
        private val KEY_NICKNAME = stringPreferencesKey("user_nickname")
        private val KEY_AVATAR_URI = stringPreferencesKey("user_avatar_uri")

        fun create(context: Context): SettingsRepository {
            return SettingsRepository(context.applicationContext.settingsDataStore)
        }
    }
}
