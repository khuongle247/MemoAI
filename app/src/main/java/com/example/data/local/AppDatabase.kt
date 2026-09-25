package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus

class Converters {
    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = try {
        TaskStatus.valueOf(value)
    } catch (_: Exception) {
        TaskStatus.TODO
    }

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority = try {
        TaskPriority.valueOf(value)
    } catch (_: Exception) {
        TaskPriority.MEDIUM
    }
}

@Database(
    entities = [TaskEntity::class, NoteEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "memo_ai_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
