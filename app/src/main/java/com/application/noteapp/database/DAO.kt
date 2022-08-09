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

    @Query("Update note Set is_favorite = :state where id = :note_id")
    suspend fun updateNoteFavoriteState(note_id:Int, state:Boolean)

    @Query("Update note Set is_locked = :state where id = :note_id")
    suspend fun updateNoteLockState(note_id:Int, state:Boolean)

    @Query("Update note Set is_locked = :state")
    suspend fun updateAllNoteLockState(state:Boolean)

    @Query("Select * from note where alarm_set = 1 and is_locked = 1 and is_favorite = 1")
    fun getNotesWithAlarmAndLockAndFavoriteFilter():LiveData<List<Note>>

    @Query("Select * from note where alarm_set = 1 and is_locked = 0 and is_favorite = 1")
    fun getNotesWithAlarmAndUnlockAndFavoriteFilter():LiveData<List<Note>>

    @Query("Select * from note where alarm_set = 1 and is_locked = 1")
    fun getNotesWithAlarmAndLockFilter():LiveData<List<Note>>

    @Query("Select * from note where alarm_set = 1 and is_locked = 0")
    fun getNotesWithAlarmAndUnLockFilter():LiveData<List<Note>>

    @Query("Select * from note where alarm_set = 1 and is_favorite = 1")
    fun getNotesWithAlarmAndFavoriteFilter():LiveData<List<Note>>

    @Query("Select * from note where is_favorite = 1 and is_locked = 1")
    fun getNotesWithLockAndFavoriteFilter():LiveData<List<Note>>

    @Query("Select * from note where is_favorite = 1 and is_locked = 0")
    fun getNotesWithUnlockAndFavoriteFilter():LiveData<List<Note>>

    @Query("Select * from note where is_locked = 1")
    fun getNotesWithLockFilter():LiveData<List<Note>>

    @Query("Select * from note where is_locked = 0")
    fun getNotesWithUnlockFilter():LiveData<List<Note>>

    @Query("Select * from note where is_favorite = 1")
    fun getNotesWithFavoriteFilter():LiveData<List<Note>>

    @Query("Select * from note where alarm_set = 1")
    fun getNotesWithAlarmFilter():LiveData<List<Note>>
}