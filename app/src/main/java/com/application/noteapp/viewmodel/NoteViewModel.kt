package com.application.noteapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.application.noteapp.model.Note
import com.application.noteapp.repository.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(private val noteRepository: NoteRepository) : ViewModel() {
    fun insertNote(note: Note) = viewModelScope.launch {
        noteRepository.insertNote(note)
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        noteRepository.deleteNote(note)
    }

    fun getAllNotes(): LiveData<List<Note>> {
        return noteRepository.getAllNotes()
    }

    fun deleteAllNotes() = viewModelScope.launch {
        noteRepository.deleteAllNotes()
    }

    fun findNote(string: String): LiveData<List<Note>> {
        return noteRepository.findNote(string)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        noteRepository.updateNote(note)
    }

    fun updateAlarmState(note_id:Int,value:Boolean,date:String) = viewModelScope.launch {
        noteRepository.updateAlarmState(note_id,value,date)
    }

    fun updateNoteFavoriteState(note_id:Int,value:Boolean) = viewModelScope.launch {
        noteRepository.updateNoteFavoriteState(note_id, value)
    }

    fun updateNoteLockState(note_id:Int, state:Boolean) = viewModelScope.launch {
        noteRepository.updateNoteLockState(note_id,state)
    }

    fun updateAllNoteLockState(state:Boolean) = viewModelScope.launch {
        noteRepository.updateAllNoteLockState(state)
    }

    fun getNotesWithAlarmAndLockFilter(): LiveData<List<Note>>  {
        return noteRepository.getNotesWithAlarmAndLockFilter()
    }

    fun getNotesWithLockFilter(): LiveData<List<Note>>  {
        return noteRepository.getNotesWithLockFilter()
    }

    fun getNotesWithUnlockFilter(): LiveData<List<Note>>  {
        return noteRepository.getNotesWithUnlockFilter()
    }

    fun getNotesWithFavoriteFilter(): LiveData<List<Note>>  {
        return noteRepository.getNotesWithFavoriteFilter()
    }

    fun getNotesWithAlarmFilter(): LiveData<List<Note>>  {
        return noteRepository.getNotesWithAlarmFilter()
    }

    fun getNotesWithAlarmAndLockAndFavoriteFilter(): LiveData<List<Note>>  {
        return noteRepository.getNotesWithAlarmAndLockAndFavoriteFilter()
    }

    fun getNotesWithAlarmAndUnlockAndFavoriteFilter(): LiveData<List<Note>>  {
        return noteRepository.getNotesWithAlarmAndUnlockAndFavoriteFilter()
    }

    fun getNotesWithAlarmAndUnLockFilter(): LiveData<List<Note>>  {
        return noteRepository.getNotesWithAlarmAndUnLockFilter()
    }

    fun getNotesWithAlarmAndFavoriteFilter(): LiveData<List<Note>>  {
        return noteRepository.getNotesWithAlarmAndFavoriteFilter()
    }

    fun getNotesWithLockAndFavoriteFilter(): LiveData<List<Note>>  {
        return noteRepository.getNotesWithLockAndFavoriteFilter()
    }

    fun getNotesWithUnlockAndFavoriteFilter(): LiveData<List<Note>>  {
        return noteRepository.getNotesWithUnlockAndFavoriteFilter()
    }

}