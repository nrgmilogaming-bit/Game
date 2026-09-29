package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val allLevels: Flow<List<LevelRecord>> = gameDao.getAllLevels()
    val userProgress: Flow<UserProgress?> = gameDao.getUserProgress()

    suspend fun getProgressOnce(): UserProgress {
        return gameDao.getUserProgressOnce() ?: UserProgress().also {
            gameDao.insertOrUpdateProgress(it)
        }
    }

    suspend fun initializeDefaultLevels(levelCount: Int) {
        val existing = gameDao.getLevel(1)
        if (existing == null) {
            val defaults = (1..levelCount).map { id ->
                LevelRecord(
                    levelId = id,
                    unlocked = (id == 1),
                    completed = false,
                    bestTimeMs = 0L,
                    stars = 0,
                    gemsCollected = 0,
                    maxGems = 10
                )
            }
            gameDao.insertAllLevels(defaults)
        }
    }

    suspend fun saveLevelResult(
        levelId: Int,
        timeMs: Long,
        starsEarned: Int,
        gemsFound: Int,
        totalGemsAvailable: Int
    ) {
        val current = gameDao.getLevel(levelId) ?: LevelRecord(levelId = levelId, unlocked = true)
        val bestTime = if (current.bestTimeMs == 0L || timeMs < current.bestTimeMs) timeMs else current.bestTimeMs
        val bestStars = maxOf(current.stars, starsEarned)
        val bestGems = maxOf(current.gemsCollected, gemsFound)

        val updated = current.copy(
            completed = true,
            bestTimeMs = bestTime,
            stars = bestStars,
            gemsCollected = bestGems,
            maxGems = totalGemsAvailable
        )
        gameDao.insertOrUpdateLevel(updated)

        // Unlock next level
        val nextLevel = gameDao.getLevel(levelId + 1)
        if (nextLevel != null && !nextLevel.unlocked) {
            gameDao.insertOrUpdateLevel(nextLevel.copy(unlocked = true))
        }

        // Add gems to progress
        val progress = getProgressOnce()
        val newTotalGems = progress.totalGems + gemsFound
        gameDao.insertOrUpdateProgress(progress.copy(totalGems = newTotalGems))
    }

    suspend fun saveEndlessScore(score: Int, gemsCollected: Int) {
        val progress = getProgressOnce()
        val newHighScore = maxOf(progress.endlessHighScore, score)
        val newTotalGems = progress.totalGems + gemsCollected
        gameDao.insertOrUpdateProgress(
            progress.copy(
                endlessHighScore = newHighScore,
                totalGems = newTotalGems
            )
        )
    }

    suspend fun unlockSkin(skinId: String, cost: Int): Boolean {
        val progress = getProgressOnce()
        if (progress.totalGems >= cost && !progress.isSkinUnlocked(skinId)) {
            val updatedSkins = "${progress.unlockedSkins},$skinId"
            gameDao.insertOrUpdateProgress(
                progress.copy(
                    totalGems = progress.totalGems - cost,
                    unlockedSkins = updatedSkins,
                    selectedSkinId = skinId
                )
            )
            return true
        }
        return false
    }

    suspend fun selectSkin(skinId: String) {
        val progress = getProgressOnce()
        if (progress.isSkinUnlocked(skinId)) {
            gameDao.insertOrUpdateProgress(progress.copy(selectedSkinId = skinId))
        }
    }

    suspend fun updateSettings(
        sound: Boolean,
        haptics: Boolean,
        sensitivity: Float,
        showGuide: Boolean
    ) {
        val progress = getProgressOnce()
        gameDao.insertOrUpdateProgress(
            progress.copy(
                soundEnabled = sound,
                hapticsEnabled = haptics,
                sensitivity = sensitivity,
                showTouchGuide = showGuide
            )
        )
    }
}
