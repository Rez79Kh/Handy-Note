package com.application.noteapp.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.application.noteapp.model.Note

@Dao
interface DAO{
    @Insert
    suspend fun insertNote(note:Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("Select * from note order by id desc")
    fun getAllNotes():LiveData<List<Note>>

    @Query("Select * from note where title like :string or content like :string or date like :string order by id desc")
    fun findNote(string: String):LiveData<List<Note>>

    @Query("Delete from note")
    suspend fun deleteAllNotes()

    @Query("Update note Set alarm_set = :state , alarm_date = :date where id = :note_id")
    fun updateAlarmState(note_id:Int,state:Boolean,date:String)


}