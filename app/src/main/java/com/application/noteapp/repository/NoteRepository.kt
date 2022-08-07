package com.application.noteapp.repository

import com.application.noteapp.database.NoteDatabase
import com.application.noteapp.model.Note

class NoteRepository(private val database:NoteDatabase) {
    suspend fun insertNote(note:Note) = database.getNoteDao().insertNote(note)

    suspend fun deleteNote(note: Note) = database.getNoteDao().deleteNote(note)

    suspend fun updateNote(note: Note) = database.getNoteDao().updateNote(note)

    fun getAllNotes() = database.getNoteDao().getAllNotes()

    suspend fun deleteAllNotes() = database.getNoteDao().deleteAllNotes()

    fun findNote(string: String) = database.getNoteDao().findNote(string)

    fun updateAlarmState(note_id:Int,value:Boolean,date:String) = database.getNoteDao().updateAlarmState(note_id,value,date)

    suspend fun updateNoteLockState(note_id:Int, state:Boolean) = database.getNoteDao().updateNoteLockState(note_id,state)

    suspend fun updateAllNoteLockState(state:Boolean) = database.getNoteDao().updateAllNoteLockState(state)

}