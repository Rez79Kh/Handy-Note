package com.application.noteapp.activities

import android.os.Bundle
import android.util.Log
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.application.noteapp.R
import com.application.noteapp.database.NoteDatabase
import com.application.noteapp.databinding.ActivityMainBinding
import com.application.noteapp.receivers.NotificationReceiver
import com.application.noteapp.repository.NoteRepository
import com.application.noteapp.viewmodel.NoteViewModel
import com.application.noteapp.viewmodel.NoteViewModelFactory


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var viewModel: NoteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)

        try {
            setContentView(binding.root)
            val noteRepository = NoteRepository(NoteDatabase(this))
            val noteViewModelFactory = NoteViewModelFactory(noteRepository)
            viewModel = ViewModelProvider(this, noteViewModelFactory)[NoteViewModel::class.java]

        } catch (exp: Exception) {
            Log.d("Exception", exp.toString())
        }
    }

}