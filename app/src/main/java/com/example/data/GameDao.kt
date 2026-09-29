package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM level_records ORDER BY levelId ASC")
    fun getAllLevels(): Flow<List<LevelRecord>>

    @Query("SELECT * FROM level_records WHERE levelId = :levelId")
    suspend fun getLevel(levelId: Int): LevelRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLevel(level: LevelRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLevels(levels: List<LevelRecord>)

    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun getUserProgress(): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE id = 1")
    suspend fun getUserProgressOnce(): UserProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: UserProgress)
}
