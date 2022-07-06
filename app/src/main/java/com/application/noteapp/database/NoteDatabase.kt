package com.application.noteapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.application.noteapp.model.Note

@Database(
    entities = [Note::class],
    exportSchema = false,
    version = 1
)
abstract class NoteDatabase() : RoomDatabase() {
    abstract fun getNoteDao(): DAO

    companion object {
        private var instance: NoteDatabase? = null
        private val lock = Any()

        operator fun invoke(context: Context) = instance?: synchronized(lock){
            instance?: createDatabase(context).also {
                instance = it
            }
        }

        private fun createDatabase(context: Context) = Room.databaseBuilder(
            context.applicationContext, NoteDatabase::class.java, "Note_Database"
        ).build()

    }
}