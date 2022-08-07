package com.application.noteapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

}