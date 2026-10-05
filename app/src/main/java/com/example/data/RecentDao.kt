package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.RecentProject
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {
    @Query("SELECT * FROM recent_projects ORDER BY createdAt DESC")
    fun getAllRecent(): Flow<List<RecentProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(project: RecentProject)

    @Delete
    suspend fun deleteRecent(project: RecentProject)

    @Query("DELETE FROM recent_projects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recent_projects")
    suspend fun clearAll()
}
