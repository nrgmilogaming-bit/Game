package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_records")
data class LevelRecord(
    @PrimaryKey val levelId: Int,
    val unlocked: Boolean = false,
    val completed: Boolean = false,
    val bestTimeMs: Long = 0L,
    val stars: Int = 0,
    val gemsCollected: Int = 0,
    val maxGems: Int = 0
)

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1,
    val totalGems: Int = 0,
    val selectedSkinId: String = "cyber",
    val unlockedSkins: String = "cyber",
    val endlessHighScore: Int = 0,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val sensitivity: Float = 1.0f,
    val showTouchGuide: Boolean = true
) {
    fun isSkinUnlocked(skinId: String): Boolean {
        return unlockedSkins.split(",").map { it.trim() }.contains(skinId)
    }
}
