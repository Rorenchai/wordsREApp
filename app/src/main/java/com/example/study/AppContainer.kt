package com.example.study

import android.content.Context
import com.example.study.data.backup.BackupRepository
import com.example.study.data.db.StudyDatabase
import com.example.study.data.plan.PlanRepository
import com.example.study.data.repo.FavoriteRepository
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.StudyRepository
import com.example.study.data.repo.WordRepository
import com.example.study.data.seed.DatabaseSeeder

/** 手动依赖容器（不引入 Hilt）：数据库、Repository 全部在这里创建一次。 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val database = StudyDatabase.get(appContext)
    private val seeder = DatabaseSeeder(appContext, database)

    val wordRepository: WordRepository = WordRepository(database.wordDao(), seeder)
    val studyRepository: StudyRepository = StudyRepository(
        wordDao = database.wordDao(),
        progressDao = database.progressDao(),
        eventDao = database.studyEventDao(),
        roundSeenDao = database.roundSeenDao(),
        studyDurationDao = database.studyDurationDao(),
        database = database
    )
    val settingsRepository: SettingsRepository = SettingsRepository.create(appContext)

    val favoriteRepository: FavoriteRepository = FavoriteRepository(database.favoriteDao())

    val planRepository: PlanRepository = PlanRepository(
        planDao = database.planDao(),
        wordDao = database.wordDao(),
        database = database
    )

    val backupRepository: BackupRepository = BackupRepository(
        wordDao = database.wordDao(),
        progressDao = database.progressDao(),
        eventDao = database.studyEventDao(),
        favoriteDao = database.favoriteDao(),
        roundSeenDao = database.roundSeenDao(),
        durationDao = database.studyDurationDao(),
        database = database
    )
}

/** Compose / ViewModel 里取容器的快捷方式。 */
val Context.appContainer: AppContainer
    get() = (applicationContext as StudyApp).container
