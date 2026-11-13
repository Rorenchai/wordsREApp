package com.example.study.data.repo

import com.example.study.data.db.FavoriteDao
import com.example.study.data.db.FavoriteEntity
import com.example.study.data.db.WordEntity
import kotlinx.coroutines.flow.Flow

/** 收藏夹：单词收藏 / 取消收藏与列表查询。 */
class FavoriteRepository(
    private val favoriteDao: FavoriteDao
) {

    val favoriteIds: Flow<List<Long>> = favoriteDao.observeFavoriteIds()

    fun observeFavorites(library: String = ALL_LIBRARIES): Flow<List<WordEntity>> =
        favoriteDao.observeFavorites(library)

    fun observeCount(): Flow<Int> = favoriteDao.observeCount()

    suspend fun isFavorite(wordId: Long): Boolean = favoriteDao.countByWordId(wordId) > 0

    /**
     * 切换收藏状态。
     * @return true 表示切换后为「已收藏」，false 表示已取消收藏
     */
    suspend fun toggle(wordId: Long): Boolean {
        return if (favoriteDao.countByWordId(wordId) > 0) {
            favoriteDao.deleteByWordId(wordId)
            false
        } else {
            favoriteDao.insert(
                FavoriteEntity(
                    wordId = wordId,
                    createdAt = System.currentTimeMillis()
                )
            )
            true
        }
    }

    companion object {
        const val ALL_LIBRARIES = "ALL"
    }
}
