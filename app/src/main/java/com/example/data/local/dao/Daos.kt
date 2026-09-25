package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, dueTime ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date ORDER BY dueTime ASC")
    fun getTasksForDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTaskById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY dueDate ASC, dueTime ASC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskStatus(id: Long, status: TaskStatus, completedAt: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSnapshot(): List<TaskEntity>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNoteById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSnapshot(): List<NoteEntity>

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}
